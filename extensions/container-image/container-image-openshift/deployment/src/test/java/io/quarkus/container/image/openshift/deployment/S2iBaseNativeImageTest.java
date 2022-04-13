
package io.quarkus.container.image.openshift.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class S2iBaseNativeImageTest {

    @Test
    public void testFindMatching() throws Exception {
        Optional<S2iBaseNativeImage> image = S2iBaseNativeImage.findMatching(OpenshiftConfig.DEFAULT_BASE_NATIVE_IMAGE);
        assertNotNull(image);
        assertTrue(image.isPresent());
        assertEquals(S2iBaseNativeImage.QUARKUS, image.get());
    }
}
