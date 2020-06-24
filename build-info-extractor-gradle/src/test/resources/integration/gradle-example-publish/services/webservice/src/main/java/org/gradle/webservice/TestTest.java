package org.gradle.webservice;

import org.apache.commons.collections.list.GrowthList;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.gradle.api.PersonList;
import org.gradle.shared.Person;

public class TestTest {
    private String name;

    public void method() {
        FilenameUtils.separatorsToUnix("my/unix/filename");
        ToStringBuilder.reflectionToString(new Person("name"));
        new GrowthList();
        new PersonList().doSomethingWithImpl(); // compile with api-spi, runtime with api
    }

}
