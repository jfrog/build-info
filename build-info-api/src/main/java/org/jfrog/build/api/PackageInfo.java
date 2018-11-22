package org.jfrog.build.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

public class PackageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;
    private String scope;

    @SuppressWarnings("unused")
    public PackageInfo() {
    }

    public PackageInfo(String name, String version, String scope) {
        this.name = name;
        this.version = version;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    private void removeVersionPrefixes() {
        version = StringUtils.removeStart(version, "v");
        version = StringUtils.removeStart(version, "=");
    }

    private void splitScopeFromName() {
        if (StringUtils.startsWith(name, "@") && StringUtils.contains(name, "/")) {
            String[] splitValues = StringUtils.split(name, "/");
            scope = splitValues[0];
            name = splitValues[1];
        }
    }

    public void readPackageInfo(File ws) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Path packageJsonPath = ws.toPath().resolve("package.json");
        try (FileInputStream fis = new FileInputStream(packageJsonPath.toFile())) {
            PackageInfo packageInfo = mapper.readValue(fis, PackageInfo.class);

            setVersion(packageInfo.getVersion());
            removeVersionPrefixes();

            setName(packageInfo.getName());
            splitScopeFromName();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        PackageInfo other = (PackageInfo) obj;
        return Objects.equals(name, other.getName()) && Objects.equals(version, other.getVersion()) && Objects.equals(scope, other.getScope());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, scope);
    }

    @Override
    public String toString() {
        String nameBase = name + ":" + version;
        if (StringUtils.isBlank(scope)) {
            return nameBase;
        }
        return nameBase + scope.replaceFirst("^@", "");
    }
}
