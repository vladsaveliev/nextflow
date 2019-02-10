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

import java.nio.file.NoSuchFileException
import java.nio.file.Path

import groovy.transform.CompileStatic
import nextflow.exception.ProcessDuplicateException
import nextflow.exception.ProcessException
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ScriptLibrary {

    private BaseScript script

    private ScriptBinding context

    private Map<String,ProcessDef> processDefs = new LinkedHashMap<>(50)

    private Map<String,MethodDef> methodDefs = new LinkedHashMap<>(50)

    ScriptLibrary(BaseScript script) {
        this.script = script
        this.context = script.getBinding()
    }

    String toString() {
        "${this.class.simpleName}[processes=${processDefs.keySet().join(',')}; methods=${methodDefs.keySet().join(',')}]"
    }

    void load( module, Map params ) {
        assert module
        final path = resolveModulePath(module)
        try {
            final binding = new ScriptBinding() .setParams(params)

            // the execution of a library file has as side effect the registration of declared processes
            def parser = new ScriptParser(context.session)
                    .setModule(true)
                    .setBinding(binding)
                    .runScript(path)

            for( MethodDef method : parser.getDefinedMethods() ) {
                register(method)
            }

            for( ProcessDef process : parser.getDefinedProcesses() ) {
                register(process)
            }

        }
        catch( ProcessException e ) {
            throw e
        }
        catch( NoSuchFileException e ) {
            throw new IllegalArgumentException("Module file does not exists: $path")
        }
        catch( Exception e ) {
            throw new IllegalArgumentException("Unable to load module file: $path -- cause: ${e.cause?.message ?: e.message}", e)
        }
    }

    protected Path resolveModulePath( path ) {
        assert path

        final parent = context.getScriptPath()
        final result = path as Path
        if( result.isAbsolute() )
            return result

        if( result.scheme == parent.scheme )
            return parent.resolveSibling(result)

        if( path instanceof CharSequence )
            return parent.resolveSibling(path.toString())

        throw new IllegalArgumentException("Cannot resolve module path: ${result.toUriString()}")
    }


    void register(ProcessDef process) {
        checkName(process)
        processDefs.put(process.name, process)
    }

    void register(MethodDef method) {
        checkName(method)
        methodDefs.put(method.name, method)
    }

    private void checkName(MethodDef method) {
        if( script.getMetaClass().getMetaMethod(method.name, method) )
            throw new IllegalArgumentException("Method name already exists: $method.name")
    }

    private void checkName(ProcessDef process ) {

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
        final MethodDef method = methodDefs[name]
        if( method )
            return method.invoke(args)

        def proc = processDefs[name]
        if( proc )
            return proc.call(args)

        throw new MissingMethodException(name, method.getOwner().getClass())
    }

}
