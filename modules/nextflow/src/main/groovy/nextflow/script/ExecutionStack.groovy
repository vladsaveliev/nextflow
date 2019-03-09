package nextflow.script


import groovy.transform.CompileStatic
import groovy.transform.PackageScope
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ExecutionStack {

    static private List<ExecutionContext> stack = new ArrayList<>()

    static ExecutionContext current() {
        stack.get(0)
    }

    static boolean withinWorkflow() {
        for( def entry : stack ) {
            if( entry instanceof WorkflowDef )
                return true
        }
        return false
    }

    static WorkflowBinding context() {
        current().getBinding()
    }

    static BaseScript script() {
        for( def item in stack ) {
            if( item instanceof BaseScript )
                return item
        }
        throw new IllegalStateException("Missing execution script")
    }

    static BaseScript owner() {
        def c = current()
        if( c instanceof BaseScript )
            return c
        if( c instanceof WorkflowDef )
            return c.getOwner()
        throw new IllegalStateException("Not a valid scope object: [${c.getClass().getName()}] $this")
    }

    static void push(ExecutionContext script) {
        stack.push(script)
    }

    static ExecutionContext pop() {
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
