package org.jfrog.build.extractor.clientConfiguration.deploy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Simple value object holding an artifact deploy request details.
 *
 * @author Yossi Shaul
 */
public class DeployDetails implements Comparable<DeployDetails>, Serializable, ProducerConsumerItem {
    /**
     * Artifact deployment path.
     */
    String artifactPath;
    /**
     * The file to deploy.
     */
    File file;
    /**
     * sha1 checksum of the file to deploy.
     */
    String sha1;
    /**
     * md5 checksum of the file to deploy.
     */
    String md5;
    /**
     * sha256 checksum of the file to deploy.
     */
    String sha256;
    /**
     * In case of deploy - is the deployment succeeded.
     */
    private Boolean deploySucceeded;
    /**
     * Properties to attach to the deployed file as matrix params.
     */
    ArrayListMultimap<String, String> properties;
    /**
     * Target deploy repository.
     */
    private String targetRepository;
    /**
     * Explode archive
     */
    private boolean explode;
    /**
     * The package type generated this artifact's deploy details.
     */
    private PackageType packageType;
    /**
     * @return Return the target deployment repository.
     */
    public String getTargetRepository() {
        return targetRepository;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public File getFile() {
        return file;
    }

    public ArrayListMultimap<String, String> getProperties() {
        return properties;
    }

    public String getSha1() {
        return sha1;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256=sha256;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath=artifactPath;
    }

    public Boolean getDeploySucceeded() {
        return deploySucceeded;
    }

    public void setDeploySucceeded(Boolean deploySucceeded) {
        this.deploySucceeded = deploySucceeded;
    }

    public String getMd5() {
        return md5;
    }

    public boolean isExplode() {
        return explode;
    }

    public PackageType getPackageType() {
        return packageType;
    }

    public int compareTo(DeployDetails that) {
        return this.artifactPath.compareTo(that.artifactPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeployDetails details = (DeployDetails) o;
        return artifactPath.equals(details.artifactPath) &&
                targetRepository.equals(details.targetRepository);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(artifactPath);
        return 31 * result + Objects.hashCode(targetRepository);
    }

    public enum PackageType {
        GENERIC, MAVEN, ANT, IVY, GRADLE, GO, NPM
    }

    public static class Builder {
        private DeployDetails deployDetails;

        public Builder() {
            deployDetails = new DeployDetails();
        }

        public DeployDetails build() {
            if (deployDetails.file == null || !deployDetails.file.exists()) {
                throw new IllegalArgumentException("File not found: " + deployDetails.file);
            }
            if (StringUtils.isBlank(deployDetails.targetRepository)) {
                throw new IllegalArgumentException("Target repository cannot be empty");
            }
            if (StringUtils.isBlank(deployDetails.artifactPath)) {
                throw new IllegalArgumentException("Artifact path cannot be empty");
            }
            return deployDetails;
        }

        public Builder bean(BuildFileBean bean) {
            Properties beanProperties = bean.getProperties();
            if (beanProperties != null) {
                ArrayListMultimap<String, String> multimap = ArrayListMultimap.create();
                for (Map.Entry<String, String> entry : Maps.fromProperties(beanProperties).entrySet()) {
                    multimap.put(entry.getKey(), entry.getValue());
                }
                deployDetails.properties = multimap;
            }
            deployDetails.sha1 = bean.getSha1();
            deployDetails.md5 = bean.getMd5();
            return this;
        }

        public Builder file(File file) {
            deployDetails.file = file;
            return this;
        }

        public Builder targetRepository(String targetRepository) {
            deployDetails.targetRepository = targetRepository;
            return this;
        }

        public Builder artifactPath(String artifactPath) {
            deployDetails.artifactPath = artifactPath;
            return this;
        }

        public Builder sha1(String sha1) {
            deployDetails.sha1 = sha1;
            return this;
        }

        public Builder md5(String md5) {
            deployDetails.md5 = md5;
            return this;
        }

        public Builder explode(boolean isExplodeArchive) {
            deployDetails.explode = isExplodeArchive;
            return this;
        }

        public Builder packageType(PackageType packageType) {
            deployDetails.packageType = packageType;
            return this;
        }

        public Builder addProperty(String key, String value) {
            if (deployDetails.properties == null) {
                deployDetails.properties = ArrayListMultimap.create();
            }
            deployDetails.properties.put(key, value);
            return this;
        }

        public Builder addProperties(Map<String, String> propertiesToAdd) {
            if (deployDetails.properties == null) {
                deployDetails.properties = ArrayListMultimap.create();
            }

            deployDetails.properties.putAll(Multimaps.forMap(propertiesToAdd));
            return this;
        }

        public Builder addProperties(Multimap<String, String> propertiesToAdd) {
            if (deployDetails.properties == null) {
                deployDetails.properties = ArrayListMultimap.create();
            }

            deployDetails.properties.putAll(propertiesToAdd);
            return this;
        }
    }
}
