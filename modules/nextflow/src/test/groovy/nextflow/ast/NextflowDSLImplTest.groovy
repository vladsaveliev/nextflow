package nextflow.ast

import spock.lang.Specification

import groovy.transform.InheritConstructors
import nextflow.script.BaseScript
import nextflow.script.IncludeDef
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class NextflowDSLImplTest extends Specification {

    def 'should fetch method names' () {

        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(BaseScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
            def foo() { 
                return 0 
            }
            
            def bar() { 
                return 1 
            }
            
            private baz() { 
                return 2 
            }
            
            process alpha {
              /hello/
            }

        '''
        when:
        new GroovyShell(config).parse(SCRIPT)
        then:
        noExceptionThrown()
    }

    def 'should not allow duplicate processes' () {
        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(BaseScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
                    
            process alpha {
              /hello/
            }
        
            process alpha {
              /world/
            }

        '''

        when:
        new GroovyShell(config).parse(SCRIPT)
        then:
        def err = thrown(MultipleCompilationErrorsException)
        err.message.contains 'Identifier `alpha` is already used by another definition'
    }


    def 'should not allow duplicate workflow' () {
        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(BaseScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
                    
            workflow alpha {
              /hello/
            }
        
            workflow alpha {
              /world/
            }

        '''

        when:
        new GroovyShell(config).parse(SCRIPT)
        then:
        def err = thrown(MultipleCompilationErrorsException)
        err.message.contains 'Identifier `alpha` is already used by another definition'
    }

    @InheritConstructors
    static class TestInclude extends IncludeDef {
        boolean loadInvoked
        @Override
        void load() {
            loadInvoked = true
        }
    }

    static abstract class TestScript extends BaseScript {

        List<TestInclude> includes = []

        @Override
        protected IncludeDef include(IncludeDef include) {
            def inc = new TestInclude(include.path, include.namespace)
            includes << inc
            return inc
        }

        @Override
        Object run() {
            return runScript()
        }
    }

    def 'should add includes' () {
        given:
        def config = new CompilerConfiguration()
        config.setScriptBaseClass(TestScript.class.name)
        config.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))

        def SCRIPT = '''
            
            include 'alpha'
            include 'bravo' params(a:1, b:2)
            include 'delta' as foo
            include 'gamma' as bar params(x:1)
            include 'omega' as _ params(z:2)
            
        '''

        when:
        def script = (TestScript)new GroovyShell(config).parse(SCRIPT)
        script.run()
        then:
        script.includes.size()==5
        script.includes[0].path == 'alpha'
        script.includes[0].namespace == null
        script.includes[0].params.size() == 0
        script.includes[0].loadInvoked

        script.includes[1].path == 'bravo'
        script.includes[1].namespace == null
        script.includes[1].params == [a:1, b:2]
        script.includes[1].loadInvoked

        script.includes[2].path == 'delta'
        script.includes[2].namespace == 'foo'
        script.includes[2].params == [:]
        script.includes[2].loadInvoked

        script.includes[3].path == 'gamma'
        script.includes[3].namespace == 'bar'
        script.includes[3].params == [x:1]
        script.includes[3].loadInvoked

        script.includes[4].path == 'omega'
        script.includes[4].namespace == '_'
        script.includes[4].params == [z:2]
        script.includes[4].loadInvoked
    }


}
