package nextflow.script

import groovy.transform.CompileStatic
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class WorkflowScope {

    static WorkflowScope INSTANCE = new WorkflowScope()

    static WorkflowScope get() { INSTANCE }

    private List<WorkflowDef> stack = new ArrayList<>()

    private WorkflowDef last

    WorkflowDef last() { last }

    WorkflowDef current() {
        stack ? stack.get(0) : null
    }

    void push(WorkflowDef workflow) {
        stack.push(workflow)
    }

    WorkflowDef pop() {
        last = stack.pop()
    }

    int size() {
        stack.size()
    }

    boolean asBoolean() {
        stack.size()>0
    }

}
