package org.jfrog.build.maven

import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

/**
 * @author Lior Hasson  
 */
class MavenBuildInfoSpec extends Specification{

    @Rule
    TestName testName = new TestName()

    def setupSpec() {

    }

    def "run maven test"() {
        when:
        def exitCode = MavenSpecUtils.launchMaven()

        then:
        // Gradle build finished successfully:
        exitCode == 0
    }
}
