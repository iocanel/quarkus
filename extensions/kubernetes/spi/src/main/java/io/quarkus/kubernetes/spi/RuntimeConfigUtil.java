package io.quarkus.kubernetes.spi;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Utility to handling runtime properties.
 * In some cases the Kubernetes manifest generation processing needs information that are considered 'runtime'.
 * For those cases we follow a best effort approach and use the {@link ConfigProvider} api in order to check
 * if the 'runtime' infomration is added before hand in config files (e.g. application.properties etc.).
 */
public class RuntimeConfigUtil {

    private static final Logger LOG = Logger.getLogger(RuntimeConfigUtil.class);

    private static final Set<String> VISITED_PROPERTIES = new HashSet<>();

    public static <T> T getConfigProperty(String name, Class<T> type, T defaultValue) {
        return getConfigProperty(name, type, defaultValue, name);
    }

    public static <T> T getConfigProperty(String name, Class<T> type, T defaultValue, String usage) {
        Optional<T> optional = ConfigProvider.getConfig().getOptionalValue(name, type);
        T value = optional.orElse(defaultValue);
        //TODO: Having the logging here is possibly too eager, as we should avoid logging in cases where user
        //has provided the equivalent kubernetes specific configuration. So, ideally the info should be encoded in the build item.
        //and the actual logging should take place in the extension itself.
        printTraceIfRuntimePropertyIsSet(name, value, optional.isPresent(), usage);
        return value;
    }

    /**
     * This method will trace an informative message to let users know that runtime
     * properties that are used in the generated
     * resources by Kubernetes can't change again at runtime.
     *
     * For example, for users that set a runtime property "quarkus.http.port=9000"
     * at build time, Kubernetes will use this value
     * in the generated resources. Then, when running the application in Kubernetes,
     * if users try to modify again the
     * runtime property "quarkus.http.port" to a different value, this won't work
     * because the generated resources already took
     * the 9000 value.
     *
     * Note that this message won't be printed if the users didn't provide the
     * runtime property at build time.
     */
    public static <T> void printTraceIfRuntimePropertyIsSet(String name, T value, boolean exists, String usage) {
        String defaultOrProvided = exists ? "provided" : "default";
        if (VISITED_PROPERTIES.add(name)) {
            LOG.info(String.format("Kubernetes manifests are generated with '%s' having %s value '%s'. "
                    + "The app and manifests will get out of sync if the property '%s' is changed at runtime.",
                    usage, defaultOrProvided, String.valueOf(value), name));
        }
    }
}
