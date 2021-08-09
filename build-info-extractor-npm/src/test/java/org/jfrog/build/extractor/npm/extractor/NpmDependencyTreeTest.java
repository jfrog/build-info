package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

/**
 * @author yahavi
 **/
@Test
public class NpmDependencyTreeTest {
    private final ObjectMapper mapper = createMapper();

    @DataProvider
    private Object[][] getProjectNameProvider() {
        return new Object[][]{
                {new GetProjectNameProvider("{}", Paths.get("a", "b", "c"), "c")},
                {new GetProjectNameProvider("{\"version\":\"1.2.3\"}", Paths.get("a", "b", "c"), "c")},
                {new GetProjectNameProvider("{\"name\":\"loki\"}", Paths.get("a", "b", "c"), "loki")},
                {new GetProjectNameProvider("{\"name\":\"loki\",\"version\":\"1.2.3\"}", Paths.get("a", "b", "c"), "loki:1.2.3")}
        };
    }

    @Test(dataProvider = "getProjectNameProvider")
    public void getProjectNameEmptyTest(GetProjectNameProvider projectNameProvider) throws JsonProcessingException {
        JsonNode npmList = mapper.readTree(projectNameProvider.npmLsResults);
        Assert.assertEquals(NpmDependencyTree.getProjectName(npmList, projectNameProvider.workingDir), projectNameProvider.expectedProjectName);
    }

    private static class GetProjectNameProvider {
        private final String npmLsResults;
        private final Path workingDir;
        private final String expectedProjectName;

        private GetProjectNameProvider(String npmLsResults, Path workingDir, String expectedProjectName) {
            this.npmLsResults = npmLsResults;
            this.workingDir = workingDir;
            this.expectedProjectName = expectedProjectName;
        }
    }
}
