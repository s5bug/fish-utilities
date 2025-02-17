package tf.bug;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface Command {
    public ApplicationCommandRequest getRequest();

    public Mono<Void> execute(final GatewayDiscordClient client, final ChatInputInteractionEvent event);
}
