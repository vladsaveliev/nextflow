/*
 * Copyright 2013-2019, Centre for Genomic Regulation (CRG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nextflow.script

import groovy.transform.Memoized
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.processor.ProcessFactory
import nextflow.processor.TaskProcessor
/**
 * Any user defined script will extends this class, it provides the base execution context
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
abstract class BaseScript extends Script {

    protected BaseScript() { }

    protected BaseScript(Binding binding) {
        super(binding)
    }

    /**
     * This method is get invoked by the DSL parser
     * @param processNames
     */
    protected void init( List<String> processNames ) {
        this.processNames = processNames
    }

    @Override
    ScriptBinding getBinding() {
        return (ScriptBinding)super.getBinding()
    }

    private boolean isModule() {
        getBinding().isModule()
    }

    /**
     * The list of process defined in the pipeline script
     */
    private List<String> processNames

    @Memoized
    private ProcessFactory getProcessFactory() {
        new ProcessFactory(this, binding.session)
    }

    @PackageScope
    List<String> getProcessNames() { processNames }

    /**
     * Holds the configuration object which will used to execution the user tasks
     */
    Map getConfig() {
        log.warn "The access of `config` object is deprecated"
        getProcessFactory().getSession().getConfig()
    }

    @Lazy
    InputStream stdin = { System.in }()

    private TaskProcessor taskProcessor

    /** Access to the last *process* object -- only for testing purpose */
    @PackageScope
    TaskProcessor getTaskProcessor() { taskProcessor }

    private result

    /** Access to the last *process* result -- only for testing purpose */
    @PackageScope
    Object getResult() { result }


    /**
     * Enable disable task 'echo' configuration property
     * @param value
     */
    void echo(boolean value = true) {
        config.process.echo = value
    }


    /**
     * Method to which is mapped the *process* declaration when using the following syntax:
     *    <pre>
     *        process myProcess( option: value[, [..]] ) &#123;
     *        ..
     *        &#125;
     *    </pre>
     *
     * @param args The process options specified in the parenthesis after the process name, as shown in the above example
     * @param name The name of the process, e.g. {@code myProcess} in the above example
     * @param body The body of the process declaration. It holds all the process definitions: inputs, outputs, code, etc.
     */
    protected process( Map<String,?> args, String name, Closure body ) {
        throw new DeprecationException("This process invocation syntax is deprecated")
    }

    /**
     * Method to which is mapped the *process* declaration.
     *
     *    <pre>
     *        process myProcess( option: value[, [..]] ) &#123;
     *        ..
     *        &#125;
     *    </pre>
     * @param name The name of the process, e.g. {@code myProcess} in the above example
     * @param body The body of the process declaration. It holds all the process definitions: inputs, outputs, code, etc.
     */
    protected process( String name, Closure body ) {
        if( isModule() ) {
            getProcessFactory().defineProcess(name,body)
        }
        else {
            // create and launch the process
            taskProcessor = getProcessFactory().createProcessor(name, body)
            result = taskProcessor.run()
        }
    }

    protected void require(path) {
        getProcessFactory().require(path)
    }

}
