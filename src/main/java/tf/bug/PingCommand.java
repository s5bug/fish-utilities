package tf.bug;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.List;
import java.util.Map;

import discord4j.store.api.util.Lazy;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public final class PingCommand extends Command {
    public static final Lazy<PingCommand> INSTANCE =
            new Lazy<>(PingCommand::new);

    private PingCommand() {}

    public String id() {
        return "ping";
    }

    @Override
    public ApplicationCommandRequest register(String id) {
        ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                .name(id)
                .description("Test command")
                .nameLocalizationsOrNull(Map.of(
                        "en-US", "ping",
                        "ja", "ピン"
                ))
                .descriptionLocalizationsOrNull(Map.of(
                        "en-US", "Test command",
                        "ja", "テストコマンド"
                ))
                .addAllIntegrationTypes(List.of(0, 1))
                .addAllContexts(List.of(0, 1, 2))
                .dmPermission(true)
                .build();

        return r;
    }

    @Override
    public Mono<Void> handleSlashCommand(final GatewayDiscordClient client, final ChatInputInteractionEvent event) {
        String reply = switch(event.getInteraction().getUserLocale()) {
            case "en-US" -> "Pong!";
            case "ja" -> "ポン！";
            default -> "Pong!";
        };
        return event.reply(reply);
    }
}
