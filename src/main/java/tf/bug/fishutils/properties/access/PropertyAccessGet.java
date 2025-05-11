package tf.bug.fishutils.properties.access;

import java.util.function.Function;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyResult;

public final class PropertyAccessGet<R> extends PropertyAccess<R> {
    private final String key;
    private final Function<String, PropertyResult<R>> getter;

    public PropertyAccessGet(String key, Function<String, PropertyResult<R>> getter) {
        this.key = key;
        this.getter = getter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PropertyResult<R> apply(Function<String, PropertyResult<String>> propertyLookup) {
        return switch(propertyLookup.apply(this.key)) {
            case PropertyResult.Success<String> succ -> this.getter.apply(succ.getValue());
            case PropertyResult.Failure<String> fail -> (PropertyResult<R>) fail;
        };
    }
}
