package nextflow.script

import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

import nextflow.Session
import nextflow.exception.DuplicateScriptDefinitionException
import test.TestHelper
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ScriptIncludesTest extends Specification {

    def 'should resolve module path' () {

        given:
        def PATH = SCRIPT as Path
        def context = new ScriptBinding().setScriptPath(PATH)
        def script = Mock(BaseScript)
        script.getBinding() >> context
        def lib = new ScriptIncludes(script)

        expect:
        lib.resolveModulePath(MODULE)  == RESOLVED as Path

        where:
        SCRIPT                      |   MODULE                  | RESOLVED
        '/some/path/main.nf'        | '/abs/foo.nf'             | '/abs/foo.nf'
        '/some/path/main.nf'        | 'module.nf'               | '/some/path/module.nf'
        '/some/path/main.nf'        | 'foo/bar.nf'              | '/some/path/foo/bar.nf'
        '/some/path/main.nf'        | 'http://host.com/path/mod.nf'   | 'http://host.com/path/mod.nf'
        'http://foo.com/dir/x.nf'   | 'z.nf'                    | 'http://foo.com/dir/z.nf'

    }

    def 'should load module'() {

        given:
        def session = new Session()
        def context = new ScriptBinding(session)
        def module = TestHelper.createInMemTempFile('foo.nf')
        def script = Mock(BaseScript)
        script.getBinding() >> context
        def lib = new ScriptIncludes(script)

        module.text = '''
        process foo {
          /echo foo/
        }
        
        process bar {
          /echo bar/
        }
        
        def hello() { return 'world' }
        '''

        when:
        lib.load(module, [:])
        then:
        ScriptMeta
                .get(lib.getIncludeScript(0))
                .getAllDefinedNames() == ['foo', 'bar', 'hello'] as Set

    }

    def 'should throw duplication error' () {
        given:
        def parent = Mock(BaseScript)
        def meta1 = Mock(ScriptMeta)
        meta1.getAllDefinedNames() >> (['foo'] as Set)
        meta1.getScriptPath() >> { Paths.get('/blah/blah/script1.nf') }

        def meta2 = Mock(ScriptMeta)
        meta2.getAllDefinedNames() >> (['foo'] as Set)
        meta2.getScriptPath() >> { Paths.get('/blah/blah/script2.nf') }

        def include1 = Mock(BaseScript)
        def include2 = Mock(BaseScript)

        ScriptIncludes loader = Spy(ScriptIncludes, constructorArgs:[parent])

        when:
        loader.addInclude0(include1)
        then:
        1 * loader.checkUniqueNames0(include1)
        1 * loader.getMeta(include1) >> meta1
        1 * loader.nameExists0('foo') >> null
        loader.getIncludeScripts().contains(include1)

        when:
        loader.addInclude0(include2)
        then:
        1 * loader.checkUniqueNames0(include2)
        1 * loader.getMeta(include2) >> meta2
        1 * loader.nameExists0('foo') >> meta1
        !loader.getIncludeScripts().contains(include2)
        thrown(DuplicateScriptDefinitionException)

    }

}
