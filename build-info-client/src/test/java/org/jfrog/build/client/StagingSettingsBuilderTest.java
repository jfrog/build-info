package org.jfrog.build.client;

import org.jfrog.build.api.release.Promotion;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests the build promotion settings helper
 *
 * @author Noam Y. Tenne
 */
@Test
public class StagingSettingsBuilderTest {

    public void testAllParams() {
        String buildName = "name";
        String buildNumber = "number";
        Promotion promotion = new Promotion();

        StagingSettingsBuilder builder = new StagingSettingsBuilder(buildName, buildNumber).promotion(promotion);
        StagingSettings build = builder.build();

        assertEquals(build.getBuildName(), buildName, "Unexpected build name.");
        assertEquals(build.getBuildNumber(), buildNumber, "Unexpected build number.");
        assertEquals(build.getPromotion(), promotion, "Unexpected promotion.");
    }

    public void testDefaultParams() {
        StagingSettings settings = new StagingSettingsBuilder("as", "as").build();
        assertNotNull(settings.getPromotion(), "Default build promotion should not be null.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)name is required(.*)")
    public void testNullBuildName() {
        new StagingSettingsBuilder(null, null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)number is required(.*)")
    public void testNullBuildNumber() {
        new StagingSettingsBuilder("asasds", null).build();
    }
}
