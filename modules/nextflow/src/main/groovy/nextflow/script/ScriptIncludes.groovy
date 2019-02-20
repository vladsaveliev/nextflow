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
import nextflow.exception.DuplicateScriptDefinitionException
import nextflow.exception.ProcessException
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ScriptIncludes {

    private BaseScript script

    private ScriptBinding context

    private List<BaseScript> allIncludes = new ArrayList<>(10)

    ScriptIncludes(BaseScript script) {
        this.script = script
        this.context = script.getBinding()
    }

    String toString() {
        "${this.class.simpleName}"
    }

    List<BaseScript> getIncludeScripts() { allIncludes }

    BaseScript getIncludeScript(int index) {
        allIncludes[index]
    }

    void load( module, Map params ) {
        assert module
        final path = resolveModulePath(module)
        try {
            final binding = new ScriptBinding() .setParams(params)

            // the execution of a library file has as side effect the registration of declared processes
            def result = new ScriptParser(context.session)
                    .setModule(true)
                    .setBinding(binding)
                    .runScript(path)

            addInclude0(result.script)
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

    protected ScriptMeta getMeta(BaseScript script) {
        ScriptMeta.get(script)
    }

    protected void addInclude0(BaseScript includeScript) {
        // check that declared names do not conflict with other includes
        checkUniqueNames0(includeScript)
        allIncludes.add(includeScript)
    }

    protected void checkUniqueNames0(BaseScript includeScript) {
        def meta = getMeta(includeScript)
        for( String name : meta.getAllDefinedNames() ) {
            def found = nameExists0(name)
            if( found ) {
              def msg = """\
                Duplicate definition `$name` in the following scripts:
                - ${meta.scriptPath.toUriString()}
                - ${found.scriptPath.toUriString()}
                """.stripIndent()
              throw new DuplicateScriptDefinitionException(msg)
            }
        }
    }

    protected ScriptMeta nameExists0(String name) {
        for( BaseScript script : allIncludes ) {
            final other = getMeta(script)
            if( other.containsDef(name) ) {
                return other
            }
        }
        return null
    }


    List<InvokableDef> getDefinitions() {
        def result = new ArrayList()
        for( BaseScript script : allIncludes ) {
            result.addAll( getMeta(script).getDefinitions() )
        }
        return result
    }

}
