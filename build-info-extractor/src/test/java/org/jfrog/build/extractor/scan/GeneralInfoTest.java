package org.jfrog.build.extractor.scan;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class GeneralInfoTest {

    @Test
    public void testEmptyComponentId() {
        GeneralInfo generalInfo = new GeneralInfo().componentId("");
        assertEquals(generalInfo.getComponentId(), "");
        assertEquals(generalInfo.getGroupId(), "");
        assertEquals(generalInfo.getArtifactId(), "");
        assertEquals(generalInfo.getVersion(), "");
    }

    @Test
    public void testNoColon() {
        GeneralInfo generalInfo = new GeneralInfo().componentId("component");
        assertEquals(generalInfo.getComponentId(), "component");
        assertEquals(generalInfo.getGroupId(), "");
        assertEquals(generalInfo.getArtifactId(), "");
        assertEquals(generalInfo.getVersion(), "");
    }

    @Test
    public void testArtifactVersion() {
        GeneralInfo generalInfo = new GeneralInfo().componentId("artifact:1.2.3");
        assertEquals(generalInfo.getComponentId(), "artifact:1.2.3");
        assertEquals(generalInfo.getGroupId(), "");
        assertEquals(generalInfo.getArtifactId(), "artifact");
        assertEquals(generalInfo.getVersion(), "1.2.3");
    }

    @Test
    public void testGroupArtifactVersion() {
        GeneralInfo generalInfo = new GeneralInfo().componentId("group:artifact:1.2.3");
        assertEquals(generalInfo.getComponentId(), "group:artifact:1.2.3");
        assertEquals(generalInfo.getGroupId(), "group");
        assertEquals(generalInfo.getArtifactId(), "artifact");
        assertEquals(generalInfo.getVersion(), "1.2.3");
    }
}
