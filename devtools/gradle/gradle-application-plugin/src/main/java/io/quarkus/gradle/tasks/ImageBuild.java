
package io.quarkus.gradle.tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.gradle.GradleUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;

public abstract class ImageBuild extends ImageTask {

    public enum Builder {
        docker,
        jib,
        buildpack,
        openshift
    }

    Builder builder = Builder.docker;

    @Option(option = "builder", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setBuilder(Builder builder) {
        this.builder = builder;
    }

    @Inject
    public ImageBuild(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perform an image build");
    }

    @Override
    public List<Dependency> forcedDependencies() {
        String quarkusVersion = GradleUtils.getQuarkusCoreVersion(getProject());
        return Arrays.asList(new ArtifactDependency("io.quarkus", "quarkus-container-image-" + builder.name(), null,
                ArtifactCoords.TYPE_JAR, quarkusVersion));
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.build", "true",
                "quarkus.container-image.builder", builder.name());
    }

    @TaskAction
    public void checkRequiredExtensions() {
        // Currently forcedDependencies() is not implemented for gradle.
        // So, let's give users a meaningful warning message.
        String requiredExtension = "quarkus-container-image-" + builder.name();
        String requiredDependency = requiredExtension + "-deployment";
        List<String> projectDependencies = getProject().getConfigurations().stream().flatMap(c -> c.getDependencies().stream())
                .map(d -> d.getName())
                .collect(Collectors.toList());

        if (!projectDependencies.contains(requiredDependency)) {
            getProject().getLogger().warn("Task: {} requires extensions: {}", getName(), requiredDependency);
            getProject().getLogger().warn("To add the extensions to the project you can run the following command:");
            getProject().getLogger().warn("\tgradle addExtension --extensions={}", requiredExtension);
        }
    }
}
