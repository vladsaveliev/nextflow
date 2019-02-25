package nextflow.extension

import spock.lang.Specification

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.script.WorkflowDef
import nextflow.script.WorkflowScope

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ChannelHelperTest extends Specification {

    def 'should create dataflow variable or queue' () {

        expect:
        ChannelHelper.create() instanceof DataflowQueue
        ChannelHelper.create(false) instanceof DataflowQueue
        ChannelHelper.create(true) instanceof DataflowVariable

        ChannelHelper.createBy(new DataflowVariable()) instanceof DataflowVariable
        ChannelHelper.createBy(new DataflowQueue()) instanceof DataflowQueue


        when:
        WorkflowScope.get().push(Mock(WorkflowDef))
        then:
        ChannelHelper.create() instanceof DataflowBroadcast
        ChannelHelper.create(false) instanceof DataflowBroadcast
        ChannelHelper.create(true) instanceof DataflowVariable

        ChannelHelper.createBy(new DataflowVariable()) instanceof DataflowVariable
        ChannelHelper.createBy(new DataflowQueue()) instanceof DataflowBroadcast

        cleanup:
        WorkflowScope.get().pop()

    }


    def 'should check queue channel' () {
        expect:
        ChannelHelper.isChannelQueue(new DataflowQueue())
        ChannelHelper.isChannelQueue(new DataflowBroadcast().createReadChannel())
        !ChannelHelper.isChannelQueue(new DataflowVariable())
        !ChannelHelper.isChannelQueue('hello')
    }

    def 'should validate allScalar method' () {

        expect:
        ChannelHelper.allScalar([1])
        ChannelHelper.allScalar([1,2,3])
        !ChannelHelper.allScalar([new DataflowVariable(), new DataflowQueue()])
        !ChannelHelper.allScalar([new DataflowQueue(), new DataflowQueue()])
    }

}
