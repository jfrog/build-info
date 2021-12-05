package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;
import org.apache.http.protocol.HTTP;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB;

public class Upload extends JFrogService<ArtifactoryUploadResponse> {
    public static final String SHA1_HEADER_NAME = "X-Checksum-Sha1";
    public static final String MD5_HEADER_NAME = "X-Checksum-Md5";
    public static final String EXPLODE_HEADER_NAME = "X-Explode-Archive";
    public static final String CHECKSUM_DEPLOY_HEADER_NAME = "X-Checksum-Deploy";
    private final DeployDetails details;
    private final String logPrefix;
    private final Integer minChecksumDeploySizeKb;
    private boolean isExplode;

    public Upload(DeployDetails details, String logPrefix, Integer minChecksumDeploySizeKb, Log logger) {
        super(logger);
        this.minChecksumDeploySizeKb = minChecksumDeploySizeKb;
        this.details = details;
        this.logPrefix = logPrefix == null ? "" : logPrefix + " ";
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPut request = createHttpPutMethod(details);
        // add the 100 continue directive
        request.addHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);
        if (details.isExplode()) {
            this.isExplode = true;
            request.addHeader(EXPLODE_HEADER_NAME, "true");
        }
        FileEntity fileEntity = new FileEntity(details.getFile(), "binary/octet-stream");
        request.setEntity(fileEntity);
        return request;
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to upload file");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        if (!isExplode) {
            result = getMapper().readValue(stream, ArtifactoryUploadResponse.class);
        }
    }

    @Override
    public ArtifactoryUploadResponse execute(JFrogHttpClient client) throws IOException {
        log.info(logPrefix + "Deploying artifact: " + client.getUrl() + "/" + StringUtils.removeStart(buildDefaultUploadPath(details), "/"));
        ArtifactoryUploadResponse response = tryChecksumUpload(client);
        if (response != null) {
            // Checksum deploy was performed:
            return response;
        }
        return super.execute(client);
    }

    private ArtifactoryUploadResponse tryChecksumUpload(JFrogHttpClient client) {
        try {
            TryChecksumUpload tryChecksumUploadService = new TryChecksumUpload(details, logPrefix, minChecksumDeploySizeKb, log);
            return tryChecksumUploadService.execute(client);
        } catch (IOException e) {
            log.debug("Failed artifact checksum deploy of file " + details.getArtifactPath() + " : " + details.getSha1());
        }
        return null;
    }

    private HttpPut createHttpPutMethod(DeployDetails details) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder().append(buildDefaultUploadPath(details));
        deploymentPathBuilder.append(DeploymentUrlUtils.buildMatrixParamsString(details.getProperties(), true));
        HttpPut httpPut = new HttpPut(deploymentPathBuilder.toString());
        httpPut.addHeader(SHA1_HEADER_NAME, details.getSha1());
        httpPut.addHeader(MD5_HEADER_NAME, details.getMd5());
        log.debug("Full Artifact Http path: " + httpPut + "\n@Http Headers: " + Arrays.toString(httpPut.getAllHeaders()));
        return httpPut;
    }

    private String buildDefaultUploadPath(DeployDetails details) {
        List<String> pathComponents = new ArrayList<>();
        pathComponents.add(DeploymentUrlUtils.encodePath(details.getTargetRepository()));
        pathComponents.add(DeploymentUrlUtils.encodePath(details.getArtifactPath()));
        return StringUtils.join(pathComponents, "/");
    }

    private class TryChecksumUpload extends JFrogService<ArtifactoryUploadResponse> {
        private final DeployDetails details;
        private final String logPrefix;
        private final Integer minChecksumDeploySizeKb;

        private TryChecksumUpload(DeployDetails details, String logPrefix, Integer minChecksumDeploySizeKb, Log logger) {
            super(logger);
            this.details = details;
            this.logPrefix = logPrefix;
            this.minChecksumDeploySizeKb = minChecksumDeploySizeKb != null ? minChecksumDeploySizeKb : DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB;
        }

        @Override
        protected void ensureRequirements(JFrogHttpClient client) throws IOException {
            Version versionService = new Version(log);
            ArtifactoryVersion version = versionService.execute(client);
            // Artifactory 2.5.1+ has efficient checksum deployment (checks if the artifact already exists by it's checksum)
            if (!version.isAtLeast(new ArtifactoryVersion("2.5.1"))) {
                throw new IOException("Artifactory below 2.5.0 doesnt have efficient checksum deployment");
            }
        }

        @Override
        public HttpRequestBase createRequest() throws IOException {
            // Try checksum upload only on file size equal or greater than 'minChecksumDeploySizeKb'
            long fileLength = details.getFile().length();
            if (fileLength < minChecksumDeploySizeKb * 1024) {
                log.debug("Skipping checksum deploy of file size " + fileLength + " bytes, falling back to regular deployment.");
                throw new IOException();
            }

            if (details.isExplode()) {
                log.debug("Skipping checksum deploy due to explode file request.");
                throw new IOException();
            }

            HttpPut request = createHttpPutMethod(details);
            // activate checksum deploy
            request.addHeader(CHECKSUM_DEPLOY_HEADER_NAME, "true");

            return request;
        }

        @Override
        protected void setResponse(InputStream stream) throws IOException {
            result = getMapper().readValue(stream, ArtifactoryUploadResponse.class);
        }
    }
}
