package tf.bug.japanese;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.KeywordTokenizer;

public class JapaneseNormalAnalyzer extends Analyzer {
    public JapaneseNormalAnalyzer() {
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String s) {
        final Tokenizer source = new KeywordTokenizer();
        // run the widthfilter first before bigramming, it sometimes combines characters.
        TokenStream result = new CJKWidthFilter(source);
        result = new LowerCaseFilter(result);
        return new TokenStreamComponents(source, result);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        TokenStream result = new CJKWidthFilter(in);
        result = new LowerCaseFilter(result);
        return result;
    }
}
