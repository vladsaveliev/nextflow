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

import groovy.util.logging.Slf4j
import nextflow.processor.TaskRun

/**
 * Implements a executor for PBSPro cluster executor
 *
 * Tested with version:
 * - 14.2.4
 * - 19.0.0
 * See http://www.pbspro.org
 *
 * @author Lorenz Gerber <lorenzottogerber@gmail.com>
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
class NciPbsProExecutor extends PbsExecutor {

    /**
     * Gets the directives to submit the specified task to the cluster for execution
     *
     * @param task A {@link TaskRun} to be submitted
     * @param result The {@link List} instance to which add the job directives
     * @return A {@link List} containing all directive tokens and values.
     */
    @Override
    protected List<String> getDirectives(TaskRun task, List<String> result ) {
        assert result !=null

        result << '-N' << getJobNameFor(task)
        result << '-o' << quote(task.workDir.resolve(TaskRun.CMD_LOG))
        result << '-j' << 'oe'

        // the requested queue name
        if( task.config.queue ) {
            result << '-q' << (String)task.config.queue
        }

        // max task duration
        if( task.config.time ) {
            final duration = task.config.getTime()
            result << "-l" << (String)"walltime=${duration.format('HH:mm:ss')}"
        }

        if( task.config.cpus > 1 ) {
            result << '-l' << (String)"ncpus=${task.config.cpus}"
        }

        // task max memory
        if( task.config.memory ) {
            // https://www.osc.edu/documentation/knowledge_base/out_of_memory_oom_or_excessive_memory_usage
            result << '-l' << (String)"mem=${task.config.getMemory().getMega()}mb"
        }

        if( task.config.penv ) {
            result << '-P' << (String)task.config.penv
        }

        // -- at the end append the command script wrapped file name
        if( task.config.clusterOptions ) {
            result << task.config.clusterOptions.toString() << ''
        }

        return result
    }

    @Override
    protected List<String> queueStatusCommand(Object queue) {
        String cmd = 'qstat -u $USER'
        return ['bash','-c', "set -o pipefail; $cmd".toString()]
    }

    @Override
    protected Map<String, QueueStatus> parseQueueStatus(String text) {

        final result = [:]

        String id = null
        String status = null
        text.eachLine { line ->
            /*
            r-man2:
                                                                        Req'd  Req'd   Elap
            Job ID          Username Queue    Jobname    SessID NDS TSK Memory Time  S Time
            --------------- -------- -------- ---------- ------ --- --- ------ ----- - -----
            8453988.r-man2  vs2870   normalbw c14m100     14650   1  14  100gb 48:00 R 24:06
            8479227.r-man2  vs2870   normalbw nf-MapRead  48299   1  28   80gb 48:00 Q 02:27
            8479228.r-man2  vs2870   normalbw nf-MapRead  36449   1  28   80gb 48:00 H 02:27
            */
            def group = (line =~ /(\S+)\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+(\S+).*/)
            if (group.size() > 0 && group[0].size() >= 2) {
                id = group[0][1]
                if (id != "Job" && id != "---------------") {
                    status = group[0][2]
                    result.put( id, decode(status) ?: QueueStatus.UNKNOWN )
                }
            }
        }

        return result
    }

    // see https://www.pbsworks.com/pdfs/PBSRefGuide18.2.pdf
    // table 8.1
    static private Map DECODE_STATUS = [
            'F': QueueStatus.DONE,      // job is finished
            'E': QueueStatus.RUNNING,   // job is exiting (therefore still running)
            'R': QueueStatus.RUNNING,   // job is running 
            'Q': QueueStatus.PENDING,   // job is queued 
            'H': QueueStatus.HOLD,      // job is held
            'S': QueueStatus.HOLD,      // job is suspended 
            'U': QueueStatus.HOLD,      // job is suspended due to workstation becoming busy
            'W': QueueStatus.HOLD,      // job is waiting 
            'T': QueueStatus.HOLD,      // job is in transition
            'M': QueueStatus.HOLD,      // job was moved to another server
    ]

    @Override
    protected QueueStatus decode(String status) {
        DECODE_STATUS.get(status)
    }

}
