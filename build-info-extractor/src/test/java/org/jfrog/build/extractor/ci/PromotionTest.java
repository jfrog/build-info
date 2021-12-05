package org.jfrog.build.extractor.ci;

import org.jfrog.build.api.release.Promotion;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PromotionTest {

    public void testDefaultValues() {
        Promotion promotion = new Promotion();
        assertNull(promotion.getStatus(), "Unexpected default status.");
        assertNull(promotion.getComment(), "Unexpected default comment.");
        assertNull(promotion.getCiUser(), "Unexpected default ci user.");
        assertNull(promotion.getTimestamp(), "Unexpected default timestamp.");
        assertFalse(promotion.isDryRun(), "Unexpected default dry run state.");
        assertNull(promotion.getTargetRepo(), "Unexpected default target repo.");
        assertNull(promotion.getSourceRepo(), "Unexpected default source repo.");
        assertFalse(promotion.isCopy(), "Unexpected default copy state.");
        assertTrue(promotion.isArtifacts(), "Unexpected default artifacts state.");
        assertFalse(promotion.isDependencies(), "Unexpected default dependencies state.");
        assertNull(promotion.getScopes(), "Unexpected default scopes.");
        assertNull(promotion.getProperties(), "Unexpected default properties.");
    }

    public void testConstructor() {
        Set<String> scopes = new HashSet<>();
        Map<String, Collection<String>> properties = new HashMap<>();

        Promotion promotion = new Promotion(Promotion.ROLLED_BACK, "comment", "ciUser", "timestamp",
                true, "targetRepo", "sourceRepo", false, true, false, scopes, properties, false);

        assertEquals(promotion.getStatus(), Promotion.ROLLED_BACK, "Unexpected status.");
        assertEquals(promotion.getComment(), "comment", "Unexpected comment.");
        assertEquals(promotion.getCiUser(), "ciUser", "Unexpected ci user.");
        assertEquals(promotion.getTimestamp(), "timestamp", "Unexpected timestamp.");
        assertTrue(promotion.isDryRun(), "Unexpected dry run state.");
        assertEquals(promotion.getTargetRepo(), "targetRepo", "Unexpected target repo.");
        assertEquals(promotion.getSourceRepo(), "sourceRepo", "Unexpected source repo.");
        assertFalse(promotion.isCopy(), "Unexpected copy state.");
        assertTrue(promotion.isArtifacts(), "Unexpected artifacts state.");
        assertFalse(promotion.isDependencies(), "Unexpected dependencies state.");
        assertEquals(promotion.getScopes(), scopes, "Unexpected scopes.");
        assertEquals(promotion.getProperties(), properties, "Unexpected properties.");
        assertFalse(promotion.isFailFast(), "Unexpected fail-fast state.");
    }

    public void testSetters() {
        Set<String> scopes = new HashSet<>();
        Map<String, Collection<String>> properties = new HashMap<>();

        Promotion promotion = new Promotion();
        promotion.setStatus(Promotion.ROLLED_BACK);
        promotion.setComment("comment");
        promotion.setCiUser("ciUser");
        promotion.setTimestamp("timestamp");
        promotion.setDryRun(true);
        promotion.setTargetRepo("targetRepo");
        promotion.setSourceRepo("sourceRepo");
        promotion.setCopy(false);
        promotion.setArtifacts(true);
        promotion.setDependencies(false);
        promotion.setScopes(scopes);
        promotion.setProperties(properties);
        promotion.setFailFast(false);

        assertEquals(promotion.getStatus(), Promotion.ROLLED_BACK, "Unexpected status.");
        assertEquals(promotion.getComment(), "comment", "Unexpected comment.");
        assertEquals(promotion.getCiUser(), "ciUser", "Unexpected ci user.");
        assertEquals(promotion.getTimestamp(), "timestamp", "Unexpected timestamp.");
        assertTrue(promotion.isDryRun(), "Unexpected dry run state.");
        assertEquals(promotion.getTargetRepo(), "targetRepo", "Unexpected target repo.");
        assertEquals(promotion.getSourceRepo(), "sourceRepo", "Unexpected source repo.");
        assertFalse(promotion.isCopy(), "Unexpected copy state.");
        assertTrue(promotion.isArtifacts(), "Unexpected artifacts state.");
        assertFalse(promotion.isDependencies(), "Unexpected dependencies state.");
        assertEquals(promotion.getScopes(), scopes, "Unexpected scopes.");
        assertEquals(promotion.getProperties(), properties, "Unexpected properties.");
        assertFalse(promotion.isFailFast(), "Unexpected fail-fast state.");
    }

    public void testNullTimestampDateGetter() {
        Promotion promotion = new Promotion(null, null, null, null, true, null, null, true, true, true, null, null, false);
        assertNull(promotion.getTimestampDate(), "No timestamp was set. Should have received null");
    }

    public void testTimestampDateGetters() {
        SimpleDateFormat format = new SimpleDateFormat(BuildInfo.STARTED_FORMAT);

        Date timestampDate = new Date();

        Promotion promotion = new Promotion(null, null, null, format.format(timestampDate), true, null, null, true, true,
                true, null, null, false);
        assertEquals(promotion.getTimestampDate(), timestampDate, "Unexpected timestamp date.");
    }
}