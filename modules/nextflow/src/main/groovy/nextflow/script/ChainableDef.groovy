package nextflow.script

import groovy.transform.CompileStatic

/**
 * An executable component i.e. a process or a workflow
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
interface ChainableDef  {

    abstract String getType()

    abstract String getName()

    abstract Object invoke_o(Object args)

    abstract Object invoke_a(Object[] args)

}
