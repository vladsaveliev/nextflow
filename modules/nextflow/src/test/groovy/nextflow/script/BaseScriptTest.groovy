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

import spock.lang.Specification

import groovyx.gpars.dataflow.DataflowReadChannel

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class BaseScriptTest extends Specification {


    def 'should define a process and invoke it' () {
        given:
        def SCRIPT = '''
        processDef foo {
          input: val sample
          output: stdout() 
          script:
          /echo Hello $sample/
        }
        
        hello_ch = Channel.from('world')
        
        foo(hello_ch)
        '''

        when:
        def runner = new ScriptRunner([process:[executor:'nope']])
        def result = runner.setScript(SCRIPT).execute()
        then:
        noExceptionThrown()
        result instanceof DataflowReadChannel
        result.val == 'echo Hello world'
    }

    def 'should define a process with multiple inputs' () {
        given:
        def SCRIPT = '''
        processDef foo {
          input: 
            val sample
            set pairId, reads
          output: 
            stdout() 
          script:
            /echo sample=$sample pairId=$pairId reads=$reads/
        }
        
        ch1 = Channel.from('world')
        ch2 = Channel.value(['x', '/some/file'])
        
        foo(ch1, ch2)
        '''

        when:
        def runner = new ScriptRunner([process:[executor:'nope']])
        def result = runner.setScript(SCRIPT).execute()
        then:
        noExceptionThrown()
        result instanceof DataflowReadChannel
        result.val == 'echo sample=world pairId=x reads=/some/file'
    }

    def 'should define and invoke as an operator' () {
        given:
        def SCRIPT = '''
        processDef foo {
          input: val sample
          output: stdout() 
          script:
          /echo Hello $sample/
        }
        
        Channel.from('world').foo()
        
        '''

        when:
        def runner = new ScriptRunner([process:[executor:'nope']])
        def result = runner.setScript(SCRIPT).execute()
        then:
        noExceptionThrown()
        result instanceof DataflowReadChannel
        result.val == 'echo Hello world'
    }


}
