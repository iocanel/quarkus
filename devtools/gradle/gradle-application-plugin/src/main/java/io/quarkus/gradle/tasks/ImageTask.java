
package io.quarkus.gradle.tasks;

import java.util.Collections;
import java.util.Map;

import org.gradle.api.tasks.options.Option;

public abstract class ImageTask extends QuarkusBuildProviderTask {

    boolean dryRun;

    @Option(option = "dry-run", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public ImageTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(buildConfiguration, description);
    }

    @Override
    public Map<String, String> forcedProperties() {
        if (dryRun) {
            return Collections.emptyMap();
        }
        return Map.of("quarkus.container-image.build", "true");
    }
}
