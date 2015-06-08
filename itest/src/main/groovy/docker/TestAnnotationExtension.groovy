package docker

import org.junit.runner.Description
import org.junit.runners.model.Statement

import java.lang.annotation.Annotation

/**
 * @author Lior Hasson
 */
interface TestAnnotationExtension {
    void init(Annotation com)

    Statement apply(final Statement base, Description description)

    void after()
}
