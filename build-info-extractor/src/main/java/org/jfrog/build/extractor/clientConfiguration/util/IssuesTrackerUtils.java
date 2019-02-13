package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Issue;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for issues tracker operations
 *
 * @author Shay Yaakov
 */
public class IssuesTrackerUtils {

    public static Set<Issue> getAffectedIssuesSet(String affectedIssues) {
        Set<Issue> affectedIssuesSet = new HashSet<>();
        if (StringUtils.isNotBlank(affectedIssues)) {
            String[] issuePairs = affectedIssues.split(",");
            for (String pair : issuePairs) {
                String[] idAndUrl = pair.split(">>");
                if (idAndUrl.length == 3) {
                    affectedIssuesSet.add(new Issue(idAndUrl[0], idAndUrl[1], idAndUrl[2]));
                }
            }
        }
        return affectedIssuesSet;
    }
}
