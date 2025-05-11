package tf.bug.fishutils.properties;

import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.function.Function3;
import reactor.function.Function4;
import tf.bug.fishutils.properties.access.PropertyAccessAggregate;
import tf.bug.fishutils.properties.access.PropertyAccessGet;
import tf.bug.fishutils.properties.access.PropertyAccessNamespace;

public abstract class PropertyAccess<R> {

    public abstract PropertyResult<R> apply(Function<String, PropertyResult<String>> propertyLookup);

    public static <R> PropertyAccess<R> get(String key, Function<String, R> converter) {
        return new PropertyAccessGet<>(key, s -> new PropertyResult.Success<>(converter.apply(s)));
    }

    public static <R> PropertyAccess<R> namespace(String namespace, PropertyAccess<R> child) {
        return new PropertyAccessNamespace<>(namespace, child);
    }

    public static <A, R> PropertyAccess<R> aggregate(Function<A, R> aggregator, PropertyAccess<A> a) {
        return new PropertyAccessAggregate<>(a, aggregator);
    }

    public static <A, B, R> PropertyAccess<R> aggregate(BiFunction<A, B, R> aggregator, PropertyAccess<A> a, PropertyAccess<B> b) {
        return new PropertyAccessAggregate<>(a, b, aggregator);
    }

    public static <A, B, C, R> PropertyAccess<R> aggregate(Function3<A, B, C, R> aggregator, PropertyAccess<A> a, PropertyAccess<B> b, PropertyAccess<C> c) {
        return new PropertyAccessAggregate<>(a, b, c, aggregator);
    }

    public static <A, B, C, D, R> PropertyAccess<R> aggregate(Function4<A, B, C, D, R> aggregator, PropertyAccess<A> a, PropertyAccess<B> b, PropertyAccess<C> c, PropertyAccess<D> d) {
        return new PropertyAccessAggregate<>(a, b, c, d, aggregator);
    }

}
