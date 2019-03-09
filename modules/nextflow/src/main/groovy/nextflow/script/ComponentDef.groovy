package nextflow.script

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.InvokerHelper

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
abstract class ComponentDef implements Cloneable {

    abstract String getType()

    abstract String getName()

    abstract ComponentDef withName(String name)

    abstract Object invoke_a(Object[] args)

    final Object invoke_o(Object args) {
        invoke_a(InvokerHelper.asArray(args))
    }

    String toString() {
        "${this.getClass().getSimpleName()}[$type $name]"
    }

}
