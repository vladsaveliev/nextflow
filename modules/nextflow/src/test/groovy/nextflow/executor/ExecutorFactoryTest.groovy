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

package nextflow.executor

import spock.lang.Specification

import nextflow.cloud.aws.batch.AwsBatchExecutor
import nextflow.k8s.K8sExecutor
import nextflow.processor.TaskHandler
import nextflow.processor.TaskMonitor
import nextflow.processor.TaskRun
import nextflow.script.ScriptType
import nextflow.util.ServiceName
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ExecutorFactoryTest extends Specification {


    def 'should load executor class' () {

        setup:
        def factory = new ExecutorFactory()
        factory.executorsMap [ 'x' ] = XExecutor.name   // // <-- this is loaded by the name
        expect:
        factory.loadExecutorClass(null) == LocalExecutor
        factory.loadExecutorClass('local') == LocalExecutor
        factory.loadExecutorClass('sge') == SgeExecutor
        factory.loadExecutorClass('oge') == SgeExecutor
        factory.loadExecutorClass('uge') == SgeExecutor
        factory.loadExecutorClass('lsf') == LsfExecutor
        factory.loadExecutorClass('pbs') == PbsExecutor
        factory.loadExecutorClass('slurm') == SlurmExecutor
        factory.loadExecutorClass('condor') == CondorExecutor
        factory.loadExecutorClass('k8s') == K8sExecutor
        factory.loadExecutorClass('awsbatch') == AwsBatchExecutor
        factory.loadExecutorClass('AwsBatch') == AwsBatchExecutor
        factory.loadExecutorClass('x') == XExecutor  // <-- this is loaded by the name

        when:
        factory.loadExecutorClass('xyz')
        then:
        thrown(IllegalArgumentException)

    }

    def 'should sge executor'() {
        when:
        def factory = new ExecutorFactory()
        then:
        factory.loadExecutorClass('sge') == SgeExecutor
    }


    def 'should check type supported'() {

        setup:
        def factory = new ExecutorFactory()

        when:
        factory.isTypeSupported(ScriptType.GROOVY, 'xxx')
        then:
        thrown(IllegalArgumentException)

        expect:
        // by default only script are supported
        factory.isTypeSupported(ScriptType.SCRIPTLET, Executor,)
        !factory.isTypeSupported(ScriptType.GROOVY, Executor)
        // same for grid
        factory.isTypeSupported(ScriptType.SCRIPTLET, SgeExecutor)
        !factory.isTypeSupported(ScriptType.GROOVY, SgeExecutor)

        // local supports both                               x
        factory.isTypeSupported(ScriptType.SCRIPTLET, LocalExecutor)
        factory.isTypeSupported(ScriptType.GROOVY, LocalExecutor)

        // repeat for instances
        factory.isTypeSupported(ScriptType.SCRIPTLET, new SgeExecutor() )
        !factory.isTypeSupported(ScriptType.GROOVY, new SgeExecutor())
        factory.isTypeSupported(ScriptType.SCRIPTLET, new LocalExecutor())
        factory.isTypeSupported(ScriptType.GROOVY, new LocalExecutor())
    }


    def 'should return executor name'() {
        expect:
        ExecutorFactory.findNameByClass( NopeExecutor ) == 'nope'
        ExecutorFactory.findNameByClass( SgeExecutor ) == 'sge'
        ExecutorFactory.findNameByClass(XExecutor) == 'my_fancy_name'
    }
}


@ServiceName('my_fancy_name')
class XExecutor extends Executor {
    @Override
    protected TaskMonitor createTaskMonitor() {
        return null
    }

    @Override
    TaskHandler createTaskHandler(TaskRun task) {
        return null
    }
}
