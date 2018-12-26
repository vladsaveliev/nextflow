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

import java.nio.file.Path

import com.google.common.hash.Hashing
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import nextflow.Channel
import nextflow.Nextflow
import nextflow.Session
import nextflow.ast.NextflowDSL
import nextflow.ast.NextflowXform
import nextflow.processor.ProcessFactory
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ScriptParser {

    private Session session

    private Binding binding

    ScriptParser(Session session, Binding binding) {
        this.session = session
        this.binding = binding
    }

    @Memoized
    protected CompilerConfiguration getCompilerConfig() {
        // define the imports
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addImports( StringUtils.name, groovy.transform.Field.name )
        importCustomizer.addImports( Path.name )
        importCustomizer.addImports( Channel.name )
        importCustomizer.addImports( Duration.name )
        importCustomizer.addImports( MemoryUnit.name )
        importCustomizer.addStaticStars( Nextflow.name )

        def result = new CompilerConfiguration()
        result.addCompilationCustomizers( importCustomizer )
        result.scriptBaseClass = BaseScript.class.name
        result.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowDSL))
        result.addCompilationCustomizers( new ASTTransformationCustomizer(NextflowXform))

        result.setTargetDirectory(session.classesDir.toFile())
        return result
    }


    /**
     * Creates a unique name for the main script class in order to avoid collision
     * with the implicit and user variables
     */
    protected String computeClassName(String text) {
        final hash = Hashing
                .murmur3_32()
                .newHasher()
                .putUnencodedChars(text)
                .hash()

        return "_nf_script_${hash}"
    }

    BaseScript parse(Path scriptPath) {
        parse(scriptPath.text)
    }

    BaseScript parse(String scriptText) {
        def config = getCompilerConfig()
        def gcl = session.getClassLoader()
        def clazzName = computeClassName(scriptText)
        def groovy = new GroovyShell(gcl, binding, config)
        def script = groovy.parse(scriptText, clazzName) as BaseScript
        // create the process factory
        script.setProcessFactory(new ProcessFactory(script, session))
        return script
    }
}
