package org.jfrog.build.extractor.maven.reader;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Read a Maven project recursively.
 *
 * @author Tomer Cohen
 */
public class ProjectReader {
    private final File rootPom;

    /**
     * Constructor, gets the file which represents the location of the <b>root</b> pom of the Maven project.
     *
     * @param rootPom The root pom of the Maven project.
     */
    public ProjectReader(File rootPom) {
        this.rootPom = rootPom;
    }

    /**
     * Read the project starting with the root pom. This method returns a {@link java.util.HashMap} that has {@link
     * ModuleName} as its key, and the location of its corresponding pom file which was used to construct the {@link
     * ModuleName}
     *
     * @return A map of {@link ModuleName} and the file that was used to build it.
     * @throws IOException Thrown in case of an error occurring while reading the pom.
     */
    public Map<ModuleName, File> read() throws IOException {
        Map<ModuleName, File> result = Maps.newHashMap();
        readRecursive(result, rootPom);
        return result;
    }

    /**
     * Read the modules recursively and populate the {@link ModuleName} and file map according to the modules.
     */
    private void readRecursive(Map<ModuleName, File> modules, File current) throws IOException {
        if (!current.exists()) {
            throw new IllegalArgumentException("Root pom file: " + current.getAbsolutePath() + " does not exist");
        }
        Model model = readModel(current);
        String groupId = model.getGroupId();
        if (StringUtils.isBlank(groupId)) {
            groupId = model.getParent().getGroupId();
        }
        modules.put(new ModuleName(groupId, model.getArtifactId()), current);

        List<String> children = model.getModules();
        for (String child : children) {
            if (!child.endsWith("pom.xml")) {
                child += "/pom.xml";
            }
            File childPom = new File(current.getParentFile().getAbsolutePath(), child);
            readRecursive(modules, childPom);
        }
    }

    /**
     * @return Construct a Maven {@link Model} from the pom.
     */
    private Model readModel(File pom) throws IOException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try (FileInputStream inputStream = new FileInputStream(pom)) {
            return reader.read(inputStream);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }
}
