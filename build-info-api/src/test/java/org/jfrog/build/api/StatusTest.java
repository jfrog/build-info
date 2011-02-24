package org.jfrog.build.api;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Noam Y. Tenne
 */
@Test
public class StatusTest {

    public void testConstructor() {
        Status status = new Status(Status.ROLLED_BACK, "momo", "popo", "koko", "jojo");
        assertEquals(status.getStatus(), Status.ROLLED_BACK, "Unexpected status.");
        assertEquals(status.getComment(), "momo", "Unexpected comment.");
        assertEquals(status.getRepository(), "popo", "Unexpected repository.");
        assertEquals(status.getTimestamp(), "koko", "Unexpected timestamp.");
        assertEquals(status.getUser(), "jojo", "Unexpected user.");
    }
}