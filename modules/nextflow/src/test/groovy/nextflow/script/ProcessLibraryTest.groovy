package nextflow.script

import spock.lang.Specification

import java.nio.file.Path

import nextflow.Session
import test.TestHelper

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ProcessLibraryTest extends Specification {

    def 'should resolve module path' () {

        given:
        def PATH = SCRIPT as Path
        def context = new ScriptBinding().setScriptPath(PATH)
        def script = Mock(BaseScript)
        script.getBinding() >> context
        def lib = new ScriptLibrary(script)

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
        def context = new ScriptBinding().setSession(session)
        def module = TestHelper.createInMemTempFile('foo.nf')
        def script = Mock(BaseScript)
        script.getBinding() >> context
        def lib = new ScriptLibrary(script)

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
        lib.contains('foo')
        lib.contains('bar')
        lib.contains('hello')
        !lib.contains('none')
        lib.invoke('hello') == 'world'

    }

}
