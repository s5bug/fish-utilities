package tf.bug.jmdict;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
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

    private static final Pattern HAN_PATTERN =
            Pattern.compile("\\p{sc=Han}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern ALL_KANA_PATTERN =
            Pattern.compile(
                    "(?:\\p{sc=Katakana}|\\p{sc=Hiragana}|\\p{sc=Kana}|[*?])+",
                    Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL
            );
    private static final Pattern NON_SIMPLE_PATTERN =
            Pattern.compile("[*?]");

    public Mono<Void> followup(ChatInputInteractionEvent event) {
        String q = event.getOptionAsString("query").get();

        String row;
        if(HAN_PATTERN.matcher(q).find()) {
            row = "keb";
        } else if(ALL_KANA_PATTERN.matcher(q).matches()) {
            row = "reb";
        } else {
            row = "sense";
        }

        Term term;
        if(NON_SIMPLE_PATTERN.matcher(q).find()) {
            term = new Term(row, q);
        } else {
            term = new Term(row, q + "*");
        }

        Query query = new WildcardQuery(term);

        IndexReader ir;
        try {
            ir = DirectoryReader.open(this.jmdict);
        } catch (IOException e) {
            // TODO make this show an error to the user
            throw new RuntimeException(e);
        }

        IndexSearcher is = new IndexSearcher(ir);
        TopDocs td;
        try {
            // TODO paginate with modal buttons
            td = is.search(query, 10);
        } catch (IOException e) {
            // TODO show an error to the user
            throw new RuntimeException(e);
        }

        // TODO paginate with modal buttons
        ArrayList<Document> docs = new ArrayList<>();
        StoredFields sf = null;
        try {
            sf = is.storedFields();
            for(ScoreDoc sd : td.scoreDocs) {
                docs.add(sf.document(sd.doc));
            }
        } catch (IOException e) {
            // TODO throw an error message
            throw new RuntimeException(e);
        }

        // TODO build an embed
        StringBuilder result = new StringBuilder();
        for(Document doc : docs) {
            result.append("- **");
            String canonical = doc.get("keb");
            result.append(canonical);
            result.append("**");
            String[] senses = doc.getValues("sense");
            for(int i = 0; i < senses.length; i++) {
                result.append("\n  ");
                result.append(1 + i);
                result.append(". ");
                result.append(senses[i]);
            }
            result.append("\n");
        }

        // TODO create a timeout(?) paginated state
        // It's probably not that hard to store the state in the embed
        // Page can be parsed from embed, query can be stored in button IDs
        return event.createFollowup(result.toString()).then();
    }
}
