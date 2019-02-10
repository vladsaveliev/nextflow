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


import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.Session
import nextflow.processor.TaskProcessor
/**
 * Any user defined script will extends this class, it provides the base execution context
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
abstract class BaseScript extends Script {

    private Session session

    private ProcessFactory processFactory

    private TaskProcessor taskProcessor

    private ScriptLibrary library

    private boolean module

    /**
     * The list of process defined in the pipeline script
     */
    private List<String> processNames

    @Lazy InputStream stdin = { System.in }()

    /** only for testing purpose */
    private result

    BaseScript() { }

    BaseScript(Binding binding) {
        super(binding)
    }

    ScriptBinding getBinding() {
        (ScriptBinding)super.getBinding()
    }


    /**
     * This method is get invoked by the DSL parser
     * @param processNames
     */
    protected void init( List<String> processNames ) {
        this.processNames = processNames
    }

    @PackageScope
    List<String> getProcessNames() { processNames }

    /**
     * Holds the configuration object which will used to execution the user tasks
     */
    protected Map getConfig() {
        log.warn "The access of `config` object is deprecated"
        session.getConfig()
    }

    /**
     * Access to the last *process* object -- only for testing purpose
     */
    @PackageScope
    TaskProcessor getTaskProcessor() { taskProcessor }

    /**
     * Access to the last *process* result -- only for testing purpose
     */
    @PackageScope
    Object getResult() { result }


    /**
     * Enable disable task 'echo' configuration property
     * @param value
     */
    protected void echo(boolean value = true) {
        log.warn "The use of `echo` method is deprecated"
        session.getConfig().process.echo = value
    }

    private void setup() {
        module = binding.module
        session = binding.getSession()
        library = new ScriptLibrary(this)
        processFactory = session.newProcessFactory(this)

        binding.setVariable( 'baseDir', session.baseDir )
        binding.setVariable( 'workDir', session.workDir )
        binding.setVariable( 'workflow', session.workflowMetadata )
        binding.setVariable( 'nextflow', session.workflowMetadata?.nextflow )
    }

    protected process( Map<String,?> args, String name, Closure body ) {
        throw new DeprecationException("This process invocation syntax is deprecated")
    }

    protected process( String name, Closure body ) {

        if( module ) {
            def proc = processFactory.defineProcess(name, body)
            binding.getDefinedProcesses().add(proc)
        }
        else {
            // create and launch the process
            taskProcessor = processFactory.createProcessor(name, body)
            result = taskProcessor.run()
        }
    }

    protected void require(path) {
        require(Collections.emptyMap(), path)
    }

    protected void require(Map opts, path) {
        final params = opts.params ? (Map)opts.params : null
        library.load(path, params)
    }
    

    @Override
    Object invokeMethod(String name, Object args) {
        if( library.contains(name) && !module )
            library.invoke(name, args as Object[])
        else
            super.invokeMethod(name, args)
    }



    final Object run() {
        setup()
        CurrentScript.push(this, !module, binding, library)
        try {
            runScript()
        }
        finally {
            CurrentScript.pop()
        }
    }

    protected abstract Object runScript()

}
