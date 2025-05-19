package tf.bug.fishutils.properties.access;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyException;
import tf.bug.fishutils.properties.PropertyResult;
import tf.bug.fishutils.xivapi.CastType;

public final class PropertyAccessEnum<K extends Enum<K>, R> extends PropertyAccess<Map<K, R>> {
    private final K[] keys;
    private final PropertyAccess<BiFunction<K, String, R>> valueGetter;

    public PropertyAccessEnum(final K[] keys, PropertyAccess<BiFunction<K, String, R>> valueGetter) {
        this.keys = keys;
        this.valueGetter = valueGetter;
    }

    @Override
    public PropertyResult<Map<K, R>> apply(Set<String> keys, Function<String, String> propertyLookup) {
        final Map<K, R> result = new HashMap<>();
        final List<PropertyException> failures = new ArrayList<>();
        for(K k : this.keys) {
            String property = k.name().toLowerCase(Locale.US);
            PropertyResult<BiFunction<K, String, R>> subParse =
                    new PropertyAccessNamespace<>(property, this.valueGetter).apply(keys, propertyLookup);
            switch (subParse) {
                case PropertyResult.Success<BiFunction<K, String, R>> v -> {
                    result.put(k, v.getValue().apply(k, propertyLookup.apply(property)));
                }
                case PropertyResult.Failure<BiFunction<K, String, R>> v -> {
                    failures.addAll(v.getTopLevelFailures());
                }
            }
        }
        if(!failures.isEmpty()) {
            return new PropertyResult.Failure<>(failures);
        } else {
            return new PropertyResult.Success<>(result);
        }
    }
}
