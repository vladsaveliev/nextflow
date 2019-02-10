package nextflow.script

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@TupleConstructor
@CompileStatic
class CurrentScript {

    final BaseScript script

    final boolean main

    final ScriptBinding binding

    final ScriptLibrary library

    final static private Stack<CurrentScript> CURRENT = new Stack<>()

    @PackageScope static void push(BaseScript script, boolean main, ScriptBinding binding, ScriptLibrary library) {
        push( new CurrentScript(script, main, binding, library) )
    }

    @PackageScope static void push(CurrentScript lib) { CURRENT.push(lib) }

    @PackageScope static CurrentScript pop() { CURRENT.pop() }

    static CurrentScript get() { CURRENT.peek() }

}
