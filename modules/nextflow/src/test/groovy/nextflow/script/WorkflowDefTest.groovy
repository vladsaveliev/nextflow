package nextflow.script

import spock.lang.Specification

import nextflow.ast.NextflowDSL
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class WorkflowDefTest extends Specification {

    static abstract class TestScript extends BaseScript {

        Object run() {
            runScript()
            return this
        }

    }

    def 'should parse workflow' () {

        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(TestScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
                    
            workflow alpha {
              print 'Hello world'
            }
        
            workflow bravo(foo, bar) {
              print foo
              print bar
              return foo+bar
            }
            
            workflow delta(foo) {
                println foo+bar
            }

            workflow empty { }
        '''

        when:
        def script = (TestScript)new GroovyShell(config).parse(SCRIPT).run()
        def meta = ScriptMeta.get(script)
        then:
        meta.definedWorkflows.size() == 4
        meta.getWorkflowDef('alpha') .declaredInputs == []
        meta.getWorkflowDef('alpha') .declaredVariables == []
        meta.getWorkflowDef('alpha') .source.stripIndent() == "print 'Hello world'\n"

        meta.getWorkflowDef('bravo') .declaredInputs == ['foo', 'bar']
        meta.getWorkflowDef('bravo') .declaredVariables == []
        meta.getWorkflowDef('bravo') .source.stripIndent() == "print foo\nprint bar\nreturn foo+bar\n"

        meta.getWorkflowDef('delta') .declaredInputs == ['foo']
        meta.getWorkflowDef('delta') .declaredVariables == ['bar']

        meta.getWorkflowDef('empty') .source == ''
        meta.getWorkflowDef('empty') .declaredInputs == []
        meta.getWorkflowDef('empty') .declaredVariables == []
    }

    def 'should define anonymous workflow' () {
        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(TestScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
                    
            workflow {
              print 1
              print 2
            }
        '''

        when:
        def binding = new ScriptBinding()
        def script = (TestScript)new GroovyShell(binding, config).parse(SCRIPT).run()
        def meta = ScriptMeta.get(script)
        then:
        meta.getWorkflowDef(null).getSource().stripIndent() == 'print 1\nprint 2\n'

    }

    def 'should run workflow block' () {

        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(TestScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
                    
            workflow alpha(x) {
              return "$x world"
            }
       
        '''

        when:
        def script = (TestScript)new GroovyShell(config).parse(SCRIPT).run()
        def workflow = ScriptMeta.get(script).getWorkflowDef('alpha')
        then:
        workflow.declaredInputs == ['x']

        when:
        def binding = new ScriptBinding()
        def result = workflow.invoke('Hello', binding)
        then:
        result == 'Hello world'
        binding.alpha.output == result

    }

}
