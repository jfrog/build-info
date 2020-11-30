package org.jfrog.build.extractor.pip.extractor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

import static org.jfrog.build.extractor.pip.extractor.PipBuildInfoExtractor.createAqlQueries;
import static org.testng.Assert.assertEquals;

@Test
public class PipBuildInfoExtractorTest {

    @DataProvider
    private Object[][] createAqlQueriesProvider() {
        return new Object[][]{
                {fileToPackageTestMap, 2, Arrays.asList(
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file2.whl\"}}]},{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file4.egg\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")",
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file3.tar.gz\"}}]},{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file1.tgz\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")")},
                {fileToPackageTestMap, 1, Arrays.asList(
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file2.whl\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")",
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file4.egg\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")",
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file3.tar.gz\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")",
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file1.tgz\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")")},
                {fileToPackageTestMap, 4, Collections.singletonList(
                        "items.find({\"repo\": \"repository\",\"$or\": [{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file2.whl\"}}]},{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file4.egg\"}}]},{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file3.tar.gz\"}}]},{\"$and\":[{\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"file1.tgz\"}}]}]}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")")}
        };
    }


    @Test(dataProvider = "createAqlQueriesProvider")
    public void createAqlQueriesTest(Map<String, String> fileToPackage, int bulkSize, List<String> expected) {
        List<String> actual = createAqlQueries(fileToPackage, "repository", bulkSize);
        assertEquals(actual, expected);
    }

    private final Map<String, String> fileToPackageTestMap = new HashMap<String, String>() {{
        put("file1.tgz", "file1");
        put("file2.whl", "file2");
        put("file3.tar.gz", "file3");
        put("file4.egg", "file4");
    }};
}
