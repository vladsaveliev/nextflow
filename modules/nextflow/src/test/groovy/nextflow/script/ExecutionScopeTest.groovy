package nextflow.script

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ExecutionScopeTest extends Specification {

    def setupSpec() {
        ExecutionScope.reset()
    }

    def 'should verify push and pop semantics' () {

        given:
        def b1 = new Binding(status: 1)
        def b2 = new Binding(status: 2)
        def b3 = new Binding(status: 3)

        def s1 = Mock(BaseScript); s1.getBinding() >> b1
        def s2 = Mock(WorkflowDef); s2.getBinding() >> b2
        def s3 = Mock(WorkflowDef); s3.getBinding() >> b3

        expect:
        ExecutionScope.current() == null
        ExecutionScope.size()==0
        ExecutionScope.context() == null

        when:
        ExecutionScope.push(s1)
        then:
        ExecutionScope.size()==1
        ExecutionScope.current() == s1
        ExecutionScope.script() == s1
        ExecutionScope.workflow() == null
        ExecutionScope.context() == b1
        ExecutionScope.isScript()
        !ExecutionScope.isWorkflow()

        when:
        ExecutionScope.push(s2)
        then:
        ExecutionScope.size()==2
        ExecutionScope.current() == s2
        ExecutionScope.script() == s1
        ExecutionScope.workflow() == s2
        ExecutionScope.context() == b2
        !ExecutionScope.isScript()
        ExecutionScope.isWorkflow()

        when:
        ExecutionScope.push(s3)
        then:
        ExecutionScope.current() == s3
        ExecutionScope.script() == s1
        ExecutionScope.workflow() == s3
        ExecutionScope.context() == b3
        !ExecutionScope.isScript()
        ExecutionScope.isWorkflow()

        when:
        def result = ExecutionScope.pop()
        then:
        result == s3
        ExecutionScope.workflow() == s2
        ExecutionScope.current() == s2
        ExecutionScope.size()==2
        ExecutionScope.script() == s1
        ExecutionScope.context() == b2
        !ExecutionScope.isScript()
        ExecutionScope.isWorkflow()

        when:
        result = ExecutionScope.pop()
        then:
        result == s2
        ExecutionScope.current() == s1
        ExecutionScope.script() == s1
        ExecutionScope.workflow() == null
        ExecutionScope.size()==1
        ExecutionScope.context() == b1
        ExecutionScope.isScript()
        !ExecutionScope.isWorkflow()

        when:
        result = ExecutionScope.pop()
        then:
        result == s1
        ExecutionScope.script() == null
        ExecutionScope.workflow() == null
        ExecutionScope.context() == null
        ExecutionScope.current() == null
        ExecutionScope.size()==0
        !ExecutionScope.isScript()
        !ExecutionScope.isWorkflow()

    }

}
