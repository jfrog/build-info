package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Test
public class ArtifactoryEclipsePluginManagerTest {

    // Mirrors Develocity's exact reflective contract: it calls
    // Class.getDeclaredMethod("checkPrerequisites", PluginDescriptor.class) on the
    // Plexus-bound DefaultMavenPluginManager implementation. getDeclaredMethod does
    // not walk the superclass, so the method must be physically declared here.
    public void testCheckPrerequisitesIsDeclaredOnSubclass() throws NoSuchMethodException {
        Method m = ArtifactoryEclipsePluginManager.class
                .getDeclaredMethod("checkPrerequisites", PluginDescriptor.class);
        assertNotNull(m);
        assertEquals(ArtifactoryEclipsePluginManager.class, m.getDeclaringClass());
    }
}
