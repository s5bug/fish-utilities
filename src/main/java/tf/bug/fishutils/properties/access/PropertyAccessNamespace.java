package tf.bug.fishutils.properties.access;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyException;
import tf.bug.fishutils.properties.PropertyResult;

public final class PropertyAccessNamespace<R> extends PropertyAccess<R> {
    private final PropertyAccess<? extends R> recursive;
    private final String namespace;

    @Override
    @SuppressWarnings("unchecked")
    public PropertyResult<R> apply(Set<String> keys, Function<String, String> propertyLookup) {
        final Set<String> unprefixedKeys = new HashSet<>();
        for(String key : keys) {
            if(key.startsWith(namespace + ".")) {
                unprefixedKeys.add(key.substring(namespace.length() + 1));
            }
        }

        final Function<String, String> namespacedLookup =
                s -> propertyLookup.apply("%s.%s".formatted(this.namespace, s));

        return switch(recursive.apply(unprefixedKeys, namespacedLookup)) {
            case PropertyResult.Success<? extends R> success -> (PropertyResult<R>) success;
            case PropertyResult.Failure<? extends R> failure -> new PropertyResult.Failure<>(List.of(
                    new PropertyException.PropertyExceptionNamespaced(this.namespace, failure.getTopLevelFailures())
            ));
        };
    }

    public PropertyAccessNamespace(String namespace, PropertyAccess<? extends R> recursive) {
        this.namespace = namespace;
        this.recursive = recursive;
    }
}
