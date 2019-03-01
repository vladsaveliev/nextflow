package nextflow.script

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import nextflow.exception.DuplicateScriptDefinitionException

/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@CompileStatic
class ScriptMeta {

    static public String ROOT_NAMESPACE = '_'

    static private List<String> INVALID_FUNCTION_NAMES = [
            'methodMissing',
            'propertyMissing'
    ]

    static private Map<BaseScript,ScriptMeta> REGISTRY = new HashMap<>(10)

    static ScriptMeta get(BaseScript script) {
        if( !script ) throw new IllegalStateException("Missing current script context")
        REGISTRY.get(script)
    }

    static ScriptMeta current() {
        get(ExecutionScope.script())
    }

    private Class<? extends BaseScript> clazz
    private Path scriptPath
    private Map<String,InvokableDef> definitions = new HashMap<>(10)
    private Map<String,ScriptMeta> imports = new HashMap<>(10)
    private boolean module

    Path getScriptPath() { scriptPath }

    boolean isModule() { module }

    ScriptMeta(BaseScript script) {
        this.clazz = script.class
        for( def entry : definedFunctions0(script) ) {
            definitions.put(entry.name, entry)
        }
    }

    @PackageScope
    void setScriptPath(Path path) {
        scriptPath = path
    }

    @PackageScope
    void setModule(boolean val) {
        this.module = val
    }

    @PackageScope
    static ScriptMeta register(BaseScript script) {
        def meta = new ScriptMeta(script)
        REGISTRY.put(script, meta)
        return meta
    }

    static List<FunctionDef> definedFunctions0(BaseScript script) {
        def allMethods = script.class.getDeclaredMethods()
        def result = new ArrayList(allMethods.length)
        for( Method method : allMethods ) {
            if( !Modifier.isPublic(method.getModifiers()) ) continue
            if( Modifier.isStatic(method.getModifiers())) continue
            if( method.name.startsWith('super$')) continue
            if( method.name in INVALID_FUNCTION_NAMES ) continue

            result.add(new FunctionDef(script, method))
        }
        return result
    }

    ScriptMeta addDefinition(InvokableDef invokable) {
        definitions.put(invokable.name, invokable)
        return this
    }

    ScriptMeta addDefinition(InvokableDef... invokable) {
        for( def item : invokable ) {
            definitions.put(item.name, item)
        }
        return this
    }

    Collection<InvokableDef> getDefinitions() { definitions.values() }

    InvokableDef getInvokable(String name) {
        definitions.get(name) ?: imports.get(ROOT_NAMESPACE)?.getInvokable(name)
    }

    WorkflowDef getWorkflow(String name) {
        (WorkflowDef)getInvokable(name)
    }

    ProcessDef getProcess(String name) {
        (ProcessDef)getInvokable(name)
    }

    FunctionDef getFunction(String name) {
        (FunctionDef)getInvokable(name)
    }

    Set<String> getProcessNames() {
        def result = new TreeSet()
        // local definitions
        for( def item : getDefinitions() ) {
            if( item instanceof ProcessDef )
                result.add(item.name)
        }
        // processes from imports
        for( def ns: imports.keySet() ) {
            def meta = imports.get(ns)
            for( def item : meta.getDefinitions()) {
                if( item instanceof ProcessDef )
                    result.add( ns==ROOT_NAMESPACE ? item.name : "${ns}.${item.name}" )
            }
        }

        return result
    }

    void addModule(String namespace, BaseScript script) {
       addModule(namespace, get(script))
    }

    void addModule(String namespace, ScriptMeta script) {
        assert namespace
        checkNamespaceCollision(namespace, script)
        imports.put(namespace, script)
    }

    protected void checkNamespaceCollision(String namespace, ScriptMeta includedScript ) {

        if( namespace != ROOT_NAMESPACE ) {
            if( imports.containsKey(namespace) ) {
                throw new DuplicateScriptDefinitionException("A module with name '$namespace' was already imported")
            }
            return
        }

        // check imports name in the root namespace
        for( def item : includedScript.getDefinitions() ) {
            if( getInvokable(item.name) ) {
                def message = "Required module contains a ${item.type} with name '${item.name}' already defined in the root namespace -- check script: ${includedScript.scriptPath}"
                throw new DuplicateScriptDefinitionException(message)
            }
        }
    }

}
