package nextflow.script

import spock.lang.Specification

import nextflow.NextflowMeta
import test.MockScriptRunner

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ScriptPipesTest extends Specification {

    def setupSpec() { NextflowMeta.instance.enableModules() }
    def cleanupSpec() { NextflowMeta.instance.disableModules() }
    
    def 'should pipe processes' () {
        given:
        def SCRIPT =  '''
        process foo {
          input: val data 
          output: val result
          exec:
            result = "$data mundo"
        }     
        
        process bar {
            input: val data 
            output: val result
            exec: 
              result = data.toUpperCase()
        } 

        workflow {
           Channel.from('Hello') >> (foo | bar)
        }
        '''

        when:
        def runner = new MockScriptRunner()
        def result = runner.setScript(SCRIPT).execute()

        then:
        result[0].val == 'Hello mundo'
        result[1].val == 'HELLO'
    }

}
