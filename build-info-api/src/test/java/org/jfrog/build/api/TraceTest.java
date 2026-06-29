package org.jfrog.build.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Tests the Trace model and its serialization / deserialization via the Build class.
 */
@Test
public class TraceTest {

    private static final String TRACE_ID = "43fb38773c526b515370ba5406ce4b89";

    public void testTraceGettersSetters() {
        Trace trace = new Trace();
        assertNull(trace.getTraceId(), "trace_id should be null initially");
        assertNull(trace.getSpanId());
        assertNull(trace.getParentSpanId());
        assertNull(trace.getName());
        assertNull(trace.getStartTime());
        assertNull(trace.getEndTime());
        assertNull(trace.getAttributes());
        assertNull(trace.getSpanKind());

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("span.type", "test_case");
        attrs.put("test.outcome", "pass");

        trace.setTraceId(TRACE_ID);
        trace.setSpanId("0246df031f708709");
        trace.setParentSpanId("8024dcd3910a35e6");
        trace.setName("boost ci-setup github");
        trace.setStartTime("2026-06-22T14:37:56.885735333Z");
        trace.setEndTime("2026-06-22T14:37:56.894949188Z");
        trace.setAttributes(attrs);
        trace.setSpanKind("internal");

        assertEquals(trace.getTraceId(), TRACE_ID);
        assertEquals(trace.getSpanId(), "0246df031f708709");
        assertEquals(trace.getParentSpanId(), "8024dcd3910a35e6");
        assertEquals(trace.getName(), "boost ci-setup github");
        assertEquals(trace.getStartTime(), "2026-06-22T14:37:56.885735333Z");
        assertEquals(trace.getEndTime(), "2026-06-22T14:37:56.894949188Z");
        assertEquals(trace.getAttributes(), attrs);
        assertEquals(trace.getSpanKind(), "internal");
    }

    public void testBuildTracesGetterSetter() {
        Build build = new Build();
        assertNull(build.getTraces(), "traces should be null by default");

        Trace t1 = buildTrace("span1", "parent1", "test run: go");
        Trace t2 = buildTrace("span2", "parent1", "testenv/TestAdd");
        List<Trace> traces = Arrays.asList(t1, t2);

        build.setTraces(traces);

        assertNotNull(build.getTraces());
        assertEquals(build.getTraces().size(), 2);
        assertEquals(build.getTraces().get(0).getSpanId(), "span1");
        assertEquals(build.getTraces().get(1).getName(), "testenv/TestAdd");
    }

    public void testTraceJsonRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("span.type", "test_run");
        attrs.put("test.case_count", 2);
        attrs.put("test.pass_count", 2);
        attrs.put("test.exit_code", 0);

        Trace trace = new Trace();
        trace.setTraceId(TRACE_ID);
        trace.setSpanId("b009c2f52bd10e4a");
        trace.setParentSpanId("679767eacc094f15");
        trace.setName("test run: go");
        trace.setStartTime("2026-06-22T14:37:56.955856491Z");
        trace.setEndTime("2026-06-22T14:38:03.242776111Z");
        trace.setAttributes(attrs);
        trace.setSpanKind("internal");

        String json = mapper.writeValueAsString(trace);

        // Verify snake_case JSON property names
        assert json.contains("\"trace_id\"") : "JSON should use trace_id (snake_case)";
        assert json.contains("\"span_id\"") : "JSON should use span_id";
        assert json.contains("\"parent_span_id\"") : "JSON should use parent_span_id";
        assert json.contains("\"start_time\"") : "JSON should use start_time";
        assert json.contains("\"end_time\"") : "JSON should use end_time";
        assert json.contains("\"span_kind\"") : "JSON should use span_kind";

        Trace deserialized = mapper.readValue(json, Trace.class);
        assertEquals(deserialized.getTraceId(), TRACE_ID);
        assertEquals(deserialized.getSpanId(), "b009c2f52bd10e4a");
        assertEquals(deserialized.getName(), "test run: go");
        assertEquals(deserialized.getAttributes().get("test.case_count"), 2);
    }

    public void testBuildWithTracesJsonRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Trace t1 = buildTrace("span1", "parent0", "step 1");
        Trace t2 = buildTrace("span2", "span1", "step 2");

        Build build = new Build();
        build.setName("my-build");
        build.setNumber("42");
        build.setStarted("2026-06-22T14:37:56.000+0000");
        build.setTraces(Arrays.asList(t1, t2));

        String json = mapper.writeValueAsString(build);
        assert json.contains("\"traces\"") : "Build JSON should include traces field";

        Build deserialized = mapper.readValue(json, Build.class);
        assertNotNull(deserialized.getTraces());
        assertEquals(deserialized.getTraces().size(), 2);
        assertEquals(deserialized.getTraces().get(0).getSpanId(), "span1");
        assertEquals(deserialized.getTraces().get(1).getSpanId(), "span2");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Trace buildTrace(String spanId, String parentSpanId, String name) {
        Trace t = new Trace();
        t.setTraceId(TRACE_ID);
        t.setSpanId(spanId);
        t.setParentSpanId(parentSpanId);
        t.setName(name);
        t.setStartTime("2026-06-22T14:37:56.000Z");
        t.setEndTime("2026-06-22T14:37:57.000Z");
        t.setSpanKind("internal");
        return t;
    }
}
