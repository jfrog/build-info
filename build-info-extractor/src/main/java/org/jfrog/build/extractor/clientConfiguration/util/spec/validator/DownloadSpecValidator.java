package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public class DownloadSpecValidator implements SpecsValidator {

    @Override
    public void validate(Spec spec) throws IOException {
        if (spec.getFiles() == null || spec.getFiles().length == 0) {
            throw new IllegalArgumentException("Spec must contain at least one fileSpec.");
        }
        for (FileSpec fileSpec : spec.getFiles()) {
            if (StringUtils.isBlank(fileSpec.getAql()) && StringUtils.isBlank(fileSpec.getPattern())){
                throw new IllegalArgumentException("Spec must contain AQL or Pattern key");
            }
            if (StringUtils.isNotBlank(fileSpec.getAql()) && StringUtils.isNotBlank(fileSpec.getPattern())){
                throw new IllegalArgumentException("Spec can't contain both AQL and Pattern keys");
            }
        }
    }
}
