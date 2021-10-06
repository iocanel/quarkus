
package io.quarkus.kubernetes.service.binding.buildtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.DecoratorBuildItem;
import io.quarkus.kubernetes.spi.KubernetesResourceMetadataBuildItem;

public class ServiceBindingProcessor {

    @BuildStep
    public List<DecoratorBuildItem> createServiceBindingDecorators(KubernetesServiceBindingConfig serviceBindingConfig,
            List<KubernetesResourceMetadataBuildItem> resources) {
        List<DecoratorBuildItem> result = new ArrayList<>();
        resources.stream().filter(distinctByKey(d -> d.getName())).forEach(r -> {
            if (!serviceBindingConfig.services.isEmpty()) {
                System.out.println("Adding SB to:" + r.getTarget() + ".yml for kind:" + r.getKind());
                result.add(new DecoratorBuildItem(r.getTarget(), new AddServiceBindingResourceDecorator(r.getGroup(),
                        r.getVersion(), r.getKind(), r.getName(), serviceBindingConfig)));
            }
        });
        return result;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
