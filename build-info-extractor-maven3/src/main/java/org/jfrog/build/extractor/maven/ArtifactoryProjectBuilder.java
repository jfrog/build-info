package org.jfrog.build.extractor.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jfrog.build.extractor.ProxySelector;
import org.jfrog.build.extractor.maven.resolver.ArtifactoryPluginResolution;
import org.jfrog.build.extractor.maven.resolver.ResolutionHelper;

import javax.inject.Named;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED;

@Named
@Component(role = DefaultProjectBuilder.class, hint = "default")
public class ArtifactoryProjectBuilder extends DefaultProjectBuilder {

    @Requirement
    private ResolutionHelper resolutionHelper;

    // Guards installIfNeeded() so the permissive SSLContext/HostnameVerifier
    // below is installed at most once per JVM, regardless of how many times
    // build() runs (e.g. a multi-module reactor calls it once per module).
    private static volatile boolean insecureTlsInstalled = false;

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request) throws ProjectBuildingException {
        if (Boolean.parseBoolean(System.getProperties().getProperty(PROP_ARTIFACTORY_RESOLUTION_ENABLED))
                || Boolean.parseBoolean(System.getenv(PROP_ARTIFACTORY_RESOLUTION_ENABLED))) {
            if (!resolutionHelper.isInitialized()) {
                Properties allMavenProps = new Properties();
                allMavenProps.putAll(request.getSystemProperties());
                allMavenProps.putAll(request.getUserProperties());
                resolutionHelper.init(allMavenProps);
            }
            installInsecureTlsIfNeeded();

            // We're setting the resolver repositories to the list of repositories.
            // This repository replaces the central repository.
            // This ensures that parent poms with snapshot versions can be downloaded from Artifactory.
            List<ArtifactRepository> repositories = getRepositories();
            request.setRemoteRepositories(repositories);
            request.setPluginArtifactRepositories(repositories);
        }
        return super.build(pomFiles, recursive, request);
    }

    // --insecure-tls previously only applied to jfrog-cli's own build-info
    // upload client (ArtifactoryManagerBuilder#resolveInsecureTls) - it never
    // affected Maven's OWN artifact resolution, so plugins/dependencies
    // resolved through the artifactory-release/artifactory-snapshot
    // repositories above still failed PKIX validation against a self-signed
    // or otherwise untrusted cert even with the flag set. Maven's resolver
    // has no per-repository "skip TLS validation" hook in its public API, so
    // this installs a permissive default SSLContext/HostnameVerifier for the
    // whole JVM instead - blunt, but --insecure-tls is itself a deliberate,
    // opt-in "trust everything" escape hatch, and this is version-agnostic
    // across whichever HTTP transport a given Maven version bundles (legacy
    // wagon-http or the newer maven-resolver-transport implementations all
    // ultimately go through the JDK's javax.net.ssl default context).
    private void installInsecureTlsIfNeeded() {
        if (insecureTlsInstalled || !resolutionHelper.isInsecureTls()) {
            return;
        }
        synchronized (ArtifactoryProjectBuilder.class) {
            if (insecureTlsInstalled) {
                return;
            }
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
                insecureTlsInstalled = true;
            } catch (Exception e) {
                resolutionHelper.getLogger().warn("Failed to install insecure TLS context for Maven artifact resolution: " + e.getMessage());
            }
        }
    }

    private List<ArtifactRepository> getRepositories() {
        List<ArtifactRepository> repositories = new ArrayList<>();
        ProxySelector proxySelector = new ProxySelector(resolutionHelper.getHttpProxyHost(), resolutionHelper.getHttpProxyPort(), resolutionHelper.getHttpProxyUsername(), resolutionHelper.getHttpProxyPassword(), resolutionHelper.getHttpsProxyHost(), resolutionHelper.getHttpsProxyPort(), resolutionHelper.getHttpsProxyUsername(), resolutionHelper.getHttpsProxyPassword(), resolutionHelper.getNoProxy());

        boolean isSnapshotEnabled = !resolutionHelper.isSnapshotDisabled();
        ArtifactoryPluginResolution artifactoryResolution = new ArtifactoryPluginResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), proxySelector, resolutionHelper.getLogger())
                .setSnapshotEnabled(isSnapshotEnabled)
                .setSnapshotUpdatePolicy(resolutionHelper.getSnapshotUpdatePolicy());
        ArtifactRepository snapshotRepository = artifactoryResolution.createSnapshotRepository();
        if (snapshotRepository != null) {
            repositories.add(snapshotRepository);
        }
        ArtifactRepository releaseRepository = artifactoryResolution.createReleaseRepository();
        if (releaseRepository != null) {
            repositories.add(releaseRepository);
        }
        return repositories;
    }
}
