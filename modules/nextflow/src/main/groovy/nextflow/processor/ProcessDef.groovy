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
import groovyx.gpars.dataflow.DataflowChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import nextflow.Session
import nextflow.script.BaseScript
import nextflow.script.TaskBody
import static nextflow.extension.DataflowHelper.newChannelBy
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

    ProcessDef(BaseScript owner, Session session, String name, ProcessConfig config, TaskBody body) {
        super(owner)
        this.session = session
        this.name = name
        this.config = config
        this.body = body
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
        assert arg instanceof DataflowReadChannel
        assert config.getInputs().size()==1
        assert config.getOutputs().size() < 2

        config.getInputs().get(0).from(arg)

        DataflowChannel result=null
        if( config.getOutputs().size()==1 ) {
            result = newChannelBy(arg)
            config.getOutputs().get(0).into(result)
        }

        // create the executor
        final executor = session.processFactory.createExecutor(name, config, body)

        // -- create processor class
        session
                .processFactory
                .newTaskProcessor(name, executor, session, owner, config, body)
                .run()

        return result
    }

    @Override
    Object call(final Object... args) {
        println "Call 2"
        return null
    }

    @Override
    Object call() {
        println "Call 3"
        throw new UnsupportedOperationException()
    }
}
