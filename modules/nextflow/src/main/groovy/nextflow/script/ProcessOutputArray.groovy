package nextflow.script

import groovy.transform.CompileStatic

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessOutputArray implements List {

    @Delegate List target

    ProcessOutputArray() {
        target = Collections.emptyList()
    }

    ProcessOutputArray(List c) {
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
}
