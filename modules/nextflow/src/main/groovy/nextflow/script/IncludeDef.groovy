package nextflow.script

import java.nio.file.NoSuchFileException
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import nextflow.Session
import nextflow.exception.ProcessException
/**
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
@EqualsAndHashCode
class IncludeDef {

    @PackageScope path
    @PackageScope String alias
    @PackageScope String name
    @PackageScope Map params = new LinkedHashMap(10)
    private Session session
    private Path ownerScript
    private Binding binding

    IncludeDef( String module ) {
        this.path = module
    }

    IncludeDef(TokenVar name, String alias=null ) {
        this.name = name.name
        this.alias = alias
    }

    protected IncludeDef() {}

    IncludeDef from(Object path) {
        this.path = path
        return this
    }

    IncludeDef params(Map args) {
        this.params.putAll(args)
        return this
    }

    IncludeDef setSession(Session session) {
        this.session = session
        return this
    }

    IncludeDef setOwnerScript(Path script) {
        this.ownerScript = script
        return this
    }

    IncludeDef setBinding(Binding binding) {
        this.binding = binding
        return this
    }

    void load() {
        if( !path )
            throw new IllegalArgumentException("Missing module path attribute")

        // -- resolve the concrete against the current script
        final moduleFile = realModulePath(path)
        // -- load the module
        def moduleScript = loadModule0(moduleFile, params, session)
        // -- add it to the inclusions
        ScriptMeta.current().addModule(moduleScript, name, alias)
    }


    @PackageScope
    BaseScript loadModule0(Path path, Map params, Session session) {
        try {
            final binding = new ScriptBinding() .setParams(params)

            // the execution of a library file has as side effect the registration of declared processes
            new ScriptParser(session)
                    .setModule(true)
                    .setBinding(binding)
                    .runScript(path)
                    .getScript()
        }
        catch( ProcessException e ) {
            throw e
        }
        catch( Exception e ) {
            throw new IllegalArgumentException("Unable to load module file: $path -- cause: ${e.cause?.message ?: e.message}", e)
        }
    }

    @PackageScope
    Path resolveModulePath(include) {
        assert include

        final result = include as Path
        if( result.isAbsolute() ) {
            if( result.scheme == 'file' ) return result
            throw new IllegalArgumentException("Cannot resolve module path: ${result.toUriString()}")
        }

        return ownerScript.resolveSibling(include.toString())
    }

    @PackageScope
    Path realModulePath(include) {
        def module = resolveModulePath(include)

        // check if exists a file with `.nf` extension
        if( !module.name.contains('.') ) {
            def extendedName = module.resolveSibling( "${module.simpleName}.nf" )
            if( extendedName.exists() )
                return extendedName
        }

        // check the file exists
        if( module.exists() )
            return module

        throw new NoSuchFileException("Can't find a matching module file for include: $include")
    }

}
