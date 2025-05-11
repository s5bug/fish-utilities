package tf.bug.fishutils.properties.access;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import reactor.function.Function3;
import reactor.function.Function4;
import tf.bug.fishutils.properties.PropertyAccess;
import tf.bug.fishutils.properties.PropertyResult;

@SuppressWarnings("unchecked")
public final class PropertyAccessAggregate<R> extends PropertyAccess<R> {
    private final PropertyAccess<?>[] elements;
    private final MethodHandle combinerMethod;

    public PropertyAccessAggregate(final Supplier<R> combiner) {
        this.elements = new PropertyAccess<?>[0];
        try {
            this.combinerMethod = MethodHandles.lookup()
                    .findVirtual(Supplier.class, "get", MethodType.genericMethodType(0))
                    .bindTo(combiner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <A> PropertyAccessAggregate(PropertyAccess<A> a, Function<A, R> combiner) {
        this.elements = new PropertyAccess<?>[] { a };
        try {
            this.combinerMethod = MethodHandles.lookup()
                    .findVirtual(Function.class, "apply", MethodType.genericMethodType(1))
                    .bindTo(combiner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <A, B> PropertyAccessAggregate(PropertyAccess<A> a, PropertyAccess<B> b, BiFunction<A, B, R> combiner) {
        this.elements = new PropertyAccess<?>[] { a, b };
        try {
            this.combinerMethod = MethodHandles.lookup()
                    .findVirtual(BiFunction.class, "apply", MethodType.genericMethodType(2))
                    .bindTo(combiner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <A, B, C> PropertyAccessAggregate(PropertyAccess<A> a, PropertyAccess<B> b, PropertyAccess<C> c, Function3<A, B, C, R> combiner) {
        this.elements = new PropertyAccess<?>[] { a, b, c };
        try {
            this.combinerMethod = MethodHandles.lookup()
                    .findVirtual(Function3.class, "apply", MethodType.genericMethodType(3))
                    .bindTo(combiner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public <A, B, C, D> PropertyAccessAggregate(PropertyAccess<A> a, PropertyAccess<B> b, PropertyAccess<C> c, PropertyAccess<D> d, Function4<A, B, C, D, R> combiner) {
        this.elements = new PropertyAccess<?>[] { a, b, c, d };
        try {
            this.combinerMethod = MethodHandles.lookup()
                    .findVirtual(Function4.class, "apply", MethodType.genericMethodType(4))
                    .bindTo(combiner);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PropertyResult<R> apply(Function<String, PropertyResult<String>> propertyLookup) {
        final Object[] reifiedElements = new Object[this.elements.length];
        final List<PropertyResult.Failure<?>> failures = new ArrayList<>();
        for (int i = 0; i < this.elements.length; i++) {
            PropertyResult<?> result = this.elements[i].apply(propertyLookup);
            switch (result) {
                case PropertyResult.Success<?> success -> reifiedElements[i] = success.getValue();
                case PropertyResult.Failure<?> failure -> failures.add(failure);
            }
        }

        if(failures.isEmpty()) {
            Object value;
            try {
                value = combinerMethod.invokeWithArguments(reifiedElements);
                return new PropertyResult.Success<>((R) value);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            return new PropertyResult.Failure<>(
                    failures.stream().flatMap(s -> s.getTopLevelFailures().stream()).toList());
        }
    }
}
