package tf.bug.jmdict;

import org.apache.lucene.document.Document;

public interface LuceneFilter {
    String asLuceneQuery();
    boolean postFilter(Document document);
}
