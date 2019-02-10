/*
 * Copyright 2013-2018, Centre for Genomic Regulation (CRG)
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

import groovy.transform.CompileStatic
import nextflow.Session
import nextflow.exception.ProcessDuplicateException

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessLibrary {

    private Session session

    private Map<String,ProcessDef> processDefs = new LinkedHashMap<>(50)

    private Map<String,MethodDef> methodDefs = new LinkedHashMap<>(50)

    ProcessLibrary(Session session) {
        this.session = session
    }

    void register(ProcessDef process) {
        checkDuplicate(process)
        processDefs.put(process.name, process)
    }

    void register(MethodDef method) {
        methodDefs.put(method.name, method)
    }

    private void checkDuplicate( ProcessDef process ) {

        def other = processDefs[process.name]
        if( other ) {
            def message = """\
                Process `$process.name` is defined in multiple library files:
                  ${other.scriptPath.toUriString()}
                  ${process.scriptPath.toUriString()}
                """ .stripIndent()
            throw new ProcessDuplicateException(message)
        }
    }


    Object invoke(Object channel, String methodName, Object[] args, Throwable MISSING_METHOD=null) {
        def proc = processDefs.get(methodName)
        if( !proc ) {
            throw MISSING_METHOD ?: new MissingMethodException(methodName, channel.class, args)
        }
        def aa = new Object[args.size()+1]
        aa[0] = channel
        for( int i=0; i<args.size(); i++ )
            aa[i+1] = args[i]

        proc.call(aa)
    }

    boolean contains(String name) {
        methodDefs.containsKey(name) || processDefs.containsKey(name)
    }

    Object invoke(String name, Object[] args) {
        def method = methodDefs[name]
        if( method )
            return method.invoke(args)

        def proc = processDefs[name]
        if( proc )
            return proc.call(args)

        throw new MissingMethodException(name, session.scriptClass)
    }

}
