package org.jfrog.build.client;

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * @author Yoav Landman
 */
@Test
public class ArtifactSpecTest {

    @Test
    public void stringConstruction() throws Exception {
        ArtifactSpec standard = ArtifactSpec.newSpec("conf grp:art:ver:cls@jar k1:v1, k2:v2 , k3:   v3");
        ArtifactSpec noPropSpaces = ArtifactSpec.newSpec("conf grp:art:ver:cls@jar k1:v1,k2:v2,k3:v3");
        ArtifactSpec noConf = ArtifactSpec.newSpec("grp:art:ver:cls@jar k1:v1, k2:v2 , k3:   v3");
        ArtifactSpec allConf = ArtifactSpec.newSpec("all grp:art:ver:cls@jar k1:v1 , k2:v2 ,k3:v3");

        assertEquals(standard.getConfiguration(), "conf");
        assertEquals(standard.getGroup(), "grp");
        assertEquals(standard.getName(), "art");
        assertEquals(standard.getVersion(), "ver");
        assertEquals(standard.getClassifier(), "cls");
        assertEquals(standard.getType(), "jar");
        assertEquals(standard.getProperties(), ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));

        assertEquals(noPropSpaces.getConfiguration(), "conf");
        assertEquals(noPropSpaces.getGroup(), "grp");
        assertEquals(noPropSpaces.getName(), "art");
        assertEquals(noPropSpaces.getVersion(), "ver");
        assertEquals(noPropSpaces.getClassifier(), "cls");
        assertEquals(noPropSpaces.getType(), "jar");
        assertEquals(noPropSpaces.getProperties(), ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));

        assertEquals(noConf.getConfiguration(), "*");
        assertEquals(noConf.getGroup(), "grp");
        assertEquals(noConf.getName(), "art");
        assertEquals(noConf.getVersion(), "ver");
        assertEquals(noConf.getClassifier(), "cls");
        assertEquals(noConf.getType(), "jar");
        assertEquals(noConf.getProperties(), ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));

        try {
            ArtifactSpec noProps = ArtifactSpec.newSpec("all grp:art:ver:cls@jar");
            fail("Artifact spec cannot be constructed from string without a properties notation.");
        } catch (IllegalArgumentException e) {
            //Expected
        }
        assertEquals(allConf.getConfiguration(), "*");
        assertEquals(allConf.getGroup(), "grp");
        assertEquals(allConf.getName(), "art");
        assertEquals(allConf.getVersion(), "ver");
        assertEquals(allConf.getClassifier(), "cls");
        assertEquals(allConf.getType(), "jar");
        assertEquals(allConf.getProperties(), ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));
    }

    @Test
    public void matches() throws Exception {
        ArtifactSpec spec =
                ArtifactSpec.builder().configuration("conf").group("grp").name("art").version("ver").classifier("cls")
                        .type("jar").build();
        assertTrue(ArtifactSpec.newSpec("conf grp:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* grp:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* g?p:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:cl*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf *:*:*er:cl*@* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("conf1 *:*:*er:cl*@* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* *:*:*et:*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:*@j?? k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* *:*:*er:*@j??? k1:v1").matches(spec));
    }
}
