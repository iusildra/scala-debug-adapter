# scala-debug-adapter

[![build-badge][]][build]
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-debug-adapter/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-debug-adapter)

[build]:       https://github.com/scalacenter/scala-debug-adapter/actions?query=branch%3Amain+workflow%3A%22Continuous+Integration%22
[build-badge]: https://github.com/scalacenter/scala-debug-adapter/workflows/Continuous%20Integration/badge.svg?branch=main

The scala-debug-adapter is a server-side implementation of the [Debug Adapter Protocol](https://microsoft.github.io/debug-adapter-protocol/) for the Scala language running on the JVM platform.
It is based on and extends the [microsoft/java-debug](https://github.com/microsoft/java-debug) implementation.

The project originated in the [Bloop](https://github.com/scalacenter/bloop) repository, it is now released independently so that it can be used in other build tools of the Scala ecosystem.
For instance, the [sbt-debug-adapter](#sbt-debug-adapter plugin) is an sbt plugin that provides sbt with the debug adapter capability.

## Usage

You can add the `scala-debug-adapter` as a dependency in your `build.sbt`:

```scala
// build.sbt
scalaVersion := "2.12.16",
libraryDependencies += "ch.epfl.scala" %% "scala-debug-adapter" % "3.0.5"
```

The `scala-debug-adapter` expects the Java Debug Interface to be class loaded.
While it is always the case with Java 9, you probably want to use the `sbt-jdi-tools` to be able to run on Java 8 as well:

```scala
// project/plugins.sbt
addSbtPlugin("org.scala-debugger" % "sbt-jdi-tools" % "1.1.1")
```

You can start a debug server by providing your own instance of `Debuggee`, `DebugToolsResolver` and `Logger`.

### The `Debuggee`

The `Debuggee` is a trait that describes the program that we want to debug.

It contains the list of modules of the current project, the list of libraries, the list of unmanaged entries and the java runtime.

There are a few difference between a module, a library and an unmanaged entry:

- `Module`: We know its exact scala version, and its compiler options.
They will be used by the debugger to evaluate expressions in the module.
- `Library`: We only know its binary version.
We can try to evaluate expression but it can fail if the compilation of the library involved some compiler options or plugins.
- `UnmanagedEntry`: We have the class files, or class jar, but not the source files, or source jar.
We won't be able to debug inside those classpath entries.
We still need them to build the full classpath.

The `Debuggee` trait also contains a `run` method that is called by the debugger to start the program.

The sbt version of `Debuggee` can be found [here](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/sbt-plugin/src/main/scala/ch/epfl/scala/debugadapter/sbtplugin/internal/SbtDebuggee.scala). There also is a Bloop version of `Debuggee` in [scalacenter/bloop](https://github.com/scalacenter/bloop).

### The `DebugToolsResolver`

Depending on the Scala versions specified in the `Debuggee`, the debugger will need to resolve some additional tools:

- The expression compiler: to compile the Scala expression that the user wants to evaluate
- The step filter (renamed to `unpickler`): to filter the intermediate steps of execution that are generated by the compiler, among which the mixin-forwarders, the bridges, the getters and setters, the synthetized methods.

The role of the `DebugToolsResolver` is to resolve the debug tools and their dependencies, and to load them in a class-loader.
The debugger takes care of reusing those class-loaders as often as possible: it should call the `DebugToolsResolver` only once by Scala version.

To implement the `DebugToolsResolver` you can use the Maven coordinates of the required tools in `ch.epfl.scala.debugadapter.BuildInfo`.

The sbt version of `DebugToolsResolver` can be found [here](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/sbt-plugin/src/main/scala/ch/epfl/scala/debugadapter/sbtplugin/internal/SbtDebugToolsResolver.scala). There also is a Bloop version of `DebugToolsResolver` that uses Coursier in [scalacenter/bloop](https://github.com/scalacenter/bloop).

### Starting the `DebugServer`

Given that you have an instance `debuggee` of `Debuggee`, an instance `resolver` of `DebugToolsResolver` and an instance `logger` of `Logger`, this is how you can wire things and start the debug server:

```scala
// find an available IP socket address
val address = new DebugServer.Address()

// resolve the debug tools needed by the debuggee
val tools = DebugTools(debuggee, resolver, logger)

// create and start the debug server
val server = DebugServer(debuggee, tools, logger, address)
server.start()

// return address.uri for the DAP client to connect
address.uri
```

## sbt-debug-adapter plugin

The `sbt-debug-adapter` is an sbt plugin compatible with sbt `1.4.0` or greater.
It provides the sbt server with the BSP `debugSession/start` endpoint to start a Scala DAP server.

The specification of the `debugSession/start` endpoint can be found in the [Bloop documentation](https://scalacenter.github.io/bloop/docs/debugging-reference).

### Plugin usage

As a global plugin use `~/.sbt/1.0/plugins/plugins.sbt`, otherwise add to your local project (e.g. `project/plugins.sbt`):

```scala
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-debug-adapter" % "1.0.0")
```

Again the JDI tools must be class loaded, this time by sbt itself.
To do so you can use the `sbt-jdi-tools` plugin in the meta project (it goes to `project/project/plugins.sbt`).

```scala
// project/project/plugins.sbt
addSbtPlugin("org.scala-debugger" % "sbt-jdi-tools" % "1.1.1")
```

## Development

### expression-compiler

The [`expression-compiler`](https://github.com/scalacenter/scala-debug-adapter/tree/main/modules/expression-compiler) is a module used to compile expression from the debug console. It will insert the expression in the source file, compile it, and return the result of the expression.

To do so, it adds 2 new phases to the Scala compiler:

- `extract-expression`: extract the expression from the source file in a new class from where we can evaluate it. To keep the context (local variables), we import them in the new class via a `Map`
- `resolve-reflect-eval`: use reflection to resolve some symbols of the expression

Debugging the `expression-compiler` requires to enable some logs to show the trees generated by the compiler and the expression compiler. To do so, you need to uncomment the following lines:

- `println(messageAndPos(...))` in `ExpressionReporter.scala`
  - For Scala 3.1+: [file](https://github.com/iusildra/scala-debug-adapter/blob/main/modules/expression-compiler/src/main/scala-3.1+/dotty/tools/dotc/ExpressionReporter.scala)
  - For Scala 3.0: [file](https://github.com/iusildra/scala-debug-adapter/blob/main/modules/expression-compiler/src/main/scala-3.0/dotty/tools/dotc/ExpressionReporter.scala)
- `// "-Vprint:typer,extract-expression,resolve-reflect-eval"` in [`ExpressionCompilerBridge.scala`](https://github.com/iusildra/scala-debug-adapter/blob/main/modules/expression-compiler/src/main/scala-3/dotty/tools/dotc/ExpressionCompilerBridge.scala)

### Runtime evaluator

#### Idea

The runtime evaluator is a fully reflection-based evaluation mode designed for simple expressions (without implicits, generics, etc.) written in the debug console. It is less precise than the expression compiler (that can evaluate any kind of expression), but it is much faster (since it does not need to compile). Also, because it is reflection-based, we can access runtime types and private members, which is not possible with the expression compiler. For instance:

```scala
class A { private val x = 64 }
class B extends A { val y = 42 }

val a: A = new A
val b: A = new B

// a.x returns 64
// b.y returns 42
```

#### Constraints

Because it is less precise than the expression compiler, it might fail while evaluating an expression (and placing the program in an invalid state), or evaluate it incorrectly (bad overload resolution for instance). To overcome this, a validation step is performed before any evaluation and an order of priority has been defined to choose between the expression compiler and the runtime evaluator:

![evaluation mode priorities](./doc/evaluation-provider.svg)

#### Implementation

The evaluator is implemented in a 3-steps process:

- parse the expression with [scalameta](https://scalameta.org/) to get the tree representing the expression
- validation: traverse the tree and recursively check that the expression and its sub-expressions can be evaluated at runtime (type-checking, polymorphism overloads resolution, type resolution, etc.). Transform the AST from scalameta to a new AST containing all the information needed for evaluation (see [RuntimeTree.scala](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/core/src/main/scala/ch/epfl/scala/debugadapter/internal/evaluator/RuntimeTree.scala))
- evaluation: consume the new AST and evaluate the expression

The validation AST is separated in 2 main parts:

- `RuntimeEvaluableTree`: these nodes are valid expressions by themselves (e.g. a literal, a method call, a module, etc.)
- `RuntimeValidationTree`: theses nodes are not a valid expression by themselves (e.g. a class name), but they can be contained within an evaluable node (e.g. static member access)

The validation is modeled with a [Validation](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/core/src/main/scala/ch/epfl/scala/debugadapter/internal/evaluator/Validation.scala) monad:

- `Valid`: the expression is valid
- `Recoverable`: the expression is not valid, but we might be able to recover from it (e.g. when validating `foo`, we might not find a local variable `foo`, but we might find a field, a method, etc. that we can use instead)
- `CompilerRecoverable` when information at runtime is not enough to validate the information (e.g. overloads ambiguity at runtime), the expression compiler might be able to validate it
- `Fatal` for errors that cannot be recovered (e.g. parsing error, unexpected exception, etc.). *Abort the whole evaluation process*

#### Pre evaluation

Pre-evaluation is a validation mode that first validate an expression, and evaluate it does not have side effects / fail (only a few nodes can be pre-evaluated). It is used to access the runtime type of an expression to get more information about it. ([RuntimePreEvaluationValidator.scala](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/core/src/main/scala/ch/epfl/scala/debugadapter/internal/evaluator/RuntimePreEvaluationValidator.scala))

### tests

The [`tests`](https://github.com/scalacenter/scala-debug-adapter/tree/main/modules/tests/src/test/scala/ch/epfl/scala/debugadapter) module contains the tests of the Scala Debug Server.
It uses the [TestingDebugClient](https://github.com/scalacenter/scala-debug-adapter/blob/main/modules/tests/src/main/scala/ch/epfl/scala/debugadapter/testfmk/TestingDebugClient.scala), a minimal debug client that is used to communicate with the debug server via a socket.

### Show logs

To print the logs during the tests, you must change the logger in `DebugTestSuite#getDebugServer` and `DebugTestSuite#startDebugServer` from `NoopLogger` to `PrintLogger`

## References

- [Bloop Debugging Reference](https://scalacenter.github.io/bloop/docs/debugging-reference)
- [Microsoft DAP for Java](https://github.com/microsoft/vscode-java-debug)
- [DebugAdapterProvider](https://github.com/build-server-protocol/build-server-protocol/issues/145)

## History

- [Original project discussion](https://github.com/scalameta/metals-feature-requests/issues/168)
