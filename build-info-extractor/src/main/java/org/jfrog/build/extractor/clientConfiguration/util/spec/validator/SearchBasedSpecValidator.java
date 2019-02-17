package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public class SearchBasedSpecValidator extends SpecsValidator {

    @Override
    public void validate(Spec spec) throws IOException {
        if (spec.getFiles() == null || spec.getFiles().length == 0) {
            throw new IllegalArgumentException("Spec must contain at least one fileSpec.");
        }
        for (FileSpec fileSpec : spec.getFiles()) {
            boolean isAql = StringUtils.isNotBlank(fileSpec.getAql());
            boolean isPattern = StringUtils.isNotBlank(fileSpec.getPattern());
            boolean isBuild = StringUtils.isNotBlank(fileSpec.getBuild());

            if (!isAql && !isPattern && !isBuild) {
                throw new IllegalArgumentException("A search based Spec must contain AQL or Pattern, and/or Build keys.");
            }
            validateQueryInputs(fileSpec);
        }
    }
}
