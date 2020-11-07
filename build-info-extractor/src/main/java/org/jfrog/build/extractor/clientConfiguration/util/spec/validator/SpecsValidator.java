package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public abstract class SpecsValidator {
    public abstract void validate(Spec spec, Log log) throws IOException;

    static void validateQueryInputs(FileSpec fileSpec) throws IOException {
        boolean isAql = StringUtils.isNotBlank(fileSpec.getAql());
        boolean isPattern = StringUtils.isNotBlank(fileSpec.getPattern());
        boolean isExcludePattern = !ArrayUtils.isEmpty(fileSpec.getExcludePatterns()) && StringUtils.isNotBlank(fileSpec.getExcludePattern(0));
        boolean isExclusion = !ArrayUtils.isEmpty(fileSpec.getExclusions()) && StringUtils.isNotBlank(fileSpec.getExclusion(0));

        if (isAql && isPattern) {
            throw new IllegalArgumentException("Spec can't contain both AQL and Pattern keys");
        }
        if (isAql && (isExcludePattern || isExclusion)) {
            throw new IllegalArgumentException("Spec can't contain both AQL and ExcludePatterns or Exclusions keys");
        }
        if (isExcludePattern && isExclusion) {
            throw new IllegalArgumentException("Spec can't contain both Exclusions and ExcludePatterns keys");
        }
    }
}