package nextflow.script

import spock.lang.Specification

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class ProcessOutputArrayTest extends Specification {

    def 'should get values' () {

        when:
        def arr = new ProcessOutputArray()
        then:
        arr.fifth == null
        arr.second == null

        when:
        arr = new ProcessOutputArray([1,2,3,4,5,6,7,8,9,10,11])
        then:
        arr.first == 1
        arr.second == 2
        arr.third == 3
        arr.fourth == 4
        arr.fifth == 5
        arr.sixth == 6
        arr.seventh == 7
        arr.eighth == 8
        arr.ninth == 9
        arr.tenth == 10

    }
}
