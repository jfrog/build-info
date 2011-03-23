package org.jfrog.build.client;

import org.jfrog.build.api.release.Promotion;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests the REST promotion command URL construction methods of the buid promotion settings
 *
 * @author Noam Y. Tenne
 */
@Test
public class StagingSettingsTest {

    public void testValues() {
        Promotion promotion = new Promotion();
        StagingSettings settings = new StagingSettings("name", "number", promotion);
        assertEquals(settings.getBuildName(), "name", "Unexpected build name.");
        assertEquals(settings.getBuildNumber(), "number", "Unexpected build number.");
        assertEquals(settings.getPromotion(), promotion, "Unexpected promotion object.");
    }
}
