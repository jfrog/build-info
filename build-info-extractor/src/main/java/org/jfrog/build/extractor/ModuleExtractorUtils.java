package org.jfrog.build.extractor;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jfrog.build.extractor.ci.Module;

import java.io.File;
import java.io.IOException;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

/**
 * Utilities for serializing/deserializing Module info as json
 */
public class ModuleExtractorUtils {

    /**
     * Given a Module object, serialize it to a json string and write it to the provided file.
     *
     * @param module The module object
     * @param toFile The file to write the serialized module to
     * @throws IOException in case of any serialization error.
     */
    public static void saveModuleToFile(Module module, File toFile) throws IOException {
        if (!toFile.getParentFile().exists()) {
            toFile.getParentFile().mkdirs();
        }
        if (!toFile.exists()) {
            toFile.createNewFile();
        }
        createMapper().writeValue(toFile, module);
    }

    /**
     * Given a file, read its contents as a json string and deserialize it as a Module object.
     *
     * @param fromFile The file containing a serialized json string
     * @return The Module object deserialized from the content of the file
     * @throws IOException in case of any deserialization error.
     */
    public static Module readModuleFromFile(File fromFile) throws IOException {
        return createMapper().readValue(fromFile, new TypeReference<Module>() {
        });
    }
}
