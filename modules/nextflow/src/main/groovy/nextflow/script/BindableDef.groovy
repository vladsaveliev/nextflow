package nextflow.script

import groovy.transform.CompileStatic

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
abstract class BindableDef extends ComponentDef {

    abstract Object run(Object[] args)

    Object invoke_a(Object[] args) {
        // use this instance an workflow template, therefore clone it
        final comp = (BindableDef)this.clone()
        // invoke the process execution
        final result = comp.run(args)
        // register this component invocation in the current context
        // so that it can be accessed in the outer execution scope
        if( name ) {
            final scope = ExecutionStack.context()
            scope.setVariable(name, comp)
        }
        return result
    }

}
