
package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;

import io.quarkus.maven.dependency.Dependency;

public abstract class QuarkusBuildProviderTask extends QuarkusTask {

    private final QuarkusBuildConfiguration buildConfiguration;

    @Inject
    public QuarkusBuildProviderTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(description);
        this.buildConfiguration = buildConfiguration;
    }

    /**
     * Allows implementations to provide extra dependencies that should be enforced on the application.
     *
     * @return list of extra dependencies that should be enforced on the application
     */
    public abstract List<Dependency> forcedDependencies();

    public abstract Map<String, String> forcedProperties();

    @TaskAction
    public void configureBuild() {
        buildConfiguration.setForcedDependencies(
                forcedDependencies().stream().map(Dependency::toGACTVString).collect(Collectors.toList()));
        buildConfiguration.setForcedProperties(forcedProperties());
    }
}
