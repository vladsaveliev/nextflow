package nextflow.script

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WorkflowScopeTest extends Specification {

    def 'should verify push and pop semantics' () {

        given:
        def wf1 = Mock(WorkflowDef)
        def wf2 = Mock(WorkflowDef)
        def stack = new WorkflowScope()

        expect:
        stack.current() == null
        stack.size()==0
        !stack

        when:
        stack.push(wf1)
        then:
        stack
        stack.size()==1
        stack.current() == wf1

        when:
        stack.push(wf2)
        then:
        stack
        stack.size()==2
        stack.current() == wf2

        when:
        def result = stack.pop()
        then:
        result == wf2
        stack.current() == wf1
        stack.last() == wf2
        stack.size()==1
        stack

        when:
        result = stack.pop()
        then:
        result == wf1
        stack.current() == null
        stack.last() == wf1
        stack.size()==0
        !stack

    }

}
