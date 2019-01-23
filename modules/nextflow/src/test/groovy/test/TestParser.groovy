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

package test


import groovy.transform.InheritConstructors
import nextflow.Session
import nextflow.executor.Executor
import nextflow.processor.ProcessConfig
import nextflow.processor.ProcessFactory
import nextflow.processor.TaskProcessor
import nextflow.script.BaseScript
import nextflow.script.ScriptBinding
import nextflow.script.ScriptParser
import nextflow.script.TaskBody
/**
 * An helper class to parse nextflow script snippets
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class TestParser {

    Session session

    TestParser() {
        session = new TestSession()
    }

    TestParser( Session session1 ) {
        session = session1
    }

    BaseScript parseScript( String scriptText, Map map = null ) {
        if( map!=null ) session.binding = new TestBinding(session, map)
        session.init(null,null)
        def script = new ScriptParser(session).parse(scriptText, session.binding)
        return script
    }

    TaskProcessor parseAndGetProcess( String scriptText ) {
        def script = parseScript(scriptText)
        script.run()
        return script.getTaskProcessor()
    }


    static TaskProcessor parseAndReturnProcess( String scriptText, Map map = [:] ) {
        def script = new TestParser().parseScript(scriptText, map)
        script.run()
        return script.getTaskProcessor()
    }


    static class MockProcessFactory extends ProcessFactory {

        BaseScript script
        Session session

        MockProcessFactory(BaseScript script, Session session) {
            super(script,session)
            this.script = script
            this.session = session
        }

        @Override
        TaskProcessor newTaskProcessor( String name, Executor executor, ProcessConfig config, TaskBody taskBody ) {
            new MockTaskProcessor(name, executor, session, script, config, taskBody)
        }

    }

    @InheritConstructors
    static class MockTaskProcessor extends TaskProcessor {
        @Override
        def run () { }
    }

    @InheritConstructors
    static class TestSession extends Session {

        @Override
        ProcessFactory newProcessFactory(BaseScript script) {
            return new MockProcessFactory(script, this)
        }
    }

    static class TestBinding extends ScriptBinding {
        TestBinding( Session session, Map map ) {
            super(map)
            setSession(session)
        }
    }

}
