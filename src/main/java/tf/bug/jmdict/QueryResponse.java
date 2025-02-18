package tf.bug.jmdict;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.possible.Possible;
import org.apache.lucene.document.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public final record QueryResponse(
        ChatInputInteractionEvent event,
        UUID uuid,
        String query,
        List<Document> entries,
        int page,
        int totalPages,
        Instant timeToDie
) {
    public static final int PAGE_SIZE = 3;

    public EmbedCreateSpec makeEmbed() {
        int start = QueryResponse.PAGE_SIZE * this.page();
        int end = start + QueryResponse.PAGE_SIZE;

        ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();
        for (int i = start; i < end && i < this.entries().size(); i++) {
            Document doc = this.entries().get(i);
            // TODO consider reb exclusions
            String[] writings = doc.getValues("keb");
            String[] readings = doc.getValues("reb");
            String[] senses = doc.getValues("sense");

            StringBuilder title = new StringBuilder();
            if(writings.length > 0) {
                for (int j = 0; j < writings.length; j++) {
                    title.append(writings[j]);
                    title.append(", ");
                }
                title.setLength(title.length() - 2);
            }
            if(writings.length > 0 && readings.length > 0) {
                title.append(" (");
            }
            if (readings.length > 0) {
                for (int j = 0; j < readings.length; j++) {
                    title.append(readings[j]);
                    title.append(", ");
                }
                title.setLength(title.length() - 2);
            }
            if(writings.length > 0 && readings.length > 0) {
                title.append(")");
            }

            StringBuilder description = new StringBuilder();
            for (int j = 0; j < senses.length; j++) {
                description.append(j);
                description.append(". ");
                description.append(senses[j]);
                description.append("\n");
            }

            fields.add(EmbedCreateFields.Field.of(
                    title.toString(),
                    description.toString(),
                    false
            ));
        }

        return EmbedCreateSpec.builder()
                .title("`%s` (Page %d/%d)".formatted(
                        this.query(),
                        1 + this.page(),
                        this.totalPages()
                ))
                .addAllFields(fields)
                .build();
    }

    ActionRow makeActionRow(BiFunction<? super UUID, ? super String, ? extends String> makeButtonId) {
        int friendlyPage = this.page() + 1;
        Button left = Button.primary(
                makeButtonId.apply(this.uuid(), Integer.toString(friendlyPage - 1)),
                "◀"
        );
        Button right = Button.primary(
                makeButtonId.apply(this.uuid(), Integer.toString(friendlyPage + 1)),
                "▶"
        );

        return ActionRow.of(
                left.disabled(friendlyPage <= 1),
                right.disabled(friendlyPage >= this.totalPages())
        );
    }

    QueryResponse withPage(int newPage, Instant timeToDie) {
        return new QueryResponse(
                this.event(),
                this.uuid(),
                this.query(),
                this.entries(),
                newPage,
                this.totalPages(),
                timeToDie
        );
    }

    InteractionFollowupCreateSpec makeInitialFollowup(BiFunction<? super UUID, ? super String, ? extends String> makeButtonId) {
        return InteractionFollowupCreateSpec.builder()
                .addEmbed(this.makeEmbed())
                .addComponent(this.makeActionRow(makeButtonId))
                .build();
    }

    InteractionReplyEditSpec makeReplyEdit(BiFunction<? super UUID, ? super String, ? extends String> makeButtonId, boolean withActions) {
        InteractionReplyEditSpec.Builder b =
                InteractionReplyEditSpec.builder();

        b.addEmbed(this.makeEmbed());
        if (withActions) b.componentsOrNull(this.makeActionRow(makeButtonId));
        else b.componentsOrNull();

        return b.build();
    }
}
