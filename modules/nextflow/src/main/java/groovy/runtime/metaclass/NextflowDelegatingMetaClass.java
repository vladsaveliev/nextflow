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

package groovy.runtime.metaclass;

import java.io.File;
import java.nio.file.Path;

import groovy.lang.MetaClass;
import groovyx.gpars.dataflow.DataflowReadChannel;
import groovyx.gpars.dataflow.DataflowWriteChannel;
import nextflow.extension.DataflowEx;
import nextflow.file.FileHelper;

/**
 * Provides the "dynamic" splitter methods and {@code isEmpty} method for {@link File} and {@link Path} classes.
 *
 * See http://groovy.codehaus.org/Using+the+Delegating+Meta+Class
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class NextflowDelegatingMetaClass extends groovy.lang.DelegatingMetaClass {

    public NextflowDelegatingMetaClass(MetaClass delegate) {
        super(delegate);
    }

    private final DataflowEx dataflowExt;

    {
        dataflowExt = new DataflowEx();
    }

    @Override
    public Object invokeMethod(Object obj, String methodName, Object[] args)
    {
        int len = args != null ? args.length : 0;

        /*
         * Implements the 'isEmpty' method for {@link Path} and {@link File} objects.
         * It cannot implemented as a extension method since Path has a private 'isEmpty' method
         *
         * See http://jira.codehaus.org/browse/GROOVY-6363
         */
        if( "isEmpty".equals(methodName) && len==0 ) {
            if( obj instanceof File)
                return FileHelper.empty((File)obj);
            if( obj instanceof Path )
                return FileHelper.empty((Path)obj);
        }
        else if( len>0 && isInto(methodName, obj, args) ) {
            DataflowWriteChannel[] target = new DataflowWriteChannel[len];
            System.arraycopy(args,0,target,0,len);
            dataflowExt.into((DataflowReadChannel) obj, target);
            return null;
        }
        else if( dataflowExt.isExtension(obj,methodName) ) {
            return dataflowExt.invokeMethod(obj, methodName, args);
        }

        return delegate.invokeMethod(obj, methodName, args);

    }


    private static boolean isInto( String name, Object source, Object[] args ) {
        if( !"into".equals(name)) return false;

        if( !(source instanceof DataflowReadChannel) ) return false;

        for( int i=0; i<args.length; i++ )
            if( !(args[i] instanceof DataflowWriteChannel )) return false;

        return true;
    }


}
