package org.jfrog.build.extractor.nuget.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class NugetPackgesConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    @JacksonXmlProperty(localName = "package")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<ConfigPackage> packages;

    @SuppressWarnings("unused")
    public NugetPackgesConfig() {
    }

    public NugetPackgesConfig(String name, List<ConfigPackage> packages) {
        this.name = name;
        this.packages = packages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ConfigPackage> getPackages() {
        return packages;
    }

    public void setPackages(List<ConfigPackage> packages) {
        this.packages = packages;
    }

    public void addPackage(ConfigPackage pkg) {
        if (pkg == null) {
            pkg = new ConfigPackage();
        }
        packages.add(pkg);
    }

    public void readPackageConfig(File packagesConfig) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        try (FileInputStream fis = new FileInputStream(packagesConfig)) {
            String xml = inputStreamToString(fis);
            NugetPackgesConfig config = xmlMapper.readValue(xml, NugetPackgesConfig.class);
            this.setName(config.getName());
            this.setPackages(config.getPackages());
        }
    }

    private String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConfigPackage {

        @JacksonXmlProperty(localName = "id")
        private String id;
        @JacksonXmlProperty(localName = "version")
        private String version;

        public ConfigPackage() {
        }

        public ConfigPackage(String id, String version) {
            this.id = id;
            this.version = version;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
