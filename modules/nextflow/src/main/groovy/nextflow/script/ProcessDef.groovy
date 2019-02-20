/*
 * Copyright 2013-2018, Centre for Genomic Regulation (CRG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.script


import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.Session
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessDef implements InvokableDef, Cloneable {

    private BaseScript owner

    private String name

    private ProcessConfig processConfig

    private TaskBody taskBody

    private Session session

    private Object output

    ProcessDef(BaseScript owner, String name, ProcessConfig config, TaskBody body) {
        this.owner = owner
        this.name = name
        this.taskBody = body
        this.processConfig = config
        this.session = owner.binding.session
    }

    ProcessDef clone() {
        def result = (ProcessDef)super.clone()
        result.@taskBody = taskBody.clone()
        result.@processConfig = processConfig.clone()
        return result
    }

    private InputsList getDeclaredInputs() { processConfig.getInputs() }

    private OutputsList getDeclaredOutputs() { processConfig.getOutputs() }

    BaseScript getOwner() { owner }

    String getName() { name }

    def getOutput() { output }

    Object invoke(Object[] args, Binding scope) {
        // use this instance an workflow template, therefore clone it
        def process = this.clone()
        // invoke the process execution
        def result = process.call0(args)
        // register this workflow invocation in the current context
        // so that it can be accessed in the outer execution scope
        scope.setProperty(name, process)
        return result
    }


    private call0(Object[] args) {
        if( args.size()==1 && args[0] instanceof ProcessOutputArray )
            args = args[0] as Object[]

        // sanity check
        if( args.size() != declaredInputs.size() )
            throw new IllegalArgumentException("Process `$name` declares ${declaredInputs.size()} input channels but ${args.size()} were specified")

        // set input channels
        for( int i=0; i<args.size(); i++ ) {
            declaredInputs[i].from(args[i])
        }

        // set output channels
        // note: the result object must be an array instead of a List to allow process
        // composition ie. to use the process output as the input in another process invocation
        List result = null
        if( declaredOutputs.size() ) {
            result = new ArrayList<>(declaredOutputs.size())
            final allScalarValues = declaredInputs.allScalarInputs()
            final hasEachParams = declaredInputs.any { it instanceof EachInParam }
            final singleton = allScalarValues && !hasEachParams

            for(int i=0; i<declaredOutputs.size(); i++ ) {
                def ch = singleton ? new DataflowVariable<>() : new DataflowQueue<>()
                result[i] = ch
                declaredOutputs[i].into(ch)
            }
        }

        // create the executor
        final executor = session
                .executorFactory
                .getExecutor(name, processConfig, taskBody, session)

        // create processor class
        session
                .newProcessFactory(owner)
                .newTaskProcessor(name, executor, processConfig, taskBody)
                .run()

        // the result object
        if( !result )
            return output=null

        return output = (result.size()==1
                ? output=result[0]
                : new ProcessOutputArray(result))
    }

}