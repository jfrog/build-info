package org.jfrog.build.extractor.clientConfiguration.client.distribution;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.CreateReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DeleteReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DistributeReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.UpdateReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributeReleaseBundleResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.GetReleaseBundleStatusResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.services.*;

import java.io.IOException;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class DistributionManager extends ManagerBase {
    public DistributionManager(String url, String username, String password, String accessToken, Log logger) {
        super(url, username, password, accessToken, logger);
    }

    public DistributionManager(String url, String username, String password, Log log) {
        super(url, username, password, StringUtils.EMPTY, log);
    }

    public DistributionManager(String url, String accessToken, Log log) {
        super(url, StringUtils.EMPTY, StringUtils.EMPTY, accessToken, log);
    }

    public DistributionManager(String url, Log log) {
        super(url, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, log);
    }

    /**
     * Get version of JFrog distribution.
     *
     * @return version of JFrog distribution
     * @throws IOException in case of any error
     */
    public org.jfrog.build.client.Version getVersion() throws IOException {
        Version versionService = new Version(log);
        return versionService.execute(jfrogHttpClient);
    }

    /**
     * Create a release bundle.
     *
     * @param request       - The release bundle details
     * @param gpgPassphrase - GPG passphrase
     * @throws IOException in case of any error
     */
    public void createReleaseBundle(CreateReleaseBundleRequest request, String gpgPassphrase) throws IOException {
        new CreateReleaseBundle(request, gpgPassphrase, log).execute(jfrogHttpClient);
    }

    /**
     * Create a release bundle.
     *
     * @param request - The release bundle details
     * @throws IOException in case of any error
     */
    public void createReleaseBundle(CreateReleaseBundleRequest request) throws IOException {
        createReleaseBundle(request, "");
    }

    /**
     * Update a release bundle.
     *
     * @param request       - The release bundle details
     * @param gpgPassphrase - GPG passphrase
     * @throws IOException in case of any error
     */
    public void updateReleaseBundle(String name, String version, UpdateReleaseBundleRequest request, String gpgPassphrase) throws IOException {
        new UpdateReleaseBundle(request, name, version, gpgPassphrase, log).execute(jfrogHttpClient);
    }

    /**
     * Update a release bundle.
     *
     * @param request - The release bundle details
     * @throws IOException in case of any error
     */
    public void updateReleaseBundle(String name, String version, UpdateReleaseBundleRequest request) throws IOException {
        updateReleaseBundle(name, version, request, "");
    }

    /**
     * Sign a release bundle
     *
     * @param name              - Release bundle name to sign
     * @param version           - Release bundle version to sign
     * @param gpgPassphrase     - GPG passphrase
     * @param storingRepository - The storing repository
     * @throws IOException in case of any error
     */
    public void signReleaseBundle(String name, String version, String gpgPassphrase, String storingRepository) throws IOException {
        new SignReleaseBundle(name, version, gpgPassphrase, storingRepository, log).execute(jfrogHttpClient);
    }

    /**
     * Sign a release bundle
     *
     * @param name          - Release bundle name to sign
     * @param version       - Release bundle version to sign
     * @param gpgPassphrase - GPG passphrase
     * @throws IOException in case of any error
     */
    public void signReleaseBundle(String name, String version, String gpgPassphrase) throws IOException {
        signReleaseBundle(name, version, gpgPassphrase, "");
    }

    /**
     * Get release bundle status.
     *
     * @param name    - Release bundle name
     * @param version - Release bundle version
     * @return release bundle status
     * @throws IOException in case of any error
     */
    public GetReleaseBundleStatusResponse getReleaseBundleStatus(String name, String version) throws IOException {
        return new GetReleaseBundleVersion(name, version, log).execute(jfrogHttpClient);
    }

    /**
     * Distribute a release bundle.
     *
     * @param name    - Release bundle name
     * @param version - Release bundle version
     * @param sync    - Set to true to enable sync distribution
     * @param request - The distribution details
     * @return the tracker id and sites details
     * @throws IOException in case of any error
     */
    public DistributeReleaseBundleResponse distributeReleaseBundle(String name, String version, boolean sync, DistributeReleaseBundleRequest request) throws IOException {
        return new DistributeReleaseBundle(name, version, sync, request, log).execute(jfrogHttpClient);
    }

    /**
     * Get status of a distributed release bundle
     *
     * @param name    - Release bundle name
     * @param version - Release bundle version
     * @return status of a distributed release bundle
     * @throws IOException in case of any error
     */
    public DistributionStatusResponse getDistributionStatus(String name, String version) throws IOException {
        return new GetDistributionStatus(name, version, "", log).execute(jfrogHttpClient);
    }

    /**
     * Get status of a distributed release bundle
     *
     * @param name      - Release bundle name
     * @param version   - Release bundle version
     * @param trackerId - The tracker ID received from distributeReleaseBundle command
     * @return status of a distributed release bundle
     * @throws IOException in case of any error
     */
    public DistributionStatusResponse getDistributionStatus(String name, String version, String trackerId) throws IOException {
        return new GetDistributionStatus(name, version, trackerId, log).execute(jfrogHttpClient);
    }

    /**
     * Delete a release bundle from local Artifactory
     *
     * @param name    - Release bundle name
     * @param version - Release bundle version
     * @throws IOException in case of any error
     */
    public void deleteLocalReleaseBundle(String name, String version) throws IOException {
        new DeleteLocalReleaseBundle(name, version, log).execute(jfrogHttpClient);
    }

    /**
     * Delete a release bundle from edge node, and optionally from the local Artifactory.
     *
     * @param name    - Release bundle name
     * @param version - Release bundle version
     * @param sync    - Set to true to enable sync deletion
     * @param request - The distribution details
     * @return sites details
     * @throws IOException in case of any error
     */
    public DistributeReleaseBundleResponse deleteReleaseBundle(String name, String version, boolean sync, DeleteReleaseBundleRequest request) throws IOException {
        return new DeleteReleaseBundle(name, version, sync, request, log).execute(jfrogHttpClient);
    }
}
