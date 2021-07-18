package org.jfrog.build.extractor.nuget.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

public class NugetProjectAssets {
    private static final long serialVersionUID = 1L;

    private int version;
    private Map<String, Map<String, TargetDependency>> targets = new LinkedHashMap<>();
    private Map<String, Library> libraries = new LinkedHashMap<>();
    private Project project;

    public NugetProjectAssets() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, Map<String, TargetDependency>> getTargets() {
        return targets;
    }

    public void setTarget(Map<String, Map<String, TargetDependency>> targets) {
        this.targets = targets;
    }

    public Map<String, Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(Map<String, Library> libraries) {
        this.libraries = libraries;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getPackagesPath() {
        return project.restore.packagesPath;
    }

    public static class TargetDependency {
        private Map<String, String> dependencies = new LinkedHashMap<>();

        public TargetDependency() {
        }

        public Map<String, String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(Map<String, String> dependencies) {
            this.dependencies = dependencies;
        }
    }

    public static class Library {
        private String type;
        private String path;
        private List<String> files;

        public Library() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getFiles() {
            return files;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }

        public String getNupkgFilePath() {
            for (String file : files) {
                if (file.endsWith("nupkg.sha512")) {
                    return path + File.separator + file.replace(".sha512", "");
                }
            }
            return StringUtils.EMPTY;
        }
    }

    public static class Project {
        private String version;
        private Restore restore;

        public Project() {
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Restore getRestore() {
            return restore;
        }

        public void setRestore(Restore restore) {
            this.restore = restore;
        }

        public static class Restore {
            private String packagesPath;

            public Restore() {
            }

            public String getPackagesPath() {
                return packagesPath;
            }

            public void setPackagesPath(String packagesPath) {
                this.packagesPath = packagesPath;
            }
        }
    }

    public void readProjectAssets(File projectAssets) throws IOException {
        try (FileInputStream fis = new FileInputStream(projectAssets)) {
            String json = inputStreamToString(fis);
            ObjectMapper mapper = createMapper();
            NugetProjectAssets assets = mapper.readValue(json, NugetProjectAssets.class);
            this.setVersion(assets.getVersion());
            this.setLibraries(assets.getLibraries());
            this.setProject(assets.getProject());
            this.setTarget(assets.targets);
        }
    }

    private String inputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
