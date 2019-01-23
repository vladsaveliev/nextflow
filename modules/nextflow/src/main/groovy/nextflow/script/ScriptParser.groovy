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

    ScriptParser(Session session) {
        this.session = session
    }

    @Memoized
    CompilerConfiguration getCompilerConfig() {
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

    protected void setupBinding(ScriptBinding binding) {
        binding.setVariable( 'baseDir', session.baseDir )
        binding.setVariable( 'workDir', session.workDir )
        binding.setVariable( 'workflow', session.workflowMetadata )
        binding.setVariable( 'nextflow', session.workflowMetadata?.nextflow )
    }

    GroovyShell getInterpreter(ScriptBinding binding) {
        setupBinding(binding)
        final config = getCompilerConfig()
        final gcl = session.getClassLoader()
        return new GroovyShell(gcl, binding, config)
    }

    BaseScript parse(String scriptText, GroovyShell interpreter) {
        final clazzName = computeClassName(scriptText)
        return (BaseScript)interpreter.parse(scriptText, clazzName)
    }

    BaseScript parse(String scriptText, ScriptBinding binding) {
        def interpreter = getInterpreter(binding)
        parse(scriptText, interpreter)
    }

    BaseScript parse(Path scriptPath, ScriptBinding binding) {
        parse(scriptPath.text, binding)
    }

}
