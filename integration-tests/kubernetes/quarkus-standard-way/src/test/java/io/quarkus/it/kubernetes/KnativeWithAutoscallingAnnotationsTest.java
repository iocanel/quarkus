package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;

public class KnativeWithAutoscallingAnnotationsTest {

    private static final String NAME = "knative-with-autoscaling-annotations";

    @RegisterExtension
    static final QuarkusProdModeTest config = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(GreetingResource.class))
            .setApplicationName(NAME)
            .setApplicationVersion("0.1-SNAPSHOT")
            .overrideConfigKey("quarkus.kubernetes.deployment-target", "knative")
            .overrideConfigKey("quarkus.knative.annotations.\"autoscaling.knative.dev/window\"", "40s");

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void assertGeneratedResources() throws IOException {
        Path kubernetesDir = prodModeTestResults.getBuildDir().resolve("kubernetes");
        assertThat(kubernetesDir)
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.json"))
                .isDirectoryContaining(p -> p.getFileName().endsWith("knative.yml"))
                .satisfies(p -> assertThat(p.toFile().listFiles()).hasSize(2));

        List<HasMetadata> kubernetesList = DeserializationUtil
                .deserializeAsList(kubernetesDir.resolve("knative.yml"));

        assertThat(kubernetesList).filteredOn(i -> "Service".equals(i.getKind())).singleElement().satisfies(i -> {
            Service service = (Service) i;
            assertThat(service.getMetadata().getName()).isEqualTo(NAME);
            // Issue: https://github.com/quarkusio/quarkus/issues/23832
            // assertThat(service.getSpec().getTemplate().getSpec().getContainerConcurrency()).isEqualTo(5);

            Map<String, String> annotations = service.getSpec().getTemplate().getMetadata().getAnnotations();
            assertThat(annotations).contains(entry("autoscaling.knative.dev/window", "40s"));
            assertThat(service.getSpec().getTemplate().getMetadata().getAnnotations())
                    .doesNotContainEntry("autoscaling.knative.dev/window", "40s");
        });
    }
}
