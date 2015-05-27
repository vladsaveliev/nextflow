/*
 * Copyright (c) 2013-2015, Centre for Genomic Regulation (CRG).
 * Copyright (c) 2013-2015, Paolo Di Tommaso and the respective authors.
 *
 *   This file is part of 'Nextflow'.
 *
 *   Nextflow is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nextflow is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nextflow.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright 2003-2013 the original author or authors.
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
import ch.grengine.Grengine
import groovy.text.Template
import groovy.text.TemplateEngine
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * A template engine that uses {@link Grengine} to parse template scripts
 * It also that ignore dollar variable and uses a custom character for variable interpolation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class TaskTemplateEngine extends TemplateEngine {

    final static private int DOLLAR = (int)('$' as char)
    final static private int BACKSLASH = (int)('\\' as char)
    final static private int CURLY_OPEN = (int)('{' as char)
    final static private int CURLY_CLOSE = (int)('}' as char)
    final static private int DOUBLE_QUOTE = (int)('"' as char)
    final static private int NL = (int)('\n' as char)
    final static private int CR = (int)('\r' as char)
    final static private int PERIOD = (int)('.' as char)

    private Grengine grengine;

    private char placeholder = '$' as char

    private boolean enableShortNotation

    TaskTemplateEngine() {
        grengine = new Grengine()
    }

    TaskTemplateEngine( Grengine engine ) {
        this.grengine = engine
    }

    String render( String text, Map binding = null )  {
        def template = createTemplate(text)
        def result = binding ? template.make(binding) : template.make()
        result?.toString()
    }

    Template createTemplate(Reader reader) throws CompilationFailedException, IOException {
        ParsableTemplate template = placeholder == DOLLAR ? new SimpleTemplate() : new EscapeTemplate()
        String script = template.parse(reader);
        if( log.isTraceEnabled() ) {
            log.trace "\n-- script source --\n${script}\n-- script end --\n"
        }
        try {
            template.script = grengine.create(script);
        }
        catch (Exception e) {
            throw new GroovyRuntimeException("Failed to parse template script (your template may contain an error or be trying to use expressions not currently supported): " + e.getCause() ?: e.toString());
        }
        return template;
    }

    TaskTemplateEngine setEnableShortNotation(boolean value) {
        enableShortNotation = value
        return this
    }

    TaskTemplateEngine setPlaceholder(char ch) {
        placeholder = ch
        return this
    }

    /**
     * Common template methods
     */
    private static abstract class ParsableTemplate implements Template {

        abstract String parse(Reader reader) throws IOException

        protected Script script;

        protected void startScript(StringWriter sw) {
            sw.write('__$$_out.print("""');
        }

        protected void endScript(StringWriter sw) {
            sw.write('""");\n');
            sw.write('\n');
        }


        Writable make() {
            return make(null);
        }

        Writable make(final Map map) {
            return new Writable() {

                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                Writer writeTo(Writer writer) {
                    // Note: the code below, in order to print the string inject the a PrintVariable object
                    // in the binding map. To avoid any side-effect in the original a newly create HashMap
                    // is specified a binding object (however this do not prevent that inner referenced object
                    // can be modified e.g `obj.something++`)
                    Binding binding = map == null ? new Binding() : new Binding(new HashMap(map));
                    Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                    PrintWriter pw = new PrintWriter(writer);
                    // note:
                    scriptObject.setProperty('__$$_out', pw);
                    scriptObject.run();
                    pw.flush();
                    return writer;
                }

                /**
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
                String toString() {
                    StringWriter sw = new StringWriter();
                    writeTo(sw);
                    return sw.toString();
                }
            };
        }

    }

    /**
     * Template class escaping standard $ prefixed variables and using a custom character
     * as variable placeholder
     */
    private class EscapeTemplate extends ParsableTemplate {

        /**
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler, otherwise copy the text directly
         * into the script while escaping quotes.
         *
         * @param reader a reader for the template text
         * @return the parsed text
         * @throws IOException if something goes wrong
         */
        String parse(Reader reader) throws IOException {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }

            final PLACEHOLDER = (int)TaskTemplateEngine.this.placeholder

            StringWriter sw = new StringWriter();
            startScript(sw);
            int c;
            while ((c = reader.read()) != -1) {

                if( c == DOLLAR ) {
                    // escape dollar characters
                    sw.write(BACKSLASH)
                    sw.write(DOLLAR)
                    continue
                }

                if( c == BACKSLASH ) {
                    // escape backslash itself
                    sw.write(BACKSLASH)
                    sw.write(BACKSLASH)
                    continue
                }

                if( c == PLACEHOLDER ) {
                    reader.mark(1);
                    c = reader.read();      // read the next character
                    if( c == CURLY_OPEN ){
                        reader.mark(1)
                        sw.write(DOLLAR)    // <-- replace the placeholder with a $ char
                        sw.write(CURLY_OPEN)
                        processGString(reader, sw);
                    }
                    else if( enableShortNotation && Character.isJavaIdentifierStart(c) ) {
                        sw.write(DOLLAR)    // <-- replace the placeholder with a $ char
                        reader.reset()
                        processIdentifier(reader, sw)
                    }
                    else {
                        // just write this char and continue
                        sw.write(PLACEHOLDER)
                        sw.write(c)
                        // escape back slash doubling it
                        if (c == BACKSLASH) sw.write(c)
                    }

                    continue; // at least '$' is consumed ... read next chars.
                }

                if( c == DOUBLE_QUOTE ) {
                    sw.write('\\');
                }

                /*
                 * Handle raw new line characters.
                 */
                if( c == NL || c == CR ) {
                    if (c == CR) { // on Windows, "\r\n" is a new line.
                        reader.mark(1);
                        c = reader.read();
                        if (c != NL) {
                            reader.reset();
                        }
                    }
                    sw.write("\n");
                    continue;
                }
                sw.write(c);
            }
            endScript(sw);
            return sw.toString();
        }

        private void processGString(Reader reader, StringWriter sw) throws IOException {
            int c;
            def name = new StringBuilder()

            while ((c = reader.read()) != -1) {
                if( c != NL && c != CR ) {
                    sw.write(c);
                }
                if(c == CURLY_CLOSE ) {
                    break;
                }
                name.append(c)
            }
        }

        private void processIdentifier(Reader reader, StringWriter sw) {
            int c;
            int pos=0;
            def name = new StringBuilder()

            while ((c = reader.read()) != -1) {
                sw.write(c);

                if( (pos==0 && Character.isJavaIdentifierStart(c)) || Character.isJavaIdentifierPart(c) ) {
                    pos++
                    name.append(c)
                    continue
                }

                if( pos && c == PERIOD ) {
                    reader.mark(1)
                    char next = reader.read()
                    if( Character.isJavaIdentifierStart(next)) {
                        pos = 0;
                        sw.write(next)
                        name.append('.')
                        name.append(next)
                        continue
                    }
                    else {
                        reader.reset()
                    }
                }
                break
            }
        }

    }

    /**
     * Default template that interpolates variables
     */
    private static class SimpleTemplate extends ParsableTemplate {

        /**
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler, otherwise copy the text directly
         * into the script while escaping quotes.
         *
         * @param reader a reader for the template text
         * @return the parsed text
         * @throws IOException if something goes wrong
         */
        String parse(Reader reader) throws IOException {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringWriter sw = new StringWriter();
            startScript(sw);
            int c;
            while ((c = reader.read()) != -1) {

                if (c == DOLLAR) {
                    reader.mark(1);
                    c = reader.read();
                    if (c != CURLY_OPEN) {
                        sw.write(DOLLAR);
                        reader.reset();
                    } else {
                        reader.mark(1);
                        sw.write(DOLLAR);
                        sw.write(CURLY_OPEN);

                        processGString(reader, sw);
                    }
                    continue; // at least '$' is consumed ... read next chars.
                }
                if (c == DOUBLE_QUOTE) {
                    sw.write('\\');
                }
                /*
                 * Handle raw new line characters.
                 */
                if (c == NL || c == CR) {
                    if (c == CR) { // on Windows, "\r\n" is a new line.
                        reader.mark(1);
                        c = reader.read();
                        if (c != '\n') {
                            reader.reset();
                        }
                    }
                    sw.write("\n");
                    continue;
                }
                sw.write(c);
            }
            endScript(sw);
            return sw.toString();
        }


        private void processGString(Reader reader, StringWriter sw) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                if (c != NL && c != CR) {
                    sw.write(c);
                }
                if (c == CURLY_CLOSE) {
                    break;
                }
            }
        }
    }

}