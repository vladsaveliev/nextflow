package nextflow

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import nextflow.util.VersionNumber

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Singleton(strict = false)
@ToString(includeNames = true)
@EqualsAndHashCode
class NextflowMeta {

    final VersionNumber version
    final int build
    final String timestamp
    final Map enable

    private NextflowMeta() {
        version = new VersionNumber(Const.APP_VER)
        build = Const.APP_BUILDNUM
        timestamp = Const.APP_TIMESTAMP_UTC
        enable = new HashMap(10)
    }

    protected NextflowMeta(String ver, int build, String timestamp, Map enable = null ) {
        this.version = new VersionNumber(ver)
        this.build = build
        this.timestamp = timestamp
        this.enable = enable != null ? enable : new HashMap(10)
    }

    boolean isModuleEnabled() {
        enable.modules == true
    }

    void enableModules() {
        enable.modules=true
    }

    void disableModules() {
        enable.modules=false
    }
}
