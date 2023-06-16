package io.quarkus.kubernetes.spi;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

public class PropertyUtil {

    private static final Set<String> VISITED_EXTENSION_PROPERTIES = new HashSet<>();
    private static final Logger LOG = Logger.getLogger(PropertyUtil.class);

    public static <T> void printMessages(String usage, String kubernetesPropertyName, Optional<Property<T>> extensionProperty) {
        extensionProperty.ifPresent(p -> {
            printMessages(usage, kubernetesPropertyName, p);
        });
    }

    public static <T> void printMessages(String usage, String kubernetesPropertyName, Property<T> extensionProperty) {
        printMessages(usage, Property.fromBuildTimeConfiguration(kubernetesPropertyName, extensionProperty.getType(), null),
                extensionProperty);
    }

    public static <T> void printMessages(String usage, Property<T> kubernetesProperty, Property<T> extensionProperty) {
        if (!VISITED_EXTENSION_PROPERTIES.add(extensionProperty.getName())) {
            return;
        }
        T kubernetesValue = kubernetesProperty.getValue().orElse(null);
        if (kubernetesValue == null) {
            // If no kubernetes property is provided, this will be used instead.
            String defaultOrProvided = extensionProperty.getValue().isPresent() ? "provided" : "default";
            String stringValue = String.valueOf(extensionProperty.getValue().orElse(extensionProperty.getDefaultValue()));
            LOG.infof("Kubernetes manifests are generated with '%s' having %s value '%s'. "
                    + "The app and manifests will get out of sync if the property '%s' is changed at runtime.",
                    usage, defaultOrProvided, stringValue, extensionProperty.getName());

        } else if (extensionProperty.getValue().filter(v -> !v.equals(kubernetesValue)).isPresent()) {
            // We have conflicting properties that need to be aligned. Maybe warn?
            String runtimeOrBuildTime = extensionProperty.isRuntime() ? "runtime" : "buildtime";
            LOG.debugf(
                    "Kubernetes property '%s' has been set with value '%s' while %s property '%s' is set with '%s'. %s will be set using the former.",
                    kubernetesProperty.getName(), kubernetesProperty.getValue().get(), runtimeOrBuildTime,
                    extensionProperty.getName(), extensionProperty.getValue().get(), usage);
        } else {
            // Both proeprties are present and aligned.
        }
    }
}
