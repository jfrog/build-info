package build.generic

import com.google.common.base.Predicate
import com.google.common.collect.Iterables
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.jfrog.build.api.Dependency
import org.jfrog.build.api.dependency.DownloadableArtifact
import org.jfrog.build.api.util.FileChecksumCalculator
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloader
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper

/**
 * @author Lior Hasson  
 */
class DependenciesDownloaderImpl implements DependenciesDownloader {

    String workspace 
    BuildInfoLog buildInfoLog 
    ArtifactoryDependenciesClient client 

    DependenciesDownloaderImpl(ArtifactoryDependenciesClient client, String workspace, BuildInfoLog buildInfoLog) {
        this.client = client
        this.workspace = workspace
        this.buildInfoLog = buildInfoLog
    }

    @Override
    ArtifactoryDependenciesClient getClient() {
        return client
    }

    @Override
    List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(this, buildInfoLog) 
        return helper.downloadDependencies(downloadableArtifacts)
    }

    @Override
    String getTargetDir(String targetDir, String relativeDir) throws IOException {
        return FilenameUtils.concat(FilenameUtils.concat(workspace, targetDir), relativeDir)
    }

    @Override
    Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        try{
            File newFile = new File(filePath) 
            FileUtils.copyInputStreamToFile(is, newFile)

            return FileChecksumCalculator.calculateChecksums(newFile, "md5", "sha1") 
        } catch (Exception e) {
            buildInfoLog.warn("Caught exception while saving dependency file" + e.getLocalizedMessage()) 
        } finally {
            IOUtils.closeQuietly(is) 
        }

        return null
    }

    @Override
    boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException {
        return false
    }

    @Override
    public void removeUnusedArtifactsFromLocal(Set<String> allResolvesFiles, Set<String> forDeletionFiles) throws IOException {
        try {
            for (String resolvedFile : forDeletionFiles) {
                File resolvedFileParent = new File(resolvedFile).getParentFile() 

                File[] fileSiblings = resolvedFileParent.listFiles() 
                if (!(fileSiblings == null || fileSiblings.length == 0)) {

                    for (File sibling : fileSiblings) {
                        if (!isResolvedOrParentOfResolvedFile(allResolvesFiles, sibling.getPath())) {
                            buildInfoLog.info("Deleted unresolved file '" + sibling.getPath() + "'") 
                            sibling.delete() 
                        }
                    }
                }
            }
        } catch (Exception e) {
            buildInfoLog.warn("Caught interrupted exception: " + e.getLocalizedMessage()) 
        }
    }

    private static boolean isResolvedOrParentOfResolvedFile(Set<String> resolvedFiles, final String path) {
        return Iterables.any(resolvedFiles, new Predicate<String>() {
            public boolean apply(String filePath) {
                return StringUtils.equals(filePath, path) || StringUtils.startsWith(filePath, path) 
            }
        }) 
    }
}
