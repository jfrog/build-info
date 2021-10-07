package org.jfrog.build.api.ci;

import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Noam Y. Tenne
 */
@Test
public class PromotionStatusTest {

    public void testConstructor() {
        PromotionStatus promotionStatus =
                new PromotionStatus(Promotion.ROLLED_BACK, "momo", "popo", "koko", "jojo", "bobo");
        assertEquals(promotionStatus.getStatus(), Promotion.ROLLED_BACK, "Unexpected status.");
        assertEquals(promotionStatus.getComment(), "momo", "Unexpected comment.");
        assertEquals(promotionStatus.getRepository(), "popo", "Unexpected repository.");
        assertEquals(promotionStatus.getTimestamp(), "koko", "Unexpected timestamp.");
        assertEquals(promotionStatus.getUser(), "jojo", "Unexpected user.");
        assertEquals(promotionStatus.getCiUser(), "bobo", "Unexpected user.");
    }
}