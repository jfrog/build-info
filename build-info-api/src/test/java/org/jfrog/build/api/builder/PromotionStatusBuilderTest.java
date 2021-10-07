package org.jfrog.build.api.builder;

import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PromotionStatusBuilderTest {

    public void testDefaultValues() {
        PromotionStatus promotionStatus = new PromotionStatusBuilder(Promotion.RELEASED).timestamp("bla").build();
        assertEquals(promotionStatus.getStatus(), Promotion.RELEASED, "Unexpected status type.");
        assertNull(promotionStatus.getComment(), "Expected null comment by default.");
        assertNull(promotionStatus.getRepository(), "Expected null repository by default.");
        assertNull(promotionStatus.getUser(), "Expected null user by default.");
        assertNull(promotionStatus.getCiUser(), "Expected null CI user by default.");
        assertEquals(promotionStatus.getTimestamp(), "bla", "Unexpected status timestamp.");
    }

    public void testNormalValues() {
        PromotionStatusBuilder builderPromotion = new PromotionStatusBuilder(Promotion.ROLLED_BACK).comment("momo").
                repository("popo").timestamp("koko").user("jojo").ciUser("bobo");
        PromotionStatus promotionStatus = builderPromotion.build();
        assertEquals(promotionStatus.getStatus(), Promotion.ROLLED_BACK, "Unexpected status.");
        assertEquals(promotionStatus.getComment(), "momo", "Unexpected comment.");
        assertEquals(promotionStatus.getRepository(), "popo", "Unexpected repository.");
        assertEquals(promotionStatus.getTimestamp(), "koko", "Unexpected timestamp.");
        assertEquals(promotionStatus.getUser(), "jojo", "Unexpected user.");
        assertEquals(promotionStatus.getCiUser(), "bobo", "Unexpected ci user.");

        Date date = new Date();
        String expectedTimeStamp = new SimpleDateFormat(BuildInfo.STARTED_FORMAT).format(date);
        builderPromotion.timestampDate(date);
        promotionStatus = builderPromotion.build();
        assertEquals(promotionStatus.getTimestamp(), expectedTimeStamp, "Unexpected timestamp.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullDateForTimestamp() {
        new PromotionStatusBuilder(null).timestampDate(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullStatusType() {
        new PromotionStatusBuilder(null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullTimestamp() {
        new PromotionStatusBuilder(Promotion.STAGED).repository("bla").build();
    }
}
