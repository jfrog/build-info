package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Created by romang on 4/26/16.
 */
public class Aql {

    private LinkedHashMap find;

    public String getFind() throws IOException {
        if (find != null) {
            return "items.find(" + new ObjectMapper().writeValueAsString(find) + ")";
        }
        return null;
    }

    @JsonProperty("items.find")
    public void setFind(LinkedHashMap find) {
        this.find = find;
    }
}
