package nextflow.script

import groovy.transform.CompileStatic

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ParallelDef extends ComponentDef implements ChainableDef {

    private List<ChainableDef> elements = new ArrayList<>(5)

    ParallelDef add(ChainableDef comp) {
        elements.add(comp)
        return this
    }

    String getType() { 'parallel' }

    ParallelDef withName(String name) {
        throw new UnsupportedOperationException()
    }

    @Override
    String getName() {
        return "( ${elements.collect{ it.name }.join(' & ')} )"
    }

    @Override
    Object invoke_a(Object[] args) {
        int i=0
        def result = new ArrayList(elements.size())
        for( def entry : elements )
            result[i++] = entry.invoke_a(args)

        new ChannelArrayList(ChannelArrayList.spread(result))
    }

}
