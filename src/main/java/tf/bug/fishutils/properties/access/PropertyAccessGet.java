package tf.bug.fishutils.properties.access;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyException;
import tf.bug.fishutils.properties.PropertyResult;

public final class PropertyAccessGet<R> extends PropertyAccess<R> {
    private final String key;
    private final Function<String, ? extends R> getter;

    public PropertyAccessGet(String key, Function<String, ? extends R> getter) {
        this.key = key;
        this.getter = getter;
    }

    @Override
    public PropertyResult<R> apply(Set<String> keys, Function<String, String> propertyLookup) {
        if(keys.contains(key)) {
            try {
                return new PropertyResult.Success<>(getter.apply(propertyLookup.apply(key)));
            } catch (RuntimeException e) {
                return new PropertyResult.Failure<>(List.of(new PropertyException.PropertyExceptionConversionFailure(e)));
            }
        } else {
            return new PropertyResult.Failure<>(List.of(new PropertyException.PropertyExecptionMissingKey(key)));
        }
    }
}
