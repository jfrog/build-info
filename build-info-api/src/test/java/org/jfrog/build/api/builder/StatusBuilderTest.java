package org.jfrog.build.api.builder;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.Status;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Noam Y. Tenne
 */
@Test
public class StatusBuilderTest {

    public void testDefaultValues() {
        Status status = new StatusBuilder(Status.RELEASED).repository("bla").timestamp("bla").user("bla").build();
        assertEquals(status.getStatus(), Status.RELEASED, "Unexpected status type.");
        assertNull(status.getComment(), "Expected null comment by default.");
        assertEquals(status.getRepository(), "bla", "Unexpected status repository.");
        assertEquals(status.getTimestamp(), "bla", "Unexpected status timestamp.");
        assertEquals(status.getUser(), "bla", "Unexpected status user.");
    }

    public void testNormalValues() {
        StatusBuilder builder = new StatusBuilder(Status.ROLLED_BACK).comment("momo").repository("popo").
                timestamp("koko").user("jojo");
        Status status = builder.build();
        assertEquals(status.getStatus(), Status.ROLLED_BACK, "Unexpected status.");
        assertEquals(status.getComment(), "momo", "Unexpected comment.");
        assertEquals(status.getRepository(), "popo", "Unexpected repository.");
        assertEquals(status.getTimestamp(), "koko", "Unexpected timestamp.");
        assertEquals(status.getUser(), "jojo", "Unexpected user.");

        Date date = new Date();
        String expectedTimeStamp = new SimpleDateFormat(Build.STARTED_FORMAT).format(date);
        builder.timestampDate(date);
        status = builder.build();
        assertEquals(status.getTimestamp(), expectedTimeStamp, "Unexpected timestamp.");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullDateForTimestamp() {
        new StatusBuilder(null).timestampDate(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullStatusType() {
        new StatusBuilder(null).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullRepository() {
        new StatusBuilder(Status.STAGED).build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullTimestamp() {
        new StatusBuilder(Status.STAGED).repository("bla").build();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullUser() {
        new StatusBuilder(Status.STAGED).repository("bla").timestamp("bla").build();
    }
}
