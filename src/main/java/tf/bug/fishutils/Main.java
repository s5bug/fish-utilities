package tf.bug.fishutils;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import tf.bug.fishutils.japanese.JishoCommand;
import tf.bug.fishutils.japanese.JapaneseLuceneDirectory;

public class Main {
    private static final Logger LOG = Loggers.getLogger(Main.class);

    public static void main(String[] args) {
        Properties prop = new Properties();
        try {
            String propertiesPath = args[0];
            Path path = Paths.get(propertiesPath);
            try (InputStream is = Files.newInputStream(path)) {
                prop.load(is);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to open properties file", e);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid program arguments", e);
        }

        FishUtilities context = new FishUtilities();

        try (HttpClient uriReaderClient = HttpClient.newBuilder().build()) {
            UriReader uriReader = new UriReader(uriReaderClient);

            String jmdictEGz = prop.getProperty("jmdict.jmdict_e_gz_uri");
            URI jmdictEGzUri = URI.create(jmdictEGz);

            String freqCc100 = prop.getProperty("freq_cc100.freq_cc100_json_uri");
            URI freqCc100Uri = URI.create(freqCc100);

            try(InputStream jmdictBody = uriReader.open(jmdictEGzUri);
                InputStream freqCc100Data = uriReader.open(freqCc100Uri)) {
                context.directory = JapaneseLuceneDirectory.of(jmdictBody, freqCc100Data);
            }


        } catch (IOException | InterruptedException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        String token = prop.getProperty("discord.token");

        DiscordClient client = DiscordClient.builder(token)
                .build();

        List<Command> commandList = List.of(
                new JishoCommand(context.directory)
        );

        final ArrayList<ApplicationCommandRequest> globalCommands = new ArrayList<>(commandList.size());
        final HashMap<String, Command> commandMap = new HashMap<>();
        commandList.forEach(c -> {
            ApplicationCommandRequest r = c.register(c.id());
            if(!c.id().equals(r.name()))
                throw new IllegalArgumentException("Command %s has a name mismatching its ID".formatted(c.id()));
            globalCommands.add(r);
            commandMap.put(c.id(), c);
        });


        Mono<Void> login = client.withGateway(gateway -> {
            context.gateway = gateway;
            return Mono.when(
                    gateway.getRestClient().getApplicationId().flatMap(appId ->
                            gateway.getRestClient().getApplicationService()
                                    .bulkOverwriteGlobalApplicationCommand(appId, globalCommands)
                                    .flatMap(data -> {
                                        context.commandIds.put(data.name(), Snowflake.of(data.id()));
                                        return Mono.empty();
                                    }).then()),
                    gateway.on(ChatInputInteractionEvent.class, event -> {
                        Command cmd = commandMap.get(event.getCommandName());
                        if(cmd != null) return cmd.handleSlashCommand(context, event);
                        else return Mono.empty();
                    }).then(),
                    gateway.on(ButtonInteractionEvent.class, event -> {
                        String namespace = event.getCustomId().substring(0, event.getCustomId().indexOf("-"));
                        Command cmd = commandMap.get(namespace);
                        if(cmd != null) return cmd.handleButton(context, event);
                        else return Mono.empty();
                    }).then()
            );
        });

        login.block();
    }
}
