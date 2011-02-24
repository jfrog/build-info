package org.jfrog.build.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;

import static org.testng.Assert.assertEquals;

/**
 * Tests the REST promotion command URL construction methods of the buid promotion settings
 *
 * @author Noam Y. Tenne
 */
@Test
public class StagingSettingsTest {

    private static final StagingSettingsBuilder BASIC_BUILDER =
            new StagingSettingsBuilder("buildName", "buildNumber", "targetRepo").includeArtifacts(false).
                    includeDependencies(true).dryRun(true);

    public void testMinimalUrlConstruction() throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder();
        BASIC_BUILDER.build().buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1",
                "Unexpected URL.");
    }

    public void testUrlConstructionWithStartDate() throws UnsupportedEncodingException {
        StagingSettings build = getBuilderCopy().buildStarted("started").build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "started=started", "Unexpected URL.");
    }

    public void testUrlConstructionWithPromotionStatus() throws UnsupportedEncodingException {
        StagingSettings build = getBuilderCopy().promotionStatus("promotionStatus").build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "status=promotionStatus", "Unexpected URL.");
    }

    public void testUrlConstructionWithPromotionComment() throws UnsupportedEncodingException {
        StagingSettings build = getBuilderCopy().promotionComment("promotionComment").build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "comment=promotionComment", "Unexpected URL.");
    }

    public void testUrlConstructionWithPromotionScopes() throws UnsupportedEncodingException {
        StagingSettings build = getBuilderCopy().scopes(Sets.newHashSet("hey", "ho")).build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "scopes=hey,ho", "Unexpected URL.");
    }

    public void testUrlConstructionWithPromotionProperties() throws UnsupportedEncodingException {
        Multimap<String, String> properties = HashMultimap.create();
        properties.put("momo", "domo");
        properties.put("momo", "komo");

        properties.put("popo", "lopo");
        properties.put("popo", "kopo");

        StagingSettings build = getBuilderCopy().properties(properties).build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "properties=popo=kopo,lopo|momo=komo,domo", "Unexpected URL.");
    }

    public void testFullUrlConstruction() throws UnsupportedEncodingException {
        Multimap<String, String> properties = HashMultimap.create();
        properties.put("1", "2");
        properties.put("1", "3");

        properties.put("5", "6");
        properties.put("5", "7");

        StagingSettings build = getBuilderCopy().buildStarted("started").promotionStatus("promotionStatus").
                promotionComment("promotionComment").scopes(Sets.newHashSet("hey", "ho")).properties(properties).
                build();
        StringBuilder urlBuilder = new StringBuilder();
        build.buildUrl(urlBuilder);
        assertEquals(urlBuilder.toString(), "/move/buildName/buildNumber?to=targetRepo&arts=0&deps=1&dry=1&" +
                "started=started&scopes=hey,ho&properties=1=3,2|5=7,6&status=promotionStatus&comment=promotionComment",
                "Unexpected URL.");
    }

    private StagingSettingsBuilder getBuilderCopy() {
        return new StagingSettingsBuilder(BASIC_BUILDER);
    }
}
