package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Test
public class ArtifactoryEclipsePluginManagerTest {

    // checkPrerequisites must be physically declared on this subclass, not just
    // inherited, so that Class.getDeclaredMethod can find it. getDeclaredMethod
    // does not walk the superclass chain.
    public void testCheckPrerequisitesIsDeclaredOnSubclass() throws NoSuchMethodException {
        Method m = ArtifactoryEclipsePluginManager.class
                .getDeclaredMethod("checkPrerequisites", PluginDescriptor.class);
        assertNotNull(m);
        assertEquals(ArtifactoryEclipsePluginManager.class, m.getDeclaringClass());
    }
}
