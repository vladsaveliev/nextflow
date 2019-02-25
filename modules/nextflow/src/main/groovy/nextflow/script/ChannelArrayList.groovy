package nextflow.script

import groovy.transform.CompileStatic

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ChannelArrayList implements List {

    @Delegate List target

    ChannelArrayList() {
        target = Collections.emptyList()
    }

    ChannelArrayList(List c) {
        target = Collections.unmodifiableList(c)
    }

    def getFirst() { target[0] }

    def getSecond() { target[1] }

    def getThird() { target[2] }

    def getFourth() { target[3] }

    def getFifth() { target[4] }

    def getSixth() { target[5] }

    def getSeventh() { target[6] }

    def getEighth() { target[7] }

    def getNinth() { target[8] }

    def getTenth() { target[9] }

    /**
     * Helper method that `spread`
     * @param args
     * @return
     */
    static List spread(Object[] args) {
        final result = new ArrayList(args.size()*2)
        for( int i=0; i<args.size(); i++ ) {
            if( args[i] instanceof ChannelArrayList ) {
                final list = (List)args[i]
                for( def el : list ) {
                    result.add(el)
                }
            }
            else {
                result.add(args[i])
            }
        }
        return result
    }

    static List spread(List args) {
        spread(args as Object[])
    }
}
