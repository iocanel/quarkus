
package io.quarkus.container.image.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.container.image.deployment.util.ImageUtil;

class ImageUtilTest {

    @Test
    public void testGetRepository() throws Exception {
        assertEquals("group/image", ImageUtil.getRepository("group/image"));
        assertEquals("group/image", ImageUtil.getRepository("group/image:tag"));
        assertEquals("group/image", ImageUtil.getRepository("registry/group/image:tag"));
    }
}
