package org.jfrog.build.api;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Tomer Cohen
 */
@XStreamAlias("buildretention")
public class BuildRetention implements Serializable {

    private int count = -1;

    private Date minimumBuildDate;

    private boolean deleteBuildArtifacts;

    private List<String> buildNumbersNotToBeDiscarded = Lists.newArrayList();

    // for json instantiation
    public BuildRetention() {
    }

    public BuildRetention(boolean deleteBuildArtifacts) {
        this.deleteBuildArtifacts = deleteBuildArtifacts;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getMinimumBuildDate() {
        return minimumBuildDate;
    }

    public void setMinimumBuildDate(Date minimumBuildDate) {
        this.minimumBuildDate = minimumBuildDate;
    }

    public void setDeleteBuildArtifacts(boolean deleteBuildArtifacts) {
        this.deleteBuildArtifacts = deleteBuildArtifacts;
    }

    public boolean isDeleteBuildArtifacts() {
        return deleteBuildArtifacts;
    }

    public void addBuildNotToBeDiscarded(String buildNumber) {
        buildNumbersNotToBeDiscarded.add(buildNumber);
    }

    public List<String> getBuildNumbersNotToBeDiscarded() {
        return buildNumbersNotToBeDiscarded;
    }
}
