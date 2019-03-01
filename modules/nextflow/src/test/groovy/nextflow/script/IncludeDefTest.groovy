package nextflow.script

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.NoSuchFileException
import java.nio.file.Path

import test.TestHelper
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class IncludeDefTest extends Specification {

    @Unroll
    def 'should resolve module path #MODULE' () {

        given:
        def script = '/some/path/main.nf' as Path
        def include = new IncludeDef(ownerScript: script)

        expect:
        include.resolveModulePath('/abs/foo.nf') == '/abs/foo.nf' as Path
        include.resolveModulePath('module.nf') == '/some/path/module.nf' as Path
        include.resolveModulePath('foo/bar.nf') == '/some/path/foo/bar.nf' as Path

        when:
        include.resolveModulePath('http://foo.com/bar')
        then:
        thrown(IllegalArgumentException)

    }

    def 'should resolve real module path' () {

        given:
        def folder = TestHelper.createInMemTempDir()
        def script = folder.resolve('main.nf'); script.text = 'echo ciao'
        def module = folder.resolve('mod-x.nf'); module.text = 'blah blah'

        def include = new IncludeDef(ownerScript: script)

        when:
        def result = include.realModulePath('mod-x.nf')
        then:
        result == module

        when:
        result = include.realModulePath('mod-x')
        then:
        result == module

        when:
        include.realModulePath('xyz')
        then:
        thrown(NoSuchFileException)

    }

}
