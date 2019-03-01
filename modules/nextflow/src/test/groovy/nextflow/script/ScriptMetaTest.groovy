package nextflow.script

import spock.lang.Specification

import java.nio.file.Paths

import groovy.transform.InheritConstructors
import nextflow.exception.DuplicateScriptDefinitionException
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ScriptMetaTest extends Specification {

    @InheritConstructors
    static class FooScript extends BaseScript {
        @Override
        protected Object runScript() { null }
    }

    def 'should return all defined names' () {

        given:
        def script = new FooScript(new ScriptBinding())

        def proc1 = new ProcessDef(script, 'proc1', Mock(ProcessConfig), Mock(TaskBody))
        def proc2 = new ProcessDef(script, 'proc2', Mock(ProcessConfig), Mock(TaskBody))
        def func1 = new FunctionDef(name: 'func1')
        def work1 = new WorkflowDef(Mock(TaskBody), 'work1')

        def meta = new ScriptMeta(script)

        when:
        meta.addDefinition(func1)
        meta.addDefinition(work1)
        meta.addDefinition(proc1)
        meta.addDefinition(proc2)

        then:
        meta.getInvokable('work1') == work1
        meta.getInvokable('func1') == func1
        meta.getInvokable('proc1') == proc1
        meta.getInvokable('proc2') == proc2
        meta.getInvokable('xxx') == null
        meta.getInvokable('yyy') == null

        then:
        meta.getProcessNames() as Set == ['proc1','proc2'] as Set

    }

    def 'should add imports' () {

        given:
        def script1 = new FooScript(new ScriptBinding())
        def script2 = new FooScript(new ScriptBinding())
        def script3 = new FooScript(new ScriptBinding())
        def meta1 = new ScriptMeta(script1)
        def meta2 = new ScriptMeta(script2)
        def meta3 = new ScriptMeta(script3)

        // defs in the root script
        def proc1 = new ProcessDef(script1, 'proc1', Mock(ProcessConfig), Mock(TaskBody))
        def func1 = new FunctionDef(name: 'func1')
        def work1 = new WorkflowDef(Mock(TaskBody), 'work1')
        meta1.addDefinition(proc1, func1, work1)

        // defs in the second script imported in the root namespace
        def proc2 = new ProcessDef(script2, 'proc2', Mock(ProcessConfig), Mock(TaskBody))
        def func2 = new FunctionDef(name: 'func2')
        def work2 = new WorkflowDef(Mock(TaskBody), 'work2')
        meta2.addDefinition(proc2, func2, work2)

        // defs in the third script imported in a separate namespace
        def proc3 = new ProcessDef(script2, 'proc3', Mock(ProcessConfig), Mock(TaskBody))
        def func3 = new FunctionDef(name: 'func3')
        def work3 = new WorkflowDef(Mock(TaskBody), 'work2')
        meta3.addDefinition(proc3, func3, work3)

        when:
        meta1.addModule('_', meta2)
        meta1.addModule('modx', meta3)

        then:
        meta1.getDefinitions() as Set == [proc1, func1, work1] as Set
        meta1.getInvokable('proc1') == proc1
        meta1.getInvokable('func1') == func1
        meta1.getInvokable('work1') == work1

        then:
        // from root namespace
        meta1.getInvokable('proc2') == proc2
        meta1.getInvokable('func2') == func2
        meta1.getInvokable('work2') == work2

        then:
        // other namespace are not reachable
        meta1.getInvokable('proc3') == null
        meta1.getInvokable('work3') == null
        meta1.getInvokable('func3') == null
        meta1.getInvokable('xxx') == null

        then:
        meta1.getProcessNames() == ['proc1','proc2','modx.proc3'] as Set
    }


    def 'should check collision' () {

        given:
        def proc1 = new ProcessDef(Mock(BaseScript), 'proc1', Mock(ProcessConfig), Mock(TaskBody))
        def func1 = new FunctionDef(name: 'func1')
        def work1 = new WorkflowDef(Mock(TaskBody), 'work1')

        def script1 = new FooScript(new ScriptBinding())
        def meta1 = new ScriptMeta(script1)
        meta1.addDefinition(proc1, func1, work1)

        // add a not colliding process
        when:
        def proc = new ProcessDef(Mock(BaseScript), 'FOO', Mock(ProcessConfig), Mock(TaskBody))
        def reqScript = new FooScript(new ScriptBinding())
        def reqMeta = new ScriptMeta(reqScript)
        reqMeta.addDefinition(proc)
        meta1.checkNamespaceCollision('_', reqMeta)

        then:
        noExceptionThrown()

        // add a process with a name already used
        when:
        proc = new ProcessDef(Mock(BaseScript), 'proc1', Mock(ProcessConfig), Mock(TaskBody))
        reqScript = new FooScript(new ScriptBinding())
        reqMeta = new ScriptMeta(reqScript)
        reqMeta.addDefinition(proc)
        reqMeta.scriptPath = Paths.get('/foo/script.nf')
        meta1.checkNamespaceCollision('_', reqMeta)

        then:
        def err = thrown(DuplicateScriptDefinitionException)
        err.message == "Required module contains a process with name 'proc1' already defined in the root namespace -- check script: /foo/script.nf"


        // add a module using a new name space
        when:
        meta1.checkNamespaceCollision('ns1', reqMeta)
        then:
        noExceptionThrown()

        // add a module using the same namespace
        when:
        meta1.imports.put('ns2', Mock(ScriptMeta))
        meta1.checkNamespaceCollision('ns2', reqMeta)
        then:
        err = thrown(DuplicateScriptDefinitionException)
        err.message == "A module with name 'ns2' was already imported"

    }
}
