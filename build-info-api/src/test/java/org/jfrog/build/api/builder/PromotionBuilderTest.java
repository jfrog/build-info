package org.jfrog.build.api.builder;

import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PromotionBuilderTest {

    public void testDefaultValues() {
        Promotion promotion = new PromotionBuilder().build();
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
        assertTrue(promotion.isFailFast(), "Unexpected default fail-fast state.");
    }

    public void testNormalValues() {
        Set<String> scopes = new HashSet<>();
        Map<String, Collection<String>> properties = new HashMap<>();

        Promotion promotion = new PromotionBuilder().status(Promotion.ROLLED_BACK).comment("comment").ciUser("ciUser").
                timestamp("timestamp").dryRun(true).targetRepo("targetRepo").sourceRepo("sourceRepo").copy(false).
                artifacts(true).dependencies(false).scopes(scopes).properties(properties).failFast(false).build();

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

    public void testAddScopesAndPropertiesToEmptyDefaults() {
        Promotion build = new PromotionBuilder().addProperty("momo", "popo").addScope("koko").build();

        Map<String, Collection<String>> properties = build.getProperties();
        Set<String> scopes = build.getScopes();

        assertNotNull(properties, "Properties multimap should have been created.");
        assertNotNull(scopes, "Scope set should have been created.");

        assertFalse(properties.isEmpty(), "Added properties should have been added.");
        assertFalse(scopes.isEmpty(), "Added scopes should have been added.");

        assertEquals(properties.get("momo").iterator().next(), "popo", "Unexpected properties value.");
        assertTrue(scopes.contains("koko"), "Unable to find expected scopes value.");
    }

    public void testAddScopesAndPropertiesToExistingCollections() {
        Set<String> initialScopes = CommonUtils.newHashSet("koko");
        Map<String, Collection<String>> initialProperties = new HashMap<>();
        initialProperties.put("momo", CommonUtils.newHashSet("popo"));

        Promotion build = new PromotionBuilder().properties(initialProperties).addProperty("jojo", "lolo").
                scopes(initialScopes).addScope("bobo").build();

        Map<String, Collection<String>> properties = build.getProperties();
        Set<String> scopes = build.getScopes();

        assertNotNull(properties, "Properties multimap should have been created.");
        assertNotNull(scopes, "Scope set should have been created.");

        assertFalse(properties.isEmpty(), "Added properties should have been added.");
        assertFalse(scopes.isEmpty(), "Added scopes should have been added.");

        assertEquals(properties.get("momo").iterator().next(), "popo", "Unexpected properties value.");
        assertEquals(properties.get("jojo").iterator().next(), "lolo", "Unexpected properties value.");
        assertTrue(scopes.contains("koko"), "Unable to find expected scopes value.");
        assertTrue(scopes.contains("bobo"), "Unable to find expected scopes value.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullDateForTimestamp() {
        new PromotionBuilder().timestampDate(null);
    }
}
