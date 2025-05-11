package tf.bug.fishutils.properties;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

public sealed abstract class PropertyException extends IllegalArgumentException {

    public PropertyException(String message) {
        super(message);
    }

    public PropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final class PropertyExceptionNamespaced extends PropertyException {
        private final String namespace;
        private final List<PropertyException> exceptions;

        public PropertyExceptionNamespaced(String namespace, List<PropertyException> exceptions, Throwable cause) {
            super("Multiple exceptions in namespace %s".formatted(namespace), cause);
            this.namespace = namespace;
            this.exceptions = exceptions;
        }

        public PropertyExceptionNamespaced(String namespace, List<PropertyException> exceptions) {
            super("Multiple exceptions in namespace %s".formatted(namespace));
            this.namespace = namespace;
            this.exceptions = exceptions;
        }

        public String getNamespace() {
            return namespace;
        }

        public List<PropertyException> getExceptions() {
            return exceptions;
        }
    }

    public static final class PropertyExecptionMissingKey extends PropertyException {
        private final String key;

        public PropertyExecptionMissingKey(String key) {
            super("Missing key '%s'".formatted(key));
            this.key = key;
        }

        public PropertyExecptionMissingKey(String key, Throwable cause) {
            super("Missing key '%s'".formatted(key), cause);
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static final class PropertyExceptionConversionFailure extends PropertyException {
        public PropertyExceptionConversionFailure(Throwable cause) {
            super("Conversion failure", cause);
        }
    }

}
