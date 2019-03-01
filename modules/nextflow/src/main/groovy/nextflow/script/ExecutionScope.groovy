package nextflow.script

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ExecutionScope {

    static private List<InvocationScope> stack = new ArrayList<>()

    static InvocationScope current() {
        stack ? stack.get(0) : null
    }

    static boolean isScript() {
        current() instanceof BaseScript
    }

    static boolean isWorkflow() {
        current() instanceof WorkflowDef
    }

    static Binding context() {
        stack ? stack.get(0).getBinding() : null
    }

    static BaseScript script() {
        for( int i=0; i<stack.size(); i++ ) {
            if( stack[i] instanceof BaseScript )
                return (BaseScript)stack[i]
        }
        return null
    }

    static WorkflowDef workflow() {
        stack[0] instanceof WorkflowDef ? (WorkflowDef)stack[0] : null
    }

    static void push(InvocationScope script) {
        stack.push(script)
    }

    static InvocationScope pop() {
        stack.pop()
    }

    static int size() {
        stack.size()
    }

    @PackageScope
    static void reset() {
        stack = new ArrayList<>()
    }

}
