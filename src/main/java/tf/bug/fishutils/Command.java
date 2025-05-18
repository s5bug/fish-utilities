package tf.bug.fishutils;

import discord4j.core.event.domain.interaction.AutoCompleteInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

public abstract class Command {
    private static final int UUID_STRING_LENGTH = 36;

    public abstract String id();
    public abstract ApplicationCommandRequest register(String id);

    public Mono<Void> handleAutoComplete(final FishUtilities client, final ChatInputAutoCompleteEvent event) {
        return Mono.empty();
    }

    public abstract Mono<Void> handleSlashCommand(final FishUtilities client, final ChatInputInteractionEvent event);

    public Mono<Void> handleButton(final FishUtilities client, final ButtonInteractionEvent event) {
        return Mono.empty();
    }

    protected final String makeButtonId(UUID uuid, String data) {
        return "%s-%s-%s".formatted(this.id(), uuid, data);
    }
    protected final UUID uuidOfButton(String buttonId) {
        String id = this.id();
        String uuidString = buttonId.substring(id.length() + 1, id.length() + 1 + UUID_STRING_LENGTH);
        return UUID.fromString(uuidString);
    }
    protected final String dataOfButton(String buttonId) {
        String id = this.id();
        int endOfUUID = id.length() + 1 + UUID_STRING_LENGTH + 1;
        return buttonId.substring(endOfUUID);
    }
}
