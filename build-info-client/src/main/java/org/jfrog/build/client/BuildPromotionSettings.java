package org.jfrog.build.client;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class BuildPromotionSettings {

    private String buildName;
    private String buildNumber;
    private String buildStarted;
    private String targetRepo;
    private boolean includeArtifacts = true;
    private boolean includeDependencies;
    private Set<String> scopes;
    private Multimap<String, String> properties;
    private boolean dryRun;
    private String promotionStatus;
    private String promotionComment;

    public BuildPromotionSettings(String buildName, String buildNumber, String buildStarted, String targetRepo,
            boolean includeArtifacts, boolean includeDependencies, Set<String> scopes,
            Multimap<String, String> properties, boolean dryRun, String promotionStatus, String promotionComment) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.buildStarted = buildStarted;
        this.targetRepo = targetRepo;
        this.includeArtifacts = includeArtifacts;
        this.includeDependencies = includeDependencies;
        this.scopes = scopes;
        this.properties = properties;
        this.dryRun = dryRun;
        this.promotionStatus = promotionStatus;
        this.promotionComment = promotionComment;
    }

    public String getBuildName() {
        return buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public String getBuildStarted() {
        return buildStarted;
    }

    public String getTargetRepo() {
        return targetRepo;
    }

    public boolean isIncludeArtifacts() {
        return includeArtifacts;
    }

    public boolean isIncludeDependencies() {
        return includeDependencies;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Multimap<String, String> getProperties() {
        return properties;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public String getPromotionStatus() {
        return promotionStatus;
    }

    public String getPromotionComment() {
        return promotionComment;
    }

    public String buildUrl(StringBuilder urlBuilder) throws UnsupportedEncodingException {
        urlBuilder.append("/").append(buildName).append("/").append(buildNumber).append("?").append("to=").
                append(encodeForUrl(targetRepo)).append("&").append("arts=").append(booleanToInt(includeArtifacts)).
                append("&deps=").append(booleanToInt(includeDependencies)).append("dry=").append(booleanToInt(dryRun));

        if (StringUtils.isNotBlank(buildStarted)) {
            urlBuilder.append("&started=").append(encodeForUrl(buildStarted));
        }

        appendPromotionScopes(urlBuilder);
        appendPromotionProperties(urlBuilder);

        if (StringUtils.isNotBlank(promotionStatus)) {
            urlBuilder.append("&status=").append(promotionStatus);
        }

        if (StringUtils.isNotBlank(promotionComment)) {
            urlBuilder.append("&comment=").append(promotionComment);
        }

        return urlBuilder.toString();
    }

    private void appendPromotionScopes(StringBuilder urlBuilder) throws UnsupportedEncodingException {
        if ((scopes != null) && !scopes.isEmpty()) {
            urlBuilder.append("&scopes=");
            Iterator<String> iterator = scopes.iterator();
            while (iterator.hasNext()) {
                urlBuilder.append(encodeForUrl(iterator.next()));
                if (iterator.hasNext()) {
                    urlBuilder.append(",");
                }
            }
        }
    }

    private void appendPromotionProperties(StringBuilder urlBuilder) throws UnsupportedEncodingException {
        if ((properties != null) && !properties.isEmpty()) {

            urlBuilder.append("&properties=");

            Iterator<String> keyIterator = properties.keySet().iterator();

            while (keyIterator.hasNext()) {

                String key = keyIterator.next();
                urlBuilder.append(encodeForUrl(key)).append("=");
                Iterator<String> valueIterator = properties.get(key).iterator();

                while (valueIterator.hasNext()) {
                    urlBuilder.append(encodeForUrl(valueIterator.next()));

                    if (valueIterator.hasNext()) {
                        urlBuilder.append(",");
                    }
                }

                if (keyIterator.hasNext()) {
                    urlBuilder.append("|");
                }

            }
        }
    }

    private int booleanToInt(boolean toConvert) {
        return toConvert ? 1 : 0;
    }

    private String encodeForUrl(String toEncode) throws UnsupportedEncodingException {
        return URLEncoder.encode(toEncode, "utf-8");
    }
}
