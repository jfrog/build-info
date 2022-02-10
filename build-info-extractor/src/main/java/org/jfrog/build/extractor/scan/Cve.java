package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author yahavi
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Cve {
    private String cveId;
    private String cvssV1;
    private String cvssV2;

    public Cve(String cveId, String cvssV1, String cvssV2) {
        this.cveId = cveId;
        this.cvssV1 = cvssV1;
        this.cvssV2 = cvssV2;
    }

    @SuppressWarnings("unused")
    public String getCveId() {
        return cveId;
    }

    @SuppressWarnings("unused")
    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    @SuppressWarnings("unused")
    public String getCvssV1() {
        return cvssV1;
    }

    @SuppressWarnings("unused")
    public void setCvssV1(String cvssV1) {
        this.cvssV1 = cvssV1;
    }

    @SuppressWarnings("unused")
    public String getCvssV2() {
        return cvssV2;
    }

    @SuppressWarnings("unused")
    public void setCvssV2(String cvssV2) {
        this.cvssV2 = cvssV2;
    }
}
