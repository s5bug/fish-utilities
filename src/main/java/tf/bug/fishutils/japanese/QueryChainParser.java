package tf.bug.fishutils.japanese;

import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

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

    // TODO sort KEB searches by
    // 1. keb with exact match (boolean)
    // 2. commonality of entry (int)
    // 3. Lucene score
    private static final Sort KEB_SORT =
            new Sort(SortField.FIELD_SCORE, new SortField("frequency", SortField.Type.INT));

    private static Query generateKebPrefixQuery(BytesRef kebPrefix) {
        Query exactMatch = new BoostQuery(new TermQuery(new Term("keb", kebPrefix)), 10.0f);
        Query prefixMatch = new PrefixQuery(new Term("keb", kebPrefix));

        return new DisjunctionMaxQuery(List.of(exactMatch, prefixMatch), 0.0f);
    }

    private static Query generateKebWildcardQuery(BytesRef kebWildcard) {
        return new WildcardQuery(new Term("keb", kebWildcard));
    }

    // TODO sort REB searches by
    // 1. reb with exact match (boolean)
    // 2. commonality of entry (int)
    // 3. Lucene score
    private static final Sort REB_SORT =
            new Sort(SortField.FIELD_SCORE, new SortField("frequency", SortField.Type.INT));

    private static Query generateRebPrefixQuery(BytesRef rebPrefix) {
        Query exactMatch = new BoostQuery(new TermQuery(new Term("reb", rebPrefix)), 10.0f);
        Query prefixMatch = new PrefixQuery(new Term("reb", rebPrefix));

        return new DisjunctionMaxQuery(List.of(exactMatch, prefixMatch), 0.0f);
    }

    private static Query generateRebWildcardQuery(BytesRef rebWildcard) {
        return new WildcardQuery(new Term("reb", rebWildcard));
    }

    // TODO sort Gloss searches by
    // 1. exact match (boolean)
    // 2. <pri> matches (boolean)
    // 3. commonality of entry (int)
    // 4. Lucene score
    private static final Sort GLOSS_SORT =
            new Sort(SortField.FIELD_SCORE, new SortField("frequency", SortField.Type.INT));

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

        boolean useWildcardQuery = false;
        if(NON_SIMPLE_PATTERN.matcher(humanQuery).find()) {
            useWildcardQuery = true;
        }

        final Function<? super BytesRef, ? extends Query> kebQueryConstructor;
        final Function<? super BytesRef, ? extends Query> rebQueryConstructor;
        final BiFunction<? super String, ? super String[], ? extends Query> enQueryConstructor = PhraseQuery::new;

        boolean isQuoted = QUOTED.matcher(humanQuery).matches();
        if(isQuoted) {
            // We don't use a capture group here to potentially save copies inside of Matcher
            humanQuery = humanQuery.substring(1, humanQuery.length() - 1);
            // And we should interpret everything as literal
            kebQueryConstructor = term -> new TermQuery(new Term("keb", term));
            rebQueryConstructor = term -> new TermQuery(new Term("reb", term));
        } else if(useWildcardQuery) {
            kebQueryConstructor = QueryChainParser::generateKebWildcardQuery;
            rebQueryConstructor = QueryChainParser::generateRebWildcardQuery;
        } else {
            kebQueryConstructor = QueryChainParser::generateKebPrefixQuery;
            rebQueryConstructor = QueryChainParser::generateRebPrefixQuery;
        }

        BytesRef kebQuery = JapaneseLuceneDirectory.getJmdictAnalyzer().normalize("keb", humanQuery);
        BytesRef rebQuery = JapaneseLuceneDirectory.getJmdictAnalyzer().normalize("reb", humanQuery);

        if(HAN_PATTERN.matcher(humanQuery).find()) {
            return new QueryChain(kebQueryConstructor.apply(kebQuery), KEB_SORT, null);
        } else {
            boolean latinMatches = LATIN_PATTERN.matcher(humanQuery).find();
            boolean kanaMatches = KANA_PATTERN.matcher(humanQuery).find();
            if(kanaMatches && !latinMatches) {
                return new QueryChain(rebQueryConstructor.apply(rebQuery), REB_SORT, null);
            } else {
                ArrayList<String> senseTerms = new ArrayList<>();
                try(TokenStream queryStream = JapaneseLuceneDirectory.getJmdictAnalyzer().tokenStream("gloss", humanQuery)) {
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
                Query senseQuery = enQueryConstructor.apply("gloss", senseTerms.toArray(arr));
                // TODO perform romanized reb search
                return new QueryChain(
                        kebQueryConstructor.apply(kebQuery),
                        KEB_SORT,
                        new QueryChain(senseQuery, GLOSS_SORT, null)
                );
            }
        }
    }
}
