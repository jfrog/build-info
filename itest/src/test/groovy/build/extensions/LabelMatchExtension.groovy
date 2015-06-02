package build.extensions

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * @author Lior Hasson  
 */
class LabelMatchExtension extends AbstractAnnotationDrivenExtension<LabelMatch>{

    String[] labels
    String fieldName

    @Override
    void visitFieldAnnotation(LabelMatch annotation, FieldInfo field) {
        //TODO validate field type of list
        labels = annotation.value()
        fieldName = field.name
    }

    @Override
    void visitSpec(SpecInfo spec) {
        //Construct and subscribe our event interceptor
        def interceptor = new LabelMatchInterceptor(labels: labels, fieldName: fieldName)
        spec.addInterceptor(interceptor)
    }
}
