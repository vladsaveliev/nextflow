package nextflow.script

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.DataflowWriteChannel
import nextflow.Channel
import nextflow.extension.ChannelHelper
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class WorkflowDef implements InvokableDef, ComponentDef, Cloneable {

    private String name

    private TaskBody body

    private List<String> declaredInputs

    private Set<String> variableNames

    // -- following attributes are mutable and instance dependant
    // -- therefore should not be cloned

    private Binding context

    private output

    WorkflowDef(TaskBody body, String name=null, List<String> inputs = Collections.emptyList() ) {
        this.body = body
        this.name = name
        this.declaredInputs = inputs
        this.variableNames = getVarNames0()
    }

    WorkflowDef clone() {
        def copy = (WorkflowDef)super.clone()
        copy.@body = body.clone()
        return copy
    }

    String getName() { name }

    def getOutput() { output }

    @PackageScope TaskBody getBody() { body }

    @PackageScope List<String> getDeclaredInputs() { declaredInputs }

    @PackageScope String getSource() { body.source }

    @PackageScope List<String> getDeclaredVariables() { new ArrayList<String>(variableNames) }

    Binding getContext() { context }

    private Set<String> getVarNames0() {
        def variableNames = body.getValNames()
        if( variableNames ) {
            Set<String> declaredNames = []
            declaredNames.addAll( declaredInputs )
            if( declaredNames )
                variableNames = variableNames - declaredNames
        }
        return variableNames
    }


    protected void collectInputs(Binding context, Object[] args) {
        final params = ChannelArrayList.spread(args)
        if( params.size() != declaredInputs.size() )
            throw new IllegalArgumentException("Workflow `$name` declares ${declaredInputs.size()} input channels but ${params.size()} were specified")

        // attach declared inputs with the invocation arguments
        for( int i=0; i< declaredInputs.size(); i++ ) {
            final name = declaredInputs[i]
            context.setProperty( name, params[i] )
        }
    }

    protected Object collectOutputs(Object output) {
        if( output==null )
            return asChannel(null, true)

        if( output instanceof ChannelArrayList )
            return output

        if( output instanceof DataflowWriteChannel )
            return output

        if( !(output instanceof List) ) {
            return asChannel(output, true)
        }

        def result = asChannel(ChannelArrayList.spread(output))
        if( result.size()==0 )
            return null
        if( result.size()==1 )
            return result[0]
        return result
    }

    private List asChannel(List list) {
        final allScalar = ChannelHelper.allScalar(list)
        for( int i=0; i<list.size(); i++ ) {
            def el = list[i]
            if( !ChannelHelper.isChannel(el) ) {
                list[i] = asChannel(el, allScalar)
            }
        }
        return list
    }

    private DataflowWriteChannel asChannel(Object x, boolean var) {
        if( var ) {
            def result = new DataflowVariable()
            result.bind(x)
            return result
        }
        else {
            def result = new DataflowQueue()
            if( x != null ) {
                result.bind(x)
                result.bind(Channel.STOP)
            }
            return result
        }
    }

    Object invoke(Object[] args, Binding scope) {
        // use this instance an workflow template, therefore clone it
        final workflow = this.clone()
        // workflow execution
        WorkflowScope.get().push(workflow)
        try {
            final result = workflow.run0(args)
            // register this workflow invocation in the current scope
            // so that it can be accessed in the outer execution scope
            if( name )
                scope.setProperty(name, workflow)
            return result
        }
        finally {
            WorkflowScope.get().pop()
        }

    }

    private getProcessOrWorkflowOrFail(String name) {
        final meta = ScriptMeta.current()
        if( !meta )
            return null
        def result = meta.getProcess(name)
        if( !result )
            result = meta.getWorkflow(name)
        if( !result )
            throw new MissingPropertyException("Not such property: $name in workflow execution context")
        return result
    }

    private Binding createBinding() {
        new Binding() {
            @Override
            Object getProperty(String propertyName) {
                try {
                    return super.getProperty(propertyName)
                }
                catch (MissingPropertyException e) {
                    return getProcessOrWorkflowOrFail(propertyName)
                }
            }
        }
    }

    protected Object run0(Object[] args) {
        // setup the execution context
        context = createBinding()
        // setup the workflow inputs
        collectInputs(context, args)
        // invoke the workflow execution
        final closure = body.closure
        closure.delegate = context
        closure.setResolveStrategy(Closure.DELEGATE_FIRST)
        final result = closure.call()
        // collect the workflow outputs
        output = collectOutputs(result)
    }

}
