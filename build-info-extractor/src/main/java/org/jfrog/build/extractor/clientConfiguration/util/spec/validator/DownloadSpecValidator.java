package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public class DownloadSpecValidator extends SpecsValidator {

    @Override
    public void validate(Spec spec) throws IOException {
        if (spec.getFiles() == null || spec.getFiles().length == 0) {
            throw new IllegalArgumentException("Spec must contain at least one fileSpec.");
        }
        for (FileSpec fileSpec : spec.getFiles()) {
            validateQueryInputs(fileSpec);
        }
    }
}
