package nextflow.script

import groovy.transform.CompileStatic
import nextflow.exception.IllegalInvocationException
import nextflow.extension.OpCall
import nextflow.extension.OperatorEx

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class WorkflowBinding extends Binding  {

    private BaseScript owner

    private ScriptMeta meta

    WorkflowBinding() { }

    WorkflowBinding(Map vars) {
        super(vars)
    }

    WorkflowBinding(BaseScript owner) {
        super()
        setOwner(owner)
    }

    WorkflowBinding setOwner(BaseScript owner) {
        this.owner = owner
        this.meta = ScriptMeta.get(owner)
        return this
    }


    BaseScript getOwner() {
        return owner
    }

    @Override
    String toString() {
        "${this.getClass().getSimpleName()}[vars=${variables}]"
    }

    private void checkScope(ComponentDef component) {
        if( component instanceof ChainableDef && !ExecutionStack.withinWorkflow() ) {
            throw new IllegalInvocationException(component)
        }
    }

    @Override
    Object invokeMethod(String name, Object args) {
        final component = meta.getComponent(name)
        if( component ) {
            checkScope(component)
            return component.invoke_o(args)
        }

        // check it's an operator name
        if( OperatorEx.OPERATOR_NAMES.contains(name) )
            return OpCall.create(name, args)

        throw new MissingMethodException(name,this.getClass())
    }


    Object getVariable(String name) {
        try {
            super.getVariable(name)
        }
        catch( MissingPropertyException e ) {
            if( meta==null )
                throw e

            def component = meta.getComponent(name)
            if( component )
                return component

            // check it's an operator name
            if( OperatorEx.OPERATOR_NAMES.contains(name) )
                return OpCall.create(name)

            throw e
        }
    }

}
