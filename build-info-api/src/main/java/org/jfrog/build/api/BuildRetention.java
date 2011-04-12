package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Tomer Cohen
 */
@XStreamAlias("buildretention")
public class BuildRetention implements Serializable{

    private int count = -1;

    private Date minimumBuildDate;

    private final boolean deleteBuildArtifacts;

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

    public boolean isDeleteBuildArtifacts() {
        return deleteBuildArtifacts;
    }
}
