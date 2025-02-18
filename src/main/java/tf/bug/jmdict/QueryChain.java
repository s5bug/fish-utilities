package tf.bug.jmdict;

import org.apache.lucene.search.Query;
import org.jetbrains.annotations.Nullable;

public final record QueryChain(Query luceneQuery, @Nullable QueryChain fallback) {

}
