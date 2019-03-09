.. _modules-page:

*******
Modules
*******

.. warning:: THIS IS AN EXPERIMENT FEATURE UNDER DEVELOPMENT. SYNTAX MAY CHANGE IN FUTURE RELEASE.


Nextflow modules features allows the definition of process and wub-workflow components that
can be re-used in the workflow script or across script libraries.

To enable this feature you need to defined the following directive at the beginning of
your workflow script::

    nextflow.enable.modules = true


Function
========

Nextflow allows the definition of custom function in the workflow script using the following syntax::

    def <function name> ( arg1, arg, .. ) {
        <function body>
    }

For example::

    def foo() {
        'Hello world'
    }

    def bar(alpha, omega) {
        alpha + omega
    }


The above snippet defines two trivial functions, that can be invoked in the workflow script as `foo()` which
returns the ``Hello world`` string and ``bar(10,20)`` which return the sum of two parameters.

.. tip:: Functions implicitly return the result of the function last evaluated statement.

The keyword ``return`` can be used to explicitly exit from a function returning the specified value.
for example::

    def fib( x ) {
        if( x <= 1 )
            return x
        else
            fib(x-1) + fib(x-2)
    }

Process
=======

Process definition
------------------
Once the `module` feature is enabled `process` components can be defined following the usual
for :ref:`process-page` definition expect you will need to omit that definition of the ``from`` and ``into``
channel declarations.

Then processes can be invoked as a function in the ``workflow`` scope, passing as parameter the expected
input channel.

For example::

    nextflow.enable.modules = true

    process FOO {
        output:
          file 'foo.txt'
        script:
          """
          your_command > foo.txt
          """
    }

     process BAR {
        input:
          file x
        output:
          file 'bar.txt'
        script:
          """
          your_command > bar.txt
          """
    }

    workflow {
        data = Channel.fromPath('/some/path/*.txt')
        FOO()
        BAR(data)
    }


Process invocation
------------------


Process composition
-------------------

Processes having matching input-output declaration can be composed so that the output
of the first process is passed as input to the following process. Take in consideration
the previous process definition, it's possible to write the following::

    workflow {
        BAR(FOO())
    }

Process outputs
---------------

A process output can also be accessed using the ``output`` attribute for the respective
process object. For example::

    workflow {
        FOO()
        BAR( FOO.output )
        BAR.output.println()
    }


When a process defines two or more output channels, each of them can be accessed
using the array element operator e.g. ``output[0]``, etc or using the `first`, ``second``, etc
sub-properties e.g. ``output.first``.

Workflow
========

Workflow definition
--------------------

The ``workflow`` keyword allows the definition of sub-workflow components that enclose the
invocation of two or more processes or operators. For example::

    workflow MY_PIPELINE {
        FOO()
        BAR( FOO.output.collect() )
    }


Once defined it can be invoked from another (sub) workflow component definition.

Workflow parameters
-------------------

A workflow component can be define one or more parameter in a similar manner as for a function
definition. For example::

        workflow MY_PIPELINE( data )  {
            FOO()
            BAR( data.mix(FOO.output) )
        }

The result channel of the last evaluated process is implicitly returned as the workflow output.


Main workflow
-------------

A workflow definition which does not define any name is assumed to be the main workflow and it's
implicitly executed. Therefore it's the entry point of the workflow application. 

Library
=======

Library scripts allows the definition workflow components that
can be included and shared across workflow applications.

A library script can contain the definition of functions, processes and workflows definition
as described above.

Library include
---------------

The library script can be imported from another Nextflow script using the ``include`` statement.
This allows the reference of the functions, processes and workflows defined in the library from
the importing script. 

For example::

    nextflow.enable.modules = true

    include 'modules/libx'

    workflow {
        data = Channel.fromPath('/some/data/*.txt')
        libx.MY_PIPELINE(data)
    }

Nextflow implicitly looks for the library script ``modules/libx.nf`` resolving the path
against the main script location.

Library namespace
-----------------

The library inclusion implicitly defines a *namespace* with the name name as the library script
e.g. ``libx`` in the previous example. Library components (i.e. functions, processes and workflows)
can be accessed by prefixing them with the library prefix e.g. ``libx.TASK_FOO``.

A different namespace can be specified using the ``as`` keyword. For example::

    nextflow.enable.modules = true

    include 'modules/libx' as helpers

    workflow {
        data = Channel.fromPath('/some/data/*.txt')
        helpers.MY_PIPELINE(data)
    }

The special ``_`` namespace can be used to import library components in the script default
namespace. For example::


    nextflow.enable.modules = true

    include 'modules/libx' as _

    workflow {
        data = Channel.fromPath('/some/data/*.txt')
        MY_PIPELINE(data)
    }


Library parameters
------------------

A library script can define script parameters as any other Nextflow script.

::

    params.foo = 'hello'
    params.bar = 'world'

    def sayHello() {
        "$params.foo $params.bar"
    }


Then, parameters can be specified when the library is imported with the ``include`` statement::


    nextflow.enable.modules = true

    include 'modules/library.nf' params(foo: 'Hola', bar: 'mundo')


Pipes
=====

Nextflow processes and operators can be composed using the ``|`` *pipe* operator. For example::

      process FOO {
          input: val data
          output: val result
          exec:
            result = "$data mundo"
      }

      workflow {
        data = Channel.from('Hello','world')
      
        data | FOO
      }


The above snippet defines a process named ``FOO`` then invoke it passing the content of the
``data`` channel.

The ``&`` *and* operator allow to feed two or more processes with the content of the same
channel e.g.::

    process FOO {
      input: val data
      output: val result
      exec:
        result = "$data mundo"
    }

    process BAR {
        input: val data
        output: val result
        exec:
          result = data.toUpperCase()
    }


    workflow {
        Channel.from('Hello') | map { it.reverse() } | (FOO & BAR)
    }
