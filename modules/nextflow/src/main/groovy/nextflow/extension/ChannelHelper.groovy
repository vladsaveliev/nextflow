package nextflow.extension

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.expression.DataflowExpression
import groovyx.gpars.dataflow.stream.DataflowStreamReadAdapter
import groovyx.gpars.dataflow.stream.DataflowStreamWriteAdapter
import nextflow.Channel
import nextflow.script.WorkflowScope
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ChannelHelper {

    static Map<DataflowQueue, DataflowBroadcast> bridges = new HashMap<>(10)

    static DataflowWriteChannel createBy(DataflowReadChannel channel) {
        create( channel instanceof DataflowExpression )
    }

    static DataflowWriteChannel create(boolean value=false) {
        if( value )
            return new DataflowVariable()

        if( WorkflowScope.get().current() == null )
            return new DataflowQueue()

        else
            return new DataflowBroadcast()
    }

    static DataflowReadChannel getReadable(channel) {
        if (channel instanceof DataflowExpression)
            return channel

        if (channel instanceof DataflowQueue)
            return getReadable(channel)

        if (channel instanceof DataflowBroadcast)
            return getReadable(channel)

        throw new IllegalArgumentException("Illegal channel source type: ${channel?.getClass()?.getName()}")
    }

    static DataflowReadChannel getReadable(DataflowQueue queue) {
        def workflow = WorkflowScope.get().current()
        if( workflow==null )
            return queue

        def broadcast = bridges.get(queue)
        if( broadcast == null ) {
            broadcast = new DataflowBroadcast()
            bridges.put(queue, broadcast)
        }
        return broadcast.createReadChannel()
    }

    static DataflowReadChannel getReadable(DataflowBroadcast channel) {
        def workflow = WorkflowScope.get().current()
        if( workflow==null )
            throw new IllegalStateException("Broadcast channel are only allowed in a workflow definition scope")

        channel.createReadChannel()
    }

    static boolean isBridge(DataflowQueue queue) {
        bridges.get(queue) != null
    }

    static void broadcast() {
        // connect all dataflow queue variables to associated broadcast channel 
        for( DataflowQueue queue : bridges.keySet() ) {
            def broadcast = bridges.get(queue)
            queue.into(broadcast)
        }
    }

    @PackageScope
    static DataflowWriteChannel close0(DataflowWriteChannel source) {
        if( source instanceof DataflowExpression ) {
            if( !source.isBound() )
                source.bind(Channel.STOP)
        }
        else {
            source.bind(Channel.STOP)
        }
        return source
    }

    static boolean isChannel(obj) {
        obj instanceof DataflowReadChannel || obj instanceof DataflowWriteChannel
    }

    static boolean isChannelQueue(obj) {
        obj instanceof DataflowQueue || obj instanceof DataflowStreamReadAdapter || obj instanceof DataflowStreamWriteAdapter
    }

    static boolean allScalar(List args) {
        for( def el : args ) {
            if( isChannelQueue(el) ) {
                return false
            }
        }
        return true
    }

}
