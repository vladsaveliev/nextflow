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

    private Session session

    private String name

    private ProcessConfig config

    private TaskBody body

    private InputsList inputs

    private OutputsList outputs

    ProcessDef(BaseScript owner, Session session, String name, ProcessConfig config, TaskBody body) {
        super(owner)
        this.session = session
        this.name = name
        this.config = config
        this.body = body
        this.inputs = config.getInputs()
        this.outputs = config.getOutputs()
    }

    BaseScript getOwner() {
        return (BaseScript)super.getOwner()
    }

    @Override
    int getMaximumNumberOfParameters() { return 0 }

    @Override
    Class[] getParameterTypes() { EMPTY }

    @Override
    Object call(final Object arg) {
        run(arg)
    }

    @Override
    Object call(final Object... args) {
        run(args)
    }

    @Override
    Object call() {
        run()
    }

    protected run(Object... args) {
        // sanity check
        if( args.size() != inputs.size() )
            throw new IllegalArgumentException("Process `$name` declares ${inputs.size()} input channels but ${args.size()} were specified")

        // set input channels
        for( int i=0; i<args.size(); i++ ) {
            inputs[i].from(args[i])
        }

        // set output channels
        List result = null
        if( outputs.size() ) {
            result = new ArrayList(outputs.size())
            final allScalarValues = inputs.allScalarInputs()
            final hasEachParams = inputs.any { it instanceof EachInParam }
            final singleton = allScalarValues && !hasEachParams

            for( int i=0; i<outputs.size(); i++ ) {
                def ch = singleton ? new DataflowVariable<>() : new DataflowQueue<>()
                result.add(ch)
                outputs[i].into(ch)
            }
        }

        // create the executor
        final executor = session.processFactory.createExecutor(name, config, body)

        // -- create processor class
        session
                .processFactory
                .newTaskProcessor(name, executor, session, owner, config, body)
                .run()

        // the result object 
        if( result.size()==1 )
            return result[0]
        else if( result.size()>1 )
            return result
        else
            return null

    }
}
