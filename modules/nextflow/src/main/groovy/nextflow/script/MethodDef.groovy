package nextflow.script

import java.lang.reflect.Method
import java.nio.file.Path

import groovy.transform.CompileStatic
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class MethodDef {

    BaseScript owner

    Path scriptPath

    Method method

    Method getMethod() { method }

    String getName() { method.name }

    Path getScriptPath() { scriptPath }

    BaseScript getOwner() { owner }

    Object invoke(Object...args) {
        method.invoke(owner, args)
    }
}
