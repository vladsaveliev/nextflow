package nextflow.script

import spock.lang.Specification
import spock.lang.Timeout

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.NextflowMeta
import nextflow.ast.NextflowDSL
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import test.MockScriptRunner
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Timeout(5)
class WorkflowDefTest extends Specification {

    def setupSpec() { NextflowMeta.instance.enableModules() }
    def cleanupSpec() { NextflowMeta.instance.disableModules() }

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
        meta.getDefinedWorkflow('alpha') .declaredInputs == []
        meta.getDefinedWorkflow('alpha') .declaredVariables == []
        meta.getDefinedWorkflow('alpha') .source.stripIndent() == "print 'Hello world'\n"

        meta.getDefinedWorkflow('bravo') .declaredInputs == ['foo', 'bar']
        meta.getDefinedWorkflow('bravo') .declaredVariables == []
        meta.getDefinedWorkflow('bravo') .source.stripIndent() == "print foo\nprint bar\nreturn foo+bar\n"

        meta.getDefinedWorkflow('delta') .declaredInputs == ['foo']
        meta.getDefinedWorkflow('delta') .declaredVariables == ['bar']

        meta.getDefinedWorkflow('empty') .source == ''
        meta.getDefinedWorkflow('empty') .declaredInputs == []
        meta.getDefinedWorkflow('empty') .declaredVariables == []
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
        meta.getDefinedWorkflow(null).getSource().stripIndent() == 'print 1\nprint 2\n'

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
        def workflow = ScriptMeta.get(script).getDefinedWorkflow('alpha')
        then:
        workflow.declaredInputs == ['x']

        when:
        def binding = new ScriptBinding()
        def result = workflow.invoke('Hello', binding)
        then:
        result instanceof DataflowReadChannel
        result.val == 'Hello world'
        binding.alpha.output == result

    }

    def 'should not fail' () {
        given:
        // this test checks that the closure used to define the workflow
        // does NOT define an implicit `it` parameter that would clash
        // with the `it` used by the inner closure

        def SCRIPT = """       
        
        workflow {
            Channel.empty().map { id -> id +1 }  
            Channel.empty().map { it -> def id = it+1 }  
        }
        """

        when:
        new MockScriptRunner().setScript(SCRIPT).execute()

        then:
        noExceptionThrown()
    }

    def 'should validate collect output'() {

        given:
        def workflow = new WorkflowDef(Mock(TaskBody))
        def result

        when:
        result = workflow.collectOutputs(null)
        then:
        result instanceof DataflowVariable
        result.val == null

        when:
        def array = new ChannelArrayList()
        result = workflow.collectOutputs(array)
        then:
        result == array

        when:
        def dataVar = new DataflowVariable()
        result = workflow.collectOutputs(dataVar)
        then:
        result == dataVar

        when:
        def dataQueue = new DataflowQueue()
        result = workflow.collectOutputs(dataQueue)
        then:
        result == dataQueue

        when:
        def dataBroad = new DataflowBroadcast()
        result = workflow.collectOutputs(dataBroad)
        then:
        result == dataBroad

        when:
        result = workflow.collectOutputs(['a', 'b'])
        then:
        result.size() == 2 
        result[0] instanceof DataflowVariable
        result[0].val == 'a'
        result[1] instanceof DataflowVariable
        result[1].val == 'b'

        when:
        result = workflow.collectOutputs(['a', dataQueue])
        then:
        result.size() == 2
        result[0] instanceof DataflowQueue
        result[0].val == 'a'
        result[1] instanceof DataflowQueue

        when:
        def var1 = new DataflowQueue(); var1 << 'a'
        def var2 = new DataflowQueue(); var2 << 'b'
        def var3 = new DataflowQueue(); var3 << 'c'
        result = workflow.collectOutputs([var1, new ChannelArrayList([var2, var3])])
        then:
        result.size() == 3
        result[0].val == 'a'
        result[1].val == 'b'
        result[2].val == 'c'
    }

}
