package tf.bug.jmdict;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.BytesTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class QueryChainParser {
    private QueryChainParser() {}

    public static final Pattern QUOTED = Pattern.compile("\".+\"");

    private static final Pattern HAN_PATTERN =
            Pattern.compile("\\p{sc=Han}", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern KANA_PATTERN =
            Pattern.compile(
                    "\\p{sc=Katakana}|\\p{sc=Hiragana}|\\p{sc=Kana}",
                    Pattern.UNICODE_CHARACTER_CLASS
            );
    private static final Pattern LATIN_PATTERN =
            Pattern.compile(
                    "\\p{sc=Latin}"
            );
    private static final Pattern NON_SIMPLE_PATTERN =
            Pattern.compile("[*?]");

    /// Follow the following rules:
    /// - Trim the query
    /// - If the query is surrounded by quotes,
    ///   - Take the content within the quotes as a literal search
    ///   - Escape this content
    /// - If the search has Han in it, only search `keb`
    /// - If the search has Latin and Kana in it,
    ///   - If not quoted, try to parse from romanization and search `reb`
    ///   - Try to search `keb`
    ///   - Search sense
    /// - If the literal search has Kana in it,
    ///   - Search `reb`
    /// - Otherwise
    ///   - If not quoted, try to parse from romanization and search `reb`
    ///   - Try to search `keb`
    ///   - Search sense
    public static QueryChain parse(String humanQuery) {
        humanQuery = humanQuery.trim();

        boolean appendGlob = true;
        if(NON_SIMPLE_PATTERN.matcher(humanQuery).find()) {
            appendGlob = false;
        }

        final Function<? super Term, ? extends Query> jpQueryConstructor;
        final BiFunction<? super String, ? super String[], ? extends Query> enQueryConstructor = PhraseQuery::new;

        boolean isQuoted = QUOTED.matcher(humanQuery).matches();
        if(isQuoted) {
            // We don't use a capture group here to potentially save copies inside of Matcher
            humanQuery = humanQuery.substring(1, humanQuery.length() - 1);
            // And we should interpret everything as literal
            jpQueryConstructor = TermQuery::new;
            appendGlob = false;
        } else {
            jpQueryConstructor = WildcardQuery::new;
        }

        BytesRef kebQuery;
        BytesRef rebQuery;
        if(appendGlob) {
            BytesRefBuilder kebBuilder = new BytesRefBuilder();
            kebBuilder.append(Jmdict.getJmdictAnalyzer().normalize("keb", humanQuery));
            kebBuilder.append(new BytesRef("*"));
            kebQuery = kebBuilder.get();

            BytesRefBuilder rebBuilder = new BytesRefBuilder();
            rebBuilder.append(Jmdict.getJmdictAnalyzer().normalize("reb", humanQuery));
            rebBuilder.append(new BytesRef("*"));
            rebQuery = rebBuilder.get();
        } else {
            kebQuery = Jmdict.getJmdictAnalyzer().normalize("keb", humanQuery);
            rebQuery = Jmdict.getJmdictAnalyzer().normalize("reb", humanQuery);
        }

        if(HAN_PATTERN.matcher(humanQuery).find()) {
            Term term = new Term("keb", kebQuery);
            return new QueryChain(jpQueryConstructor.apply(term), null);
        } else {
            boolean latinMatches = LATIN_PATTERN.matcher(humanQuery).find();
            boolean kanaMatches = KANA_PATTERN.matcher(humanQuery).find();
            if(kanaMatches && !latinMatches) {
                Term term = new Term("reb", rebQuery);
                return new QueryChain(jpQueryConstructor.apply(term), null);
            } else {
                ArrayList<String> senseTerms = new ArrayList<>();
                try(TokenStream queryStream = Jmdict.getJmdictAnalyzer().tokenStream("sense", humanQuery)) {
                    CharTermAttribute termAttribute = queryStream.addAttribute(CharTermAttribute.class);

                    queryStream.reset();
                    while (queryStream.incrementToken()) {
                        String termString = termAttribute.toString();
                        senseTerms.add(termString);
                    }
                    queryStream.end();
                } catch (IOException ioe) {
                    throw new IllegalStateException("Should not encounter IOException on pure string tokenization", ioe);
                }

                String[] arr = new String[senseTerms.size()];
                Query senseQuery = enQueryConstructor.apply("sense", senseTerms.toArray(arr));
                Term kebTerm = new Term("keb", kebQuery);
                // TODO perform romanized reb search
                return new QueryChain(
                        jpQueryConstructor.apply(kebTerm),
                        new QueryChain(senseQuery, null)
                );
            }
        }
    }
}
