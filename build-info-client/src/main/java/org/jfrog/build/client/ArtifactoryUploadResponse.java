package org.jfrog.build.client;

import org.apache.http.StatusLine;

import java.util.List;

/**
 * This class represents the response received from Artifactory for artifact deployment requests.
 */
public class ArtifactoryUploadResponse {
    private String repo;
    private String path;
    private String created;
    private String createdBy;
    private String downloadUri;
    private String mimeType;
    private String size;
    private String uri;
    private List<Error> errors;
    private Checksums checksums;
    private Checksums originalChecksums;
    private StatusLine statusLine;

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDownloadUri() {
        return downloadUri;
    }

    public void setDownloadUri(String downloadUri) {
        this.downloadUri = downloadUri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Checksums getChecksums() {
        return checksums;
    }

    public void setChecksums(Checksums checksums) {
        this.checksums = checksums;
    }

    public Checksums getOriginalChecksums() {
        return originalChecksums;
    }

    public void setOriginalChecksums(Checksums originalChecksums) {
        this.originalChecksums = originalChecksums;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

    public void setStatusLine(StatusLine statusLine) {
        this.statusLine = statusLine;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public static class Checksums {
        private String sha1;
        private String md5;
        private String sha256;

        public String getSha256() { return sha256; }

        public void setSha256(String sha256) { this.sha256 = sha256; }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }
    }

    public static class Error {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
