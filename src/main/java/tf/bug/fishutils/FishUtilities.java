package tf.bug.fishutils;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import gg.xp.xivapi.XivApiClient;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.lucene.store.Directory;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;
import tf.bug.fishutils.japanese.JapaneseLuceneDirectory;
import tf.bug.fishutils.japanese.JishoCommand;
import tf.bug.fishutils.xivapi.XivActionCommand;

public final class FishUtilities {

    public GatewayDiscordClient gateway;
    public Directory directory;
    public final Map<String, Snowflake> commandIds;

    public FishUtilities(
            GatewayDiscordClient gateway,
            Directory japaneseLuceneDirectory,
            Map<String, Snowflake> commandIds
    ) {
        this.gateway = gateway;
        this.directory = japaneseLuceneDirectory;
        this.commandIds = new HashMap<>();
    }

    public static Mono<Void> run(FishUtilitiesProperties properties) {
        Directory directory;

        try (HttpClient uriReaderClient = HttpClient.newBuilder().build()) {
            UriReader uriReader = new UriReader(uriReaderClient);

            try(InputStream jmdictBody = uriReader.open(properties.jmdictEgz());
                InputStream freqCc100Data = uriReader.open(properties.freqCc100Json())) {
                directory = JapaneseLuceneDirectory.of(jmdictBody, freqCc100Data);
            }
        } catch (IOException | InterruptedException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        XivApiClient xivApiClient = new XivApiClient(builder -> {
            builder.setBaseUri(properties.xivapiBase());
        });

        DiscordClient client = DiscordClient.builder(properties.discordToken())
                .build();

        List<Command> commandList = List.of(
                new JishoCommand(directory),
                new XivActionCommand(xivApiClient)
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


        return client.withGateway(gateway -> {
            final FishUtilities self = new FishUtilities(
                    gateway,
                    directory,
                    new HashMap<>()
            );
            return Mono.when(
                    gateway.getRestClient().getApplicationId().flatMap(appId ->
                            gateway.getRestClient().getApplicationService()
                                    .bulkOverwriteGlobalApplicationCommand(appId, globalCommands)
                                    .flatMap(data -> {
                                        self.commandIds.put(data.name(), Snowflake.of(data.id()));
                                        return Mono.empty();
                                    }).then()),
                    gateway.on(ChatInputAutoCompleteEvent.class, event -> {
                        Command cmd = commandMap.get(event.getCommandName());
                        if(cmd != null) return cmd.handleAutoComplete(self, event);
                        else return Mono.empty();
                    }).then(),
                    gateway.on(ChatInputInteractionEvent.class, event -> {
                        Command cmd = commandMap.get(event.getCommandName());
                        if(cmd != null) return cmd.handleSlashCommand(self, event);
                        else return Mono.empty();
                    }).then(),
                    gateway.on(ButtonInteractionEvent.class, event -> {
                        String namespace = event.getCustomId().substring(0, event.getCustomId().indexOf("-"));
                        Command cmd = commandMap.get(namespace);
                        if(cmd != null) return cmd.handleButton(self, event);
                        else return Mono.empty();
                    }).then()
            );
        }).doFinally(_ -> xivApiClient.close());
    }

}
