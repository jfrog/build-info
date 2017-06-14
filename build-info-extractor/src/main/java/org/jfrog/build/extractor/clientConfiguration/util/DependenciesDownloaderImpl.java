package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by diman on 12/03/2017.
 */
public class DependenciesDownloaderImpl implements DependenciesDownloader {

    private ArtifactoryDependenciesClient client;
    private File workingDirectory;
    private Log log;
    private boolean flatDownload = false;

    public DependenciesDownloaderImpl(ArtifactoryDependenciesClient client, String workingDirectory, Log log) {
        this.workingDirectory = new File(workingDirectory);
        this.log = log;
        this.flatDownload = false;
        this.client = client;
    }

    @Override
    public ArtifactoryDependenciesClient getClient() {
        return client;
    }

    @Override
    public List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(this, log);
        return helper.downloadDependencies(downloadableArtifacts);
    }

    @Override
    public String getTargetDir(String targetDir, String relativeDir) throws IOException {
        String downloadFileRelativePath = this.flatDownload && relativeDir.contains("/") ?
                StringUtils.substringAfterLast(relativeDir, "/") : relativeDir;
        return FilenameUtils.concat(workingDirectory.getPath(), FilenameUtils.concat(targetDir, downloadFileRelativePath));
    }

    @Override
    public Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException {
        File dest = new File(filePath);
        if (dest.exists()) {
            dest.delete();
        } else {
            dest.getParentFile().mkdirs();
        }

        FileOutputStream fileOutputStream = null;
        try {
            dest.createNewFile();
            fileOutputStream = new FileOutputStream(dest);
            IOUtils.copyLarge(is, fileOutputStream);
        } catch (IOException e) {
            throw new IOException("Could not create or write to file: " + dest.getCanonicalPath() + ". " +
                    e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(fileOutputStream);
        }

        try {
            return FileChecksumCalculator.calculateChecksums(dest, "md5", "sha1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find checksum algorithm: " + e.getLocalizedMessage());
        }
    }

    @Override
    public boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException {
        File dest = new File(filePath);
        if (!dest.exists()) {
            return false;
        }

        if (dest.isDirectory()) {
            throw new IOException(String.format("File can't override an existing directory: %s", dest.toString()));
        }

        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(dest, "md5", "sha1");
            boolean isExists = checksumsMap != null &&
                    StringUtils.isNotBlank(md5) && StringUtils.equals(md5, checksumsMap.get("md5")) &&
                    StringUtils.isNotBlank(sha1) && StringUtils.equals(sha1, checksumsMap.get("sha1"));
            if (isExists) {
                return true;
            } else {
                log.info(String.format("Overriding existing in destination file: %s", dest.toString()));
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find checksum algorithm: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void removeUnusedArtifactsFromLocal(Set<String> allResolvesFiles, Set<String> forDeletionFiles)
            throws IOException {
        for (String resolvedFile : forDeletionFiles) {
            File resolvedFileParent = org.apache.commons.io.FileUtils.getFile(resolvedFile).getParentFile();

            File[] fileSiblings = resolvedFileParent.listFiles();
            if (!(fileSiblings == null || fileSiblings.length == 0)) {

                for (File sibling : fileSiblings) {
                    if (!isResolvedOrParentOfResolvedFile(allResolvesFiles, sibling.getPath())) {
                        log.info("Deleted unresolved file '" + sibling.getPath() + "'");
                        sibling.delete();
                    }
                }
            }
        }
    }

    @Override
    public void setFlatDownload(boolean flat) {
        this.flatDownload = flat;
    }

    private boolean isResolvedOrParentOfResolvedFile(Set<String> resolvedFiles, final String path) {
        return Iterables.any(resolvedFiles, new Predicate<String>() {
            public boolean apply(String filePath) {
                return StringUtils.equals(filePath, path) || StringUtils.startsWith(filePath, path);
            }
        });
    }
}