package tf.bug.japanese;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import tf.bug.Command;
import tf.bug.FishUtilities;

public final class JishoCommand extends Command {
    public static final String ID = "jisho";

    private final Directory jmdict;
    public JishoCommand(Directory jmdict) {
        this.jmdict = jmdict;
    }

    @Override
    public String id() {
        return JishoCommand.ID;
    }

    @Override
    public ApplicationCommandRequest register(String id) {
        ApplicationCommandRequest r = ApplicationCommandRequest.builder()
                .name(id)
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

    private QueryResponse updateInteraction(String data, QueryResponse oldResponse) {
        if(oldResponse == null) return null;
        else {
            int page = Integer.parseInt(data) - 1;
            Instant timeToDie = Instant.now().plus(TIMEOUT);
            return oldResponse.withPage(page, timeToDie);
        }
    }

    @Override
    public Mono<Void> handleButton(FishUtilities client, ButtonInteractionEvent event) {
        String data = this.dataOfButton(event.getCustomId());
        UUID target = this.uuidOfButton(event.getCustomId());
        QueryResponse newResponse =
                    this.interactionMap.compute(target, (_, qr) -> updateInteraction(data, qr));

            if(newResponse != null) {
                this.interactionMessages.putIfAbsent(target, event.getMessageId());
                return this.edit(client, event, newResponse);
            }
            else return Mono.empty();
    }

    @Override
    public Mono<Void> handleSlashCommand(FishUtilities client, ChatInputInteractionEvent event) {
        var a = event.deferReply();
        Mono<Void> b;
        try {
            b = followup(client, event);
            return a.then(b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return a.then(event.editReply(":(")).then();
    }

    private @Nullable TopDocs getNonEmptyResults(IndexSearcher is, QueryChain[] refChain) throws IOException {
        while(refChain[0] != null) {
            TopDocs td = is.search(
                    refChain[0].luceneQuery(),
                    1,
                    refChain[0].bestSort(),
                    true
            );
            if(td.totalHits.value() > 0L) return td;

            refChain[0] = refChain[0].fallback();
        }
        return null;
    }

    private final ConcurrentHashMap<UUID, QueryResponse> interactionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Snowflake> interactionMessages = new ConcurrentHashMap<>();

    private static final Duration TIMEOUT =
            Duration.of(5L, ChronoUnit.MINUTES);

    public Mono<Void> followup(FishUtilities client, ChatInputInteractionEvent event) {
        String q = event.getOptionAsString("query").get();

        QueryChain chain = QueryChainParser.parse(q);

        final ArrayList<List<Document>> resultsReified = new ArrayList<>();
        final ArrayList<Document> pageAccumulator = new ArrayList<>();
        int lineTotal = 0;

        try(IndexReader ir = DirectoryReader.open(this.jmdict)) {
            IndexSearcher is = new IndexSearcher(ir);
            QueryChain[] refChain = new QueryChain[] { chain };
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

            for(ScoreDoc sd : results.scoreDocs) {
                Document doc = sf.document(sd.doc);
                lineTotal += QueryResponse.linesInDocument(doc);
                pageAccumulator.add(doc);

                if(lineTotal > QueryResponse.PAGE_LINE_LENGTH) {
                    resultsReified.add(List.copyOf(pageAccumulator));
                    pageAccumulator.clear();
                    lineTotal = 0;
                }
            }

            // TODO replace this with further page searches on request
            // TODO don't close ir until timeout
            TopDocs rest = is.searchAfter(
                    results.scoreDocs[results.scoreDocs.length - 1],
                    chain.luceneQuery(),
                    (int) results.totalHits.value(),
                    chain.bestSort(),
                    true
            );

            for (ScoreDoc sd : rest.scoreDocs) {
                Document doc = sf.document(sd.doc);
                lineTotal += QueryResponse.linesInDocument(doc);
                pageAccumulator.add(doc);

                if(lineTotal > QueryResponse.PAGE_LINE_LENGTH) {
                    resultsReified.add(List.copyOf(pageAccumulator));
                    pageAccumulator.clear();
                    lineTotal = 0;
                }
            }

            resultsReified.add(List.copyOf(pageAccumulator));
            pageAccumulator.clear();
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

        UUID entryUuid = UUID.randomUUID();
        Instant timeToDie = Instant.now().plus(TIMEOUT);
        QueryResponse initialResponse = new QueryResponse(
                event,
                entryUuid,
                q,
                resultsReified,
                0,
                timeToDie
        );

        interactionMap.put(entryUuid, initialResponse);

        return event.createFollowup(initialResponse.makeInitialFollowup(client, this::makeButtonId))
                .flatMap(msg -> {
                    interactionMessages.put(entryUuid, msg.getId());
                    return Mono.empty();
                })
                .then(this.tryToDie(client, entryUuid));
    }

    Mono<Void> edit(FishUtilities client, ButtonInteractionEvent event, QueryResponse newResponse) {
        InteractionReplyEditSpec s = newResponse.makeReplyEdit(client, this::makeButtonId, true);

        return newResponse.event().editFollowup(event.getMessageId(), s)
                .then(event.deferEdit());
    }

    Mono<Void> tryToDie(FishUtilities client, UUID interaction) {
        QueryResponse retrieved = interactionMap.get(interaction);
        if(retrieved == null) return Mono.empty();
        else {
            Instant deathTime = retrieved.timeToDie();
            Instant now = Instant.now();
            if(now.isAfter(deathTime)) {
                InteractionReplyEditSpec withoutButtons =
                        retrieved.makeReplyEdit(client, this::makeButtonId, false);
                return retrieved.event().editFollowup(
                        this.interactionMessages.get(retrieved.uuid()),
                        withoutButtons
                ).flatMap(msg -> {
                    QueryResponse raced = interactionMap.compute(interaction, (_, qr) -> {
                        if(Objects.equals(qr, retrieved)) return null;
                        else return qr;
                    });
                    if(raced == null) {
                        interactionMessages.remove(retrieved.uuid());
                        return Mono.empty();
                    }
                    else return tryToDie(client, interaction);
                });
            } else {
                return Mono.delay(Duration.between(now, deathTime))
                        .then(Mono.defer(() -> tryToDie(client, interaction)));
            }
        }
    }
}
