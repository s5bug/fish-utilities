package tf.bug.fishutils.japanese;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.jetbrains.annotations.Nullable;

public final record QueryChain(
        Query luceneQuery,
        Sort bestSort,
        @Nullable QueryChain fallback
) {

}
