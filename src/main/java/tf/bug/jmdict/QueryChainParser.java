package tf.bug.jmdict;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

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

        final Function<? super Term, ? extends Query> queryConstructor;

        boolean isQuoted = QUOTED.matcher(humanQuery).matches();
        if(isQuoted) {
            // We don't use a capture group here to potentially save copies inside of Matcher
            humanQuery = humanQuery.substring(1, humanQuery.length() - 1);
            // And we should interpret everything as literal
            queryConstructor = TermQuery::new;
            appendGlob = false;
        } else {
            queryConstructor = WildcardQuery::new;
        }

        if(appendGlob) {
            humanQuery = humanQuery + "*";
        }

        if(HAN_PATTERN.matcher(humanQuery).find()) {
            Term term = new Term("keb", humanQuery);
            return new QueryChain(queryConstructor.apply(term), null);
        } else {
            boolean latinMatches = LATIN_PATTERN.matcher(humanQuery).find();
            boolean kanaMatches = KANA_PATTERN.matcher(humanQuery).find();
            if(kanaMatches && !latinMatches) {
                Term term = new Term("reb", humanQuery);
                return new QueryChain(queryConstructor.apply(term), null);
            } else {
                Term senseTerm = new Term("sense", humanQuery);
                Term kebTerm = new Term("keb", humanQuery);
                // TODO perform romanized reb search
                return new QueryChain(
                        queryConstructor.apply(kebTerm),
                        new QueryChain(queryConstructor.apply(senseTerm), null)
                );
            }
        }
    }
}
