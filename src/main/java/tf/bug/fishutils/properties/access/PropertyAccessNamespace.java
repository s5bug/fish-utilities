package tf.bug.fishutils.properties.access;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyException;
import tf.bug.fishutils.properties.PropertyResult;

public final class PropertyAccessNamespace<R> extends PropertyAccess<R> {
    private final PropertyAccess<R> recursive;
    private final String namespace;

    @Override
    public PropertyResult<R> apply(Function<String, PropertyResult<String>> propertyLookup) {
        final Function<String, PropertyResult<String>> namespacedLookup =
                s -> propertyLookup.apply("%s.%s".formatted(this.namespace, s));

        return switch(recursive.apply(namespacedLookup)) {
            case PropertyResult.Success<R> success -> success;
            case PropertyResult.Failure<R> failure -> new PropertyResult.Failure<>(List.of(
                    new PropertyException.PropertyExceptionNamespaced(this.namespace, failure.getTopLevelFailures())
            ));
        };
    }

    public PropertyAccessNamespace(String namespace, PropertyAccess<R> recursive) {
        this.namespace = namespace;
        this.recursive = recursive;
    }
}
