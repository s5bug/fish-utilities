package tf.bug.fishutils.properties;

import java.util.List;
import java.util.Objects;

public sealed abstract class PropertyResult<R> permits PropertyResult.Success, PropertyResult.Failure {
    public static final class Success<R> extends PropertyResult<R> {
        private final R value;

        public Success(final R value) {
            this.value = value;
        }

        public R getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Success<?> success)) return false;
            return Objects.equals(value, success.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return "Success{" +
                   "value=" + value +
                   '}';
        }
    }

    public static final class Failure<R> extends PropertyResult<R> {
        private final List<PropertyException> topLevelFailures;

        public Failure(final List<PropertyException> topLevelFailures) {
            this.topLevelFailures = topLevelFailures;
        }

        public List<PropertyException> getTopLevelFailures() {
            return topLevelFailures;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Failure<?> failure)) return false;
            return Objects.equals(topLevelFailures, failure.topLevelFailures);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(topLevelFailures);
        }

        @Override
        public String toString() {
            return "Failure{" +
                   "topLevelFailures=" + topLevelFailures +
                   '}';
        }
    }
}
