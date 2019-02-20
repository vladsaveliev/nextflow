package nextflow.script

import java.lang.reflect.Method

import groovy.transform.CompileStatic
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@CompileStatic
class FunctionDef implements InvokableDef {

    private BaseScript owner

    private Method method

    private String name

    FunctionDef(BaseScript owner, Method method) {
        this.owner = owner
        this.method = method
        this.name = method.name
    }

    protected FunctionDef() { }

    Method getMethod() { method }

    String getName() { name }

    BaseScript getOwner() { owner }

    Object invoke(Object[] args, Binding binding) {
        method.invoke(owner, args)
    }
}
