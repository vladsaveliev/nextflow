/*
 * Copyright 2013-2019, Centre for Genomic Regulation (CRG)
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

import java.nio.file.Paths

import nextflow.processor.TaskConfig
import nextflow.processor.TaskRun
/**
 *
 * @author Lorenz Gerber <lorenzottogerber@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class NciPbsProExecutorTest extends Specification {

    def 'should get directives' () {
        given:
        def executor = Spy(NciPbsProExecutor)
        def WORK_DIR = Paths.get('/here')

        def task = Mock(TaskRun)
        task.getWorkDir() >> WORK_DIR
        task.getConfig() >> new TaskConfig()

        when:
        def result = executor.getDirectives(task, [])
        then:
        1 * executor.getJobNameFor(task) >> 'my-name'
        1 * executor.quote( WORK_DIR.resolve(TaskRun.CMD_LOG)) >> '/here/.command.log'

        result == [
            '-N', 'my-name',
            '-o', '/here/.command.log',
            '-j', 'oe'
        ]
    }

    def 'should get directives with queue' () {
        given:
        def executor = Spy(NciPbsProExecutor)
        def WORK_DIR = Paths.get('/foo/bar')

        def task = Mock(TaskRun)
        task.getWorkDir() >> WORK_DIR
        task.getConfig() >> new TaskConfig([ queue: 'my-queue' ])

        when:
        def result = executor.getDirectives(task, [])
        then:
        1 * executor.getJobNameFor(task) >> 'foo'
        1 * executor.quote( WORK_DIR.resolve(TaskRun.CMD_LOG)) >> '/foo/bar/.command.log'

        result == [
            '-N', 'foo',
            '-o', '/foo/bar/.command.log',
            '-j', 'oe',
            '-q', 'my-queue'
        ]
    }

    def 'should get directives with cpus' () {
        given:
        def executor = Spy(NciPbsProExecutor)
        def WORK_DIR = Paths.get('/foo/bar')

        def task = Mock(TaskRun)
        task.getWorkDir() >> WORK_DIR
        task.getConfig() >> new TaskConfig([ queue: 'my-queue', cpus:4 ])

        when:
        def result = executor.getDirectives(task, [])
        then:
        1 * executor.getJobNameFor(task) >> 'foo'
        1 * executor.quote( WORK_DIR.resolve(TaskRun.CMD_LOG)) >> '/foo/bar/.command.log'

        result == [
            '-N', 'foo',
            '-o', '/foo/bar/.command.log',
            '-j', 'oe',
            '-q', 'my-queue',
            '-l', 'ncpus=4'
        ]
    }

    def 'should get directives with mem' () {
        given:
        def executor = Spy(NciPbsProExecutor)
        def WORK_DIR = Paths.get('/foo/bar')

        def task = Mock(TaskRun)
        task.getWorkDir() >> WORK_DIR
        task.getConfig() >> new TaskConfig([ queue: 'my-queue', memory:'2 GB' ])

        when:
        def result = executor.getDirectives(task, [])
        then:
        1 * executor.getJobNameFor(task) >> 'foo'
        1 * executor.quote( WORK_DIR.resolve(TaskRun.CMD_LOG)) >> '/foo/bar/.command.log'

        result == [
            '-N', 'foo',
            '-o', '/foo/bar/.command.log',
            '-j', 'oe',
            '-q', 'my-queue',
            '-l', 'mem=2048mb'
        ]
    }

    def 'should get directives with cpus and mem' () {
        given:
        def executor = Spy(NciPbsProExecutor)
        def WORK_DIR = Paths.get('/foo/bar')

        def task = Mock(TaskRun)
        task.getWorkDir() >> WORK_DIR
        task.getConfig() >> new TaskConfig([ queue: 'my-queue', memory:'1 GB', cpus: 8 ])

        when:
        def result = executor.getDirectives(task, [])
        then:
        1 * executor.getJobNameFor(task) >> 'foo'
        1 * executor.quote( WORK_DIR.resolve(TaskRun.CMD_LOG)) >> '/foo/bar/.command.log'

        result == [
            '-N', 'foo',
            '-o', '/foo/bar/.command.log',
            '-j', 'oe',
            '-q', 'my-queue',
            '-l', 'ncpus=8',
            '-l', 'mem=1024mb'
        ]
    }

    def 'should return qstat command line' () {
        given:
        def executor = [:] as NciPbsProExecutor

        expect:
        executor.queueStatusCommand(null) == ['bash','-c', "set -o pipefail; qstat -u \$USER"]
        executor.queueStatusCommand('xxx') == ['bash','-c', "set -o pipefail; qstat -u \$USER"]
        executor.queueStatusCommand('xxx').each { assert it instanceof String }
    }

    def 'should parse queue status'() {

        setup:
        def executor = [:] as NciPbsProExecutor
        def text =
            """

r-man2:
                                                            Req'd  Req'd   Elap
Job ID          Username Queue    Jobname    SessID NDS TSK Memory Time  S Time
--------------- -------- -------- ---------- ------ --- --- ------ ----- - -----
8453988.r-man2  vs2870   normalbw c14m100     14650   1  14  100gb 48:00 R 24:06
8479227.r-man2  vs2870   normalbw nf-MapRead  48299   1  28   80gb 48:00 Q 02:27
8479228.r-man2  vs2870   normalbw nf-MapRead  36449   1  28   80gb 48:00 H 02:27
                """.stripIndent().trim()

        when:
        def result = executor.parseQueueStatus(text)
        then:
        println("result=${result}")
        result.size() == 3
        result['8453988.r-man2'] == AbstractGridExecutor.QueueStatus.RUNNING
        result['8479227.r-man2'] == AbstractGridExecutor.QueueStatus.PENDING
        result['8479228.r-man2'] == AbstractGridExecutor.QueueStatus.HOLD

    }


}
