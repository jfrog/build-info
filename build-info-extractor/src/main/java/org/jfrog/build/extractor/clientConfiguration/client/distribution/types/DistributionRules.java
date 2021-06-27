package org.jfrog.build.extractor.clientConfiguration.client.distribution.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class DistributionRules {
    @JsonProperty("country_codes")
    private List<String> countryCodes;
    @JsonProperty("site_name")
    private String siteName;
    @JsonProperty("city_name")
    private String cityName;

    public List<String> getCountryCodes() {
        return countryCodes;
    }

    public void setCountryCodes(List<String> countryCodes) {
        this.countryCodes = countryCodes;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public static class Builder {
        private List<String> countryCodes;
        private String siteName;
        private String cityName;

        public Builder countryCodes(List<String> countryCodes) {
            this.countryCodes = countryCodes;
            return this;
        }

        public Builder siteName(String siteName) {
            this.siteName = siteName;
            return this;
        }

        public Builder cityName(String cityName) {
            this.cityName = cityName;
            return this;
        }

        public DistributionRules build() {
            DistributionRules distributionRules = new DistributionRules();
            distributionRules.setCountryCodes(countryCodes);
            distributionRules.setSiteName(siteName);
            distributionRules.setCityName(cityName);
            return distributionRules;
        }
    }
}