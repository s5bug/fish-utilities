package tf.bug;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandContexts;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

public enum PingCommand implements Command {
    INSTANCE;

    @Override
    public ApplicationCommandRequest getRequest() {
        ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                .name("ping")
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
    public Mono<Void> execute(final GatewayDiscordClient client, final ChatInputInteractionEvent event) {
        String reply = switch(event.getInteraction().getUserLocale()) {
            case "en-US" -> "Pong!";
            case "ja" -> "ポン！";
            default -> "Pong!";
        };
        return event.reply(reply);
    }
}
