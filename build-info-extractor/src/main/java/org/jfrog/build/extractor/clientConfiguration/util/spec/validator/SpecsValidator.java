package org.jfrog.build.extractor.clientConfiguration.util.spec.validator;

import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;

import java.io.IOException;

/**
 * Created by tamirh on 19/06/2017.
 */
public interface SpecsValidator {
    void validate(Spec spec) throws IOException;
}
