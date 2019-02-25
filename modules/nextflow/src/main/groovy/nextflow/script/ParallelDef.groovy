package nextflow.script

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ParallelDef implements ComponentDef {

    private List<ComponentDef> elements = new ArrayList<>(5)

    ParallelDef add(ComponentDef comp) {
        elements.add(comp)
        return this
    }

    @Override
    String getName() {
        return "( ${elements.collect{ it.name }.join(' | ')} )"
    }

    @Override
    Object invoke(Object[] args, Binding binding) {
        int i=0
        def result = new ArrayList(args.size())
        for( def entry : elements )
            result[i++] = entry.invoke(args, binding)

        new ChannelArrayList(ChannelArrayList.spread(result))
    }
}
