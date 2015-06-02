package build

import docker.TestAnnotationExtension
import docker.RuleAnnotation
import org.junit.ClassRule
import org.junit.internal.AssumptionViolatedException
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.annotation.Annotation

/**
 * Extend this abstract class if you want to run your tests together with custom annotations that
 * uses {@link RuleAnnotation} to be executed, for example look at {@link docker.RunWithDocker}
 *
 * @author Lior Hasson  
 */
abstract class AbstractJUnitTest {
    @ClassRule
    public static ExternalResource runAnnotations() {
        return new ExternalResource() {
            @Override
            Statement apply(final Statement base, final Description description) {
                final Logger logger = LoggerFactory.getLogger(AbstractJUnitTest.class);
                return new Statement() {
                    @Override
                    void evaluate() throws Throwable {
                        List<TestAnnotationExtension> extensions = new ArrayList<TestAnnotationExtension>()
                        try {
                            decorateWithRules(description, base, extensions).evaluate()
                        } catch (AssumptionViolatedException e) {
                            logger.error("Skipping %s: %s%n", description.getDisplayName(), e.getMessage());
                            throw e;
                        } catch (Exception | AssertionError e) { // Errors and failures
                            logger.error("Error in %s: %s%n", description.getDisplayName(), e.getMessage());
                            throw e;
                        }
                        finally {
                            //Run all the after methods, can be use for cleanup etc.
                            for (TestAnnotationExtension rule : extensions) {
                                    rule.after();
                            }
                        }
                    }

                    /**
                     * Look for annotations on a test and honor {@link RuleAnnotation}s in them.
                     */
                    public Statement decorateWithRules(Description d, Statement body, List<TestAnnotationExtension> extensions) {
                        for (Annotation a : d.getAnnotations()) {
                            RuleAnnotation r = a.annotationType().getAnnotation(RuleAnnotation.class)
                            if (r != null) {
                                TestAnnotationExtension rule = r.value().newInstance()
                                extensions.add(rule)
                                rule.init(a);
                                body = rule.apply(body, d)
                            }
                        }

                        body
                    }
                }
            }
        }
    }
}
