package tf.bug.fishutils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import tf.bug.fishutils.properties.PropertyException;
import tf.bug.fishutils.properties.PropertyResult;

public class Main {
    public static void main(String[] args) {
        Properties prop = new Properties();
        try {
            String propertiesPath = args[0];
            Path path = Paths.get(propertiesPath);
            try (InputStream is = Files.newInputStream(path)) {
                prop.load(is);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to open properties file", e);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid program arguments", e);
        }

        PropertyResult<FishUtilitiesProperties> properties =
                FishUtilitiesProperties.ACCESS.apply(prop.stringPropertyNames(), prop::getProperty);

        switch(properties) {
            case PropertyResult.Success<FishUtilitiesProperties> success -> {
                FishUtilities.run(success.getValue()).block();
            }
            case PropertyResult.Failure<FishUtilitiesProperties> failure -> {
                Main.prettyPrintPropertiesFailures(failure.getTopLevelFailures());
            }
        }
    }

    public static void prettyPrintPropertiesFailures(List<PropertyException> properties) {
        System.err.println("Unable to parse properties file:");
        prettyPrintPropertiesFailures(properties, 0);
    }

    private static void prettyPrintPropertiesFailures(List<PropertyException> properties, int indent) {
        String indentString = " ".repeat(indent);
        for (PropertyException propertyException : properties) {
            switch (propertyException) {
                case PropertyException.PropertyExecptionMissingKey pemk -> {
                    System.err.printf("%smissing key: %s%n", indentString, pemk.getKey());
                }
                case PropertyException.PropertyExceptionConversionFailure pecf -> {
                    // TODO refactor PropertyAccess to use a Try or something
                    System.err.printf("%sconversion failure: %s%n", indentString, pecf.getMessage());
                }
                case PropertyException.PropertyExceptionNamespaced pen -> {
                    System.err.printf("%serrors in namespace %s:%n", indentString, pen.getNamespace());
                    prettyPrintPropertiesFailures(pen.getExceptions(), indent + 2);
                }
            }
        }
    }
}
