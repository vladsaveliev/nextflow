package nextflow.exception

import nextflow.script.InvokableDef
import nextflow.script.ProcessDef
import nextflow.script.WorkflowDef

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class IllegalInvocationException extends ProcessException {

    IllegalInvocationException(InvokableDef invokable) {
        super(message(invokable))
    }

    static private String message(InvokableDef invokable) {
        if( invokable instanceof WorkflowDef )
            return "Workflow $invokable.name cannot be invoked from a module script"

        if( invokable instanceof ProcessDef )
            return "Process $invokable.name can only be invoked from a workflow context"

        return "Invalid invocation context: $invokable.name"
    }
}
