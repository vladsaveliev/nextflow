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

package nextflow.processor

import groovy.transform.CompileStatic
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.Session
import nextflow.script.BaseScript
import nextflow.script.EachInParam
import nextflow.script.InputsList
import nextflow.script.OutputsList
import nextflow.script.TaskBody
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessDef extends Closure {

    private static Class[] EMPTY = []

    private String name

    private ProcessFactory processFactory

    private ProcessConfig processConfig

    private TaskBody taskBody

    private InputsList inputs

    private OutputsList outputs

    private Session session

    ProcessDef(BaseScript owner, String name, ProcessFactory factory, ProcessConfig config, TaskBody body) {
        super(owner)
        this.name = name
        this.processFactory = factory
        this.processConfig = config
        this.taskBody = body
        this.inputs = config.getInputs()
        this.outputs = config.getOutputs()
        this.session = processFactory.session
    }

    BaseScript getOwner() {
        return (BaseScript)super.getOwner()
    }

    String getName() { name }

    @Override
    int getMaximumNumberOfParameters() { return 0 }

    @Override
    Class[] getParameterTypes() { EMPTY }

    @Override
    Object call(final Object arg) {
        call0(arg)
    }

    @Override
    Object call(final Object... args) {
        call0(args)
    }

    @Override
    Object call() {
        call0()
    }

    private call0(Object... args) {
        // sanity check
        if( args.size() != inputs.size() )
            throw new IllegalArgumentException("Process `$name` declares ${inputs.size()} input channels but ${args.size()} were specified")

        // set input channels
        for( int i=0; i<args.size(); i++ ) {
            inputs[i].from(args[i])
        }

        // set output channels
        // note: the result object must be an array instead of a List to allow process
        // composition ie. to use the process output as the input in another process invocation
        Object[] result = null
        if( outputs.size() ) {
            result = new Object[outputs.size()]
            final allScalarValues = inputs.allScalarInputs()
            final hasEachParams = inputs.any { it instanceof EachInParam }
            final singleton = allScalarValues && !hasEachParams

            for( int i=0; i<outputs.size(); i++ ) {
                def ch = singleton ? new DataflowVariable<>() : new DataflowQueue<>()
                result[i] = ch
                outputs[i].into(ch)
            }
        }

        // create the executor
        final executor = session
                .executorFactory
                .getExecutor(name, processConfig, taskBody, session)

        //create processor class
        processFactory
                .newTaskProcessor(name, executor, processConfig, taskBody)
                .run()

        // the result object
        if( !result )
            return null
        if( result.size()==1 )
            return result[0]
        else
            return result

    }
}
