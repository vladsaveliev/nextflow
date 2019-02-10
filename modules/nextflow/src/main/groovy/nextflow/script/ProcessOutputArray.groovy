package nextflow.script

import groovy.transform.CompileStatic

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessOutputArray implements List {

    @Delegate List target

    ProcessOutputArray(List c) {
        target = Collections.unmodifiableList(c)
    }
}
