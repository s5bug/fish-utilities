package tf.bug.jmdict;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import discord4j.discordjson.json.EmbedFieldData;
import io.netty.buffer.ByteBufOutputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import tf.bug.Command;

public final class JishoCommand implements Command {
    private final Directory jmdict;
    public JishoCommand(Directory jmdict) {
        this.jmdict = jmdict;
    }

    @Override
    public ApplicationCommandRequest getRequest() {
        ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                .name("jisho")
                .description("Display information for Japanese query")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("query")
                        .description("Dictionary search query")
                        .required(true)
                        .type(3)
                        .nameLocalizationsOrNull(Map.of(
                                "en-US", "query",
                                "ja", "クエリ"
                        ))
                        .descriptionLocalizationsOrNull(Map.of(
                                "en-US", "Dictionary search query",
                                "ja", "辞書検索クエリ"
                        ))
                        .build()
                )
                .nameLocalizationsOrNull(Map.of(
                        "en-US", "jisho",
                        "ja", "辞書"
                ))
                .descriptionLocalizationsOrNull(Map.of(
                        "en-US", "Display information for Japanese query",
                        "ja", "日本語のクエリを実行"
                ))
                .addAllIntegrationTypes(List.of(0, 1))
                .addAllContexts(List.of(0, 1, 2))
                .dmPermission(true)
                .build();

        return r;
    }

    @Override
    public Mono<Void> execute(GatewayDiscordClient client, ChatInputInteractionEvent event) {
        var a = event.deferReply();
        Mono<Void> b;
        try {
            b = followup(event);
            return a.then(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a.then(event.editReply(":(")).then();
    }

    private static final int PAGE_SIZE = 3;

    private @Nullable TopDocs getNonEmptyResults(IndexSearcher is, QueryChain[] refChain) throws IOException {
        while(refChain[0] != null) {
            TopDocs td = is.search(refChain[0].luceneQuery(), 1);
            if(td.totalHits.value() > 0L) return td;

            refChain[0] = refChain[0].fallback();
        }
        return null;
    }

    public Mono<Void> followup(ChatInputInteractionEvent event) {
        String q = event.getOptionAsString("query").get();

        QueryChain chain = QueryChainParser.parse(q);

        final ArrayList<Document> resultsReified;

        try(IndexReader ir = DirectoryReader.open(this.jmdict)) {
            IndexSearcher is = new IndexSearcher(ir);
            QueryChain[] refChain = new QueryChain[] { chain};
            @Nullable TopDocs results = this.getNonEmptyResults(is, refChain);
            chain = refChain[0];
            StoredFields sf = is.storedFields();

            if(results == null) {
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .description("No results found for query `%s`".formatted(q))
                        .build();
                return event.createFollowup(InteractionFollowupCreateSpec.builder()
                        .addEmbed(embed)
                        .build()).then();
            }

            resultsReified = new ArrayList<>();

            for(ScoreDoc sd : results.scoreDocs) {
                resultsReified.add(sf.document(sd.doc));
            }

            TopDocs rest = is.searchAfter(
                    results.scoreDocs[results.scoreDocs.length - 1],
                    chain.luceneQuery(),
                    (int) results.totalHits.value()
            );

            for (ScoreDoc sd : rest.scoreDocs) {
                resultsReified.add(sf.document(sd.doc));
            }
        } catch (IOException e) {
            ByteArrayOutputStream grabExceptionPrint = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(grabExceptionPrint, false, StandardCharsets.UTF_8);
            e.printStackTrace(ps);
            ps.close();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .description("Error trying to execute query: ```\n%s\n```"
                            .formatted(grabExceptionPrint.toString(StandardCharsets.UTF_8)))
                    .build();
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addEmbed(embed)
                    .build()).then();
        }

        // TODO paginated result scrolling with buttons
        ModalStateHolder stateHolder = new ModalStateHolder(q, resultsReified);
        return stateHolder.initializeOn(event);
    }

    private static final Duration TIMEOUT =
            Duration.of(5L, ChronoUnit.MINUTES);

    public static final class ModalStateHolder {
        private final String query;
        private final List<Document> entries;
        private final AtomicInteger page;
        private final int totalPages;
        private final AtomicReference<Instant> timeToDie;

        private ModalStateHolder(String query, List<Document> entries) {
            this.query = query;
            this.entries = entries;
            this.page = new AtomicInteger(0);
            this.totalPages = Math.ceilDiv(this.entries.size(), PAGE_SIZE);
            this.timeToDie = new AtomicReference<>(Instant.now().plus(TIMEOUT));
        }

        public EmbedCreateSpec makeEmbed() {
            int page = this.page.get();
            int start = PAGE_SIZE * page;
            int end = start + PAGE_SIZE;

            ArrayList<EmbedCreateFields.Field> fields = new ArrayList<>();
            for(int i = start; i < end && i < this.entries.size(); i++) {
                Document doc = this.entries.get(i);
                // TODO consider reb exclusions
                String[] writings = doc.getValues("keb");
                String[] readings = doc.getValues("reb");
                String[] senses = doc.getValues("sense");

                StringBuilder title = new StringBuilder();
                for(int j = 0; j < writings.length; j++) {
                    title.append(writings[j]);
                    title.append(", ");
                }
                title.setLength(title.length() - 2);
                if(readings.length > 0) {
                    title.append(" (");
                    for(int j = 0; j < readings.length; j++) {
                        title.append(readings[j]);
                        title.append(", ");
                    }
                    title.setLength(title.length() - 2);
                    title.append(")");
                }

                StringBuilder description = new StringBuilder();
                for(int j = 0; j < senses.length; j++) {
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
                            this.query,
                            1 + page,
                            this.totalPages
                    ))
                    .addAllFields(fields)
                    .build();
        }

        public Mono<Void> initializeOn(ChatInputInteractionEvent event) {
            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                    .addEmbed(this.makeEmbed())
                    .build()).then();
        }
    }
}
