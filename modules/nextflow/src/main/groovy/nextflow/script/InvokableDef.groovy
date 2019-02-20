package nextflow.script

import org.codehaus.groovy.runtime.InvokerHelper

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
trait InvokableDef {

    abstract String getName()

    abstract Object invoke(Object[] args, Binding binding)

    Object invoke(Object args, Binding binding=null) {
        invoke(InvokerHelper.asArray(args),binding)
    }
}
