package tf.bug.fishutils.properties;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import reactor.function.Function3;
import reactor.function.Function4;
import tf.bug.fishutils.properties.access.PropertyAccessAggregate;
import tf.bug.fishutils.properties.access.PropertyAccessEnum;
import tf.bug.fishutils.properties.access.PropertyAccessGet;
import tf.bug.fishutils.properties.access.PropertyAccessNamespace;

public abstract class PropertyAccess<R> {

    public abstract PropertyResult<R> apply(Set<String> keys, Function<String, String> propertyLookup);

    public static <R> PropertyAccess<R> pure(final R value) {
        return new PropertyAccess<>() {
            @Override
            public PropertyResult<R> apply(Set<String> keys, Function<String, String> propertyLookup) {
                return new PropertyResult.Success<>(value);
            }
        };
    }

    public static <R> PropertyAccess<R> get(String key, Function<String, ? extends R> converter) {
        return new PropertyAccessGet<>(key, converter);
    }

    public static <R> PropertyAccess<R> namespace(String namespace, PropertyAccess<? extends R> child) {
        return new PropertyAccessNamespace<>(namespace, child);
    }

    public static <A, R> PropertyAccess<R> aggregate(Function<? super A, ? extends R> aggregator, PropertyAccess<? extends A> a) {
        return new PropertyAccessAggregate<>(a, aggregator);
    }

    public static <A, B, R> PropertyAccess<R> aggregate(BiFunction<? super A, ? super B, ? extends R> aggregator, PropertyAccess<? extends A> a, PropertyAccess<? extends B> b) {
        return new PropertyAccessAggregate<>(a, b, aggregator);
    }

    public static <A, B, C, R> PropertyAccess<R> aggregate(Function3<? super A, ? super B, ? super C, ? extends R> aggregator, PropertyAccess<? extends A> a, PropertyAccess<? extends B> b, PropertyAccess<? extends C> c) {
        return new PropertyAccessAggregate<>(a, b, c, aggregator);
    }

    public static <A, B, C, D, R> PropertyAccess<R> aggregate(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> aggregator, PropertyAccess<? extends A> a, PropertyAccess<? extends B> b, PropertyAccess<? extends C> c, PropertyAccess<? extends D> d) {
        return new PropertyAccessAggregate<>(a, b, c, d, aggregator);
    }

    public static <K extends Enum<K>, R> PropertyAccess<Map<K, R>> enums(
            K[] enums,
            PropertyAccess<BiFunction<K, String, R>> valueDecoder
    ) {
        return new PropertyAccessEnum<>(enums, valueDecoder);
    }

}
