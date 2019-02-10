package nextflow.script

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class CurrentScriptTest extends Specification {

    def 'should add and remove current script' () {

        given:
        def SCRIPT1 = new CurrentScript(Mock(BaseScript), true, Mock(ScriptBinding), Mock(ScriptLibrary))
        def SCRIPT2 = new CurrentScript(Mock(BaseScript), false, Mock(ScriptBinding), Mock(ScriptLibrary))

        when:
        CurrentScript.get()
        then:
        thrown(EmptyStackException)

        when:
        CurrentScript.push(SCRIPT1)
        def current = CurrentScript.get()
        then:
        current == SCRIPT1

        when:
        CurrentScript.push(SCRIPT2)
        current = CurrentScript.get()
        then:
        current == SCRIPT2

        when:
        current = CurrentScript.pop()
        then:
        current == SCRIPT2

        when:
        current = CurrentScript.pop()
        then:
        current == SCRIPT1

        when:
        CurrentScript.pop()
        then:
        thrown(EmptyStackException)
    }

}
