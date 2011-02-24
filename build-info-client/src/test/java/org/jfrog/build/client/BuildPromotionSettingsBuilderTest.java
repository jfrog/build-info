package org.jfrog.build.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests the build promotion settings helper
 *
 * @author Noam Y. Tenne
 */
@Test
public class BuildPromotionSettingsBuilderTest {

    public void testAllParams() {
        String buildName = "name";
        String buildNumber = "number";
        String buildStarted = "started";
        String targetRepo = "targetRepo";
        boolean includeArtifacts = false;
        boolean includeDependencies = true;
        Set<String> scopes = Sets.newHashSet();
        Multimap<String, String> properties = HashMultimap.create();
        boolean dryRun = true;
        String promotionStatus = "promotionStatus";
        String promotionComment = "promotionComment";

        BuildPromotionSettingsBuilder builder = new BuildPromotionSettingsBuilder(buildName, buildNumber, targetRepo)
                .buildStarted(buildStarted).includeArtifacts(includeArtifacts).includeDependencies(includeDependencies).
                        scopes(scopes).properties(properties).dryRun(dryRun).promotionStatus(promotionStatus).
                        promotionComment(promotionComment);
        BuildPromotionSettings build = builder.build();

        assertEquals(build.getBuildName(), buildName, "Unexpected build name.");
        assertEquals(build.getBuildNumber(), buildNumber, "Unexpected build number.");
        assertEquals(build.getBuildStarted(), buildStarted, "Unexpected build started.");
        assertEquals(build.getTargetRepo(), targetRepo, "Unexpected target repo.");
        assertEquals(build.isIncludeArtifacts(), includeArtifacts, "Unexpected artifact inclusion switch value.");
        assertEquals(build.isIncludeDependencies(), includeDependencies,
                "Unexpected dependency inclusion switch value.");
        assertEquals(build.getScopes(), scopes, "Unexpected scopes.");
        assertEquals(build.getProperties(), properties, "Unexpected properties.");
        assertEquals(build.isDryRun(), dryRun, "Unexpected dry run switch value.");
        assertEquals(build.getPromotionStatus(), promotionStatus, "Unexpected promotion status.");
        assertEquals(build.getPromotionComment(), promotionComment, "Unexpected promotion comment.");

        builder.buildName("momo").buildNumber("popo").targetRepo("jojo");
        build = builder.build();
        assertEquals(build.getBuildName(), "momo", "Unexpected build name.");
        assertEquals(build.getBuildNumber(), "popo", "Unexpected build number.");
        assertEquals(build.getTargetRepo(), "jojo", "Unexpected target repo.");
    }

    public void testDefaultParams() {
        BuildPromotionSettings build = new BuildPromotionSettingsBuilder("as", "as", "as").build();
        assertTrue(build.isIncludeArtifacts(), "Artifacts should be included by default.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)name is required(.*)")
    public void testNullBuildName() {
        new BuildPromotionSettingsBuilder(null, null, null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)number is required(.*)")
    public void testNullBuildNumber() {
        new BuildPromotionSettingsBuilder("asasds", null, null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "(.*)repository is required(.*)")
    public void testNullTargetRepo() {
        new BuildPromotionSettingsBuilder("asasds", "asdasd", null).build();
    }
}
