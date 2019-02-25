package nextflow.script

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ScriptScopeTest extends Specification {

    def 'should verify push and pop semantics' () {

        given:
        def s1 = Mock(BaseScript)
        def s2 = Mock(BaseScript)
        def stack = new ScriptScope()

        expect:
        stack.current() == null
        stack.size()==0
        !stack

        when:
        stack.push(s1)
        then:
        stack
        stack.size()==1
        stack.current() == s1

        when:
        stack.push(s2)
        then:
        stack
        stack.size()==2
        stack.current() == s2

        when:
        def result = stack.pop()
        then:
        result == s2
        stack.current() == s1
        stack.size()==1
        stack

        when:
        result = stack.pop()
        then:
        result == s1
        stack.current() == null
        stack.size()==0
        !stack

    }

}
