package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public class UploadSpecValidator extends SpecsValidator {

    @Override
    public void validate(Spec spec) throws IOException {
        if (ArrayUtils.isEmpty(spec.getFiles())) {
            throw new IllegalArgumentException("Spec must contain at least one fileSpec.");
        }
        for (FileSpec fileSpec : spec.getFiles()) {
            if (StringUtils.isBlank(fileSpec.getTarget())) {
                throw new IllegalArgumentException("The argument 'target' is missing from the upload spec.");
            }
            if (StringUtils.isBlank(fileSpec.getPattern())) {
                throw new IllegalArgumentException("The argument 'pattern' is missing from the upload spec.");
            }
            validateQueryInputs(fileSpec);
        }
    }
}