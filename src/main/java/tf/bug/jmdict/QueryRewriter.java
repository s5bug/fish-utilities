package tf.bug.jmdict;

public final class QueryRewriter {
    private QueryRewriter() {}

    public static LuceneFilter asLuceneQuery(String humanQuery) {
        throw new RuntimeException("Not implemented");
    }
}
