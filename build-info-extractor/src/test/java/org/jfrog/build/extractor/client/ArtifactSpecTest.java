package org.jfrog.build.extractor.client;

import org.jfrog.build.extractor.clientConfiguration.ArtifactSpec;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.testng.Assert.*;


/**
 * @author Yoav Landman
 */
@Test
public class ArtifactSpecTest {

    @Test
    public void stringConstruction() {
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
        assertEquals(standard.getProperties(), new HashMap<String, String>() {{
            put("k1", "v1");
            put("k2", "v2");
            put("k3", "v3");
        }});
        assertEquals(noPropSpaces.getConfiguration(), "conf");
        assertEquals(noPropSpaces.getGroup(), "grp");
        assertEquals(noPropSpaces.getName(), "art");
        assertEquals(noPropSpaces.getVersion(), "ver");
        assertEquals(noPropSpaces.getClassifier(), "cls");
        assertEquals(noPropSpaces.getType(), "jar");
        assertEquals(noPropSpaces.getProperties(), new HashMap<String, String>() {{
            put("k1", "v1");
            put("k2", "v2");
            put("k3", "v3");
        }});
        assertEquals(noConf.getConfiguration(), "*");
        assertEquals(noConf.getGroup(), "grp");
        assertEquals(noConf.getName(), "art");
        assertEquals(noConf.getVersion(), "ver");
        assertEquals(noConf.getClassifier(), "cls");
        assertEquals(noConf.getType(), "jar");
        assertEquals(noConf.getProperties(), new HashMap<String, String>() {{
            put("k1", "v1");
            put("k2", "v2");
            put("k3", "v3");
        }});

        assertThrows(IllegalArgumentException.class, () -> ArtifactSpec.newSpec("all grp:art:ver:cls@jar"));
        assertEquals(allConf.getConfiguration(), "*");
        assertEquals(allConf.getGroup(), "grp");
        assertEquals(allConf.getName(), "art");
        assertEquals(allConf.getVersion(), "ver");
        assertEquals(allConf.getClassifier(), "cls");
        assertEquals(allConf.getType(), "jar");
        assertEquals(allConf.getProperties(), new HashMap<String, String>() {{
            put("k1", "v1");
            put("k2", "v2");
            put("k3", "v3");
        }});
    }

    @Test
    public void matches() {
        ArtifactSpec spec =
                ArtifactSpec.builder().configuration("conf").group("grp").name("art").version("ver").classifier("cls")
                        .type("jar").build();
        assertTrue(ArtifactSpec.newSpec("conf grp:art:ver:cls k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* grp:art:ver:cls k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* g?p:art:ver:cls k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* noGrp:art:ver:cls k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf grp:*:*:cls k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:cl* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf *:*:*er:cl* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf *:*:*:* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("conf1 *:*:*er:cl* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* *:*:*et:* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf grp:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* grp:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* g?p:art:ver:cls@jar k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* noGrp:art:ver:cls@jar k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:cl*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf *:*:*er:cl*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("conf *:*:*:*@jar k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("conf1 *:*:*er:cl*@* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* *:*:*et:*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("* *:*:*er:*@j?? k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("* *:*:*er:*@j??? k1:v1").matches(spec));
    }

    @Test
    public void matchesWithNull1() {
        ArtifactSpec spec =
                ArtifactSpec.builder().configuration(null).group("org.jfrog").name("shared")
                        .version("1.0")
                        .classifier(null)
                        .type("jar").build();
        someTests(spec);
    }

    @Test
    public void matchesWithNull2() {
        ArtifactSpec spec = ArtifactSpec.builder().group("org.jfrog").name("shared")
                .version("1.0").build();
        someTests(spec);
    }

    private void someTests(ArtifactSpec spec) {
        assertFalse(ArtifactSpec.newSpec("archives org.jfrog:*:*:*@* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("all org.jfrog:shared:?.?:* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("all org.jfrog:*:*.?:* k1:v1").matches(spec));
        assertTrue(ArtifactSpec.newSpec("all org.jfrog:*:?.?:* k1:v1").matches(spec));
        assertFalse(ArtifactSpec.newSpec("foo org.jfrog:*:?.?:* k1:v1").matches(spec));
    }
}
