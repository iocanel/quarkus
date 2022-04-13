
package io.quarkus.container.image.openshift.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class S2iBaseJavaImageTest {

    @Test
    public void testFindMatching() throws Exception {
        Optional<S2iBaseJavaImage> image = S2iBaseJavaImage.findMatching(OpenshiftConfig.DEFAULT_BASE_JVM_JDK11_IMAGE);
        assertNotNull(image);
        assertTrue(image.isPresent());
        assertEquals(S2iBaseJavaImage.UBI_11, image.get());
    }
}
