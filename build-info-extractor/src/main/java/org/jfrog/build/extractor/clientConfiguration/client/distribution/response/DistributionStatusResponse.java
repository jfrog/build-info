package org.jfrog.build.extractor.clientConfiguration.client.distribution.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.DistributionRules;

import java.util.List;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class DistributionStatusResponse {
    @JsonProperty("distribution_rules")
    private List<DistributionRules> distributionRules;
    private List<DistributionSiteStatus> sites;
    @JsonProperty("distribution_friendly_id")
    private int friendlyId;
    private String version;
    private String status;
    private String type;
    @JsonProperty("distribution_id")
    private String id;

    public List<DistributionRules> getDistributionRules() {
        return distributionRules;
    }

    public void setDistributionRules(List<DistributionRules> distributionRules) {
        this.distributionRules = distributionRules;
    }

    public List<DistributionSiteStatus> getSites() {
        return sites;
    }

    public void setSites(List<DistributionSiteStatus> sites) {
        this.sites = sites;
    }

    public int getFriendlyId() {
        return friendlyId;
    }

    public void setFriendlyId(int friendlyId) {
        this.friendlyId = friendlyId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static class DistributionSiteStatus {
        @JsonProperty("target_artifactory")
        private TargetArtifactory targetArtifactory;
        @JsonProperty("files_in_progress")
        private List<String> filesInProgress;
        @JsonProperty("file_errors")
        private List<String> fileErrors;
        @JsonProperty("distributed_bytes")
        private int distributedBytes;
        @JsonProperty("distributed_files")
        private int distributedFiles;
        @JsonProperty("general_error")
        private String generalError;
        @JsonProperty("total_files")
        private int totalFiles;
        @JsonProperty("total_bytes")
        private int totalBytes;
        private String status;

        public TargetArtifactory getTargetArtifactory() {
            return targetArtifactory;
        }

        public void setTargetArtifactory(TargetArtifactory targetArtifactory) {
            this.targetArtifactory = targetArtifactory;
        }

        public List<String> getFilesInProgress() {
            return filesInProgress;
        }

        public void setFilesInProgress(List<String> filesInProgress) {
            this.filesInProgress = filesInProgress;
        }

        public List<String> getFileErrors() {
            return fileErrors;
        }

        public void setFileErrors(List<String> fileErrors) {
            this.fileErrors = fileErrors;
        }

        public int getDistributedBytes() {
            return distributedBytes;
        }

        public void setDistributedBytes(int distributedBytes) {
            this.distributedBytes = distributedBytes;
        }

        public int getDistributedFiles() {
            return distributedFiles;
        }

        public void setDistributedFiles(int distributedFiles) {
            this.distributedFiles = distributedFiles;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }

        public int getTotalBytes() {
            return totalBytes;
        }

        public void setTotalBytes(int totalBytes) {
            this.totalBytes = totalBytes;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getGeneralError() {
            return generalError;
        }

        public void setGeneralError(String generalError) {
            this.generalError = generalError;
        }
    }

    public static class TargetArtifactory {
        @JsonProperty("service_id")
        private String serviceId;
        private String name;
        private String type;

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
