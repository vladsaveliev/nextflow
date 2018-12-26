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

package nextflow.processor

import groovy.transform.CompileStatic
import nextflow.Session

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ProcessLibrary {

    private Session session

    private Map<String,ProcessDef> processDefs = [:]

    ProcessLibrary(Session session) {
        this.session = session
    }

    void register(ProcessDef process) {
        processDefs.put(process.name, process)
        session.binding.setVariable(process.name, process)
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

}
