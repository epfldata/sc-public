Vector Tutorial
===============

As we discussed in development process, there are two main phases for defining an EDSL in Scala. First, we should define an interpreter for the DSL in Scala. Second, a compiler should be defined for this DSL in Scala. The compiler definition involves defining IR nodes, optimizations and code generation for the given DSL.

## Vector Interpreter (Step 1)

Defining an interpreter does not require Pardis compiler. In other words, its definition can be written standalone. However, for overriding language constructs of Scala, we propose using Yin-Yang. This interpreter is defined as a project named as `vector-interpreter`. As we do not override any language feature of Scala, this project is not dependent on Yin-Yang.

The `Vector` data-structure is implemented by defining the [corresponding class](https://github.com/epfldata/sc-examples/blob/master/vector-interpreter/src/main/scala/ch/epfl/data/vector/shallow/Vector.scala) for it. Different operations on this data-structure are defined as methods of this class. [`Step1`](https://github.com/epfldata/sc-examples/tree/Step1) shows how to define `vector-interpreter` and the `Vector` class.

You can try out the `Vector` data structure in the Scala REPL as follows. Start sbt, enter `project vector-interpreter`, and then run `console`. Then enter, e.g.,
```scala
scala> val v = ch.epfl.data.vector.shallow.Vector(Seq(1,3,4))
v: ch.epfl.data.vector.shallow.Vector = Vector(1, 3, 4)

scala> v+v
res0: ch.epfl.data.vector.shallow.Vector = Vector(2, 6, 8)

scala> v*v
res1: Int = 26
```

## Vector Compiler (Step 2)

For converting this interpreter to a compiler, we should use the Purgatory compiler plugin. For using this compiler plugin, the corresponding sbt plugin is used. After adding `generatorSettings` to the settings of this project, 3 parameters should be configured for Purgatory compiler plugin:
 * `outputFolder`: The common parent folder of generated deep classes
 * `inputPackage`: The common parent package of shallow classes
 * `outputPackage`: The common parent package of generated deep classes
By running `embed` command in the sbt console of the `vector-interpreter` project, the deep classes are generated.
The classes that we are interested to generate their corresponding deep classes should be annotated with `@deep` annotation. These classes will be put to the corresponding DSL compiler project.

The project `vector-compiler` is the compiler project for this Vector DSL. `DeepVector` file represents the deep embedding interface defined for the shallow class `Vector`. In order to use the deep interfaces, one should combine all deep interfaces which are needed for applications. This task is done in `VectorDSL` trait, which combines `VectorComponent`, which is the deep embedding interface for the `Vector` class, with the `ScalaPredef` interface which represents the deep embedding interface of the methods defined in the `Predef` module of Scala, such as `println`. 

The core part of the compiler which receives an input and applies optimizations and finally generates codes is `VectorCompiler` class. By adding a transformation to the pipeline, we can specify different optimizations which should be applied. Furthermore, for the compiler we should specify a code generator which is defined in `VectorScalaGenerator` class.

[`Step2`](https://github.com/epfldata/sc-examples/tree/Step2) shows how to define `vector-compiler` and how to annotate the `Vector` class. `@noImplementation` specifies that we do not want to lift the implementation of the methods and it suffices to lift only the signature of the methods.

You can run the compiler from the console as follows. Start sbt and enter `project vector-compiler` and then `console`. Then copy-paste the following code.
```scala
import ch.epfl.data.sc.pardis.types.PardisTypeImplicits._

val context = new ch.epfl.data.vector.deep.VectorDSL {
  implicit def liftInt(i: Int): Rep[Int] = unit(i)
  def prog = {
    val v1 = Vector(Seq(1, 2, 3))
    val v2 = Vector(Seq(2, 3, 4))
    println(v1 + v2)
  }
}

new ch.epfl.data.vector.compiler.VectorCompiler(context).compile(
  context.prog, "GeneratedVectorApp")
   ```
The output file is written to the `generator-out` directory.

## Vector Application (Steps 3 & 4)

The project `vector-application` is the project which uses both projects `vector-interpreter` and `vector-compiler` in order to either interpret or compile the input applications. For interpreting the program, the program uses the Vector library in a normal manner. This the program produces the result of computation whenever it is executed. For compiling the program, we use the polymorphic embedding approach to lift the program. However, as we need operations over `Seq` and `Int`, the `VectorDSL` should mix-in the corresponding deep interfaces (`IntOps` and `SeqOps`). Then, we invoke the `compile` method of `VectorCompiler` over the lifted program. This will generate a Scala program out of this program in the `generator-out` folder. 

`Example1` file shows the interpretation under the name `Example1Shallow` and the compilation under the name `Example1Deep`. [`Step3`](https://github.com/epfldata/sc-examples/tree/Step3) shows how we implemented this example.

An alternative for compiling a program would be to write the program as we write in the shallow interface, and use Yin-Yang to lift it to the corresponding deep program. The shallow program is written inside the `dsl` macro. This macro uses Yin-Yang in order to convert the block inside it into the corresponding block in the deep embedding interface. This process is done in [`Step4`](https://github.com/epfldata/sc-examples/tree/Step4). 

## Vector Optimization (Steps 5 & 6)

There are two approaches for defining optimizations. 1) Online transformations: which are using smart constructors in order to check if some rewrite can be applied or not. This transformation is applied while the IR nodes of a program are being reified. 2) Offline transformations: which examine the application of different rewrite rules whenever the IR nodes are already reified. This class of transformations are chained using `pipeline` construct defined in `Compiler` classes (in the case of Vector DSL, `VectorCompiler` class).

In the file `VectorOpt` we have defined a domain-specific optimization for addition of two vectors using online transformation. Whenever a vector addition node is reified, we check if one of two vectors is zero, there is no more need to create an addition node. It is enough to return the non-zero node. To combine the optimized interfaces, we define `VectorDSLOpt` which combines all optimization interfaces.

For applying this optimizations for input applications, there are again two ways. 1) Using polymorphic embedding on trait `VectorDSLOpt` and compile the lifted program. 2) Use `dslOpt` macro defined using Yin-Yang. We define `Example2` which adds a zero vector with another vector. This is done in [`Step5`](https://github.com/epfldata/sc-examples/tree/Step5).

After generating the code for one of the applications in which the mentioned optimization is applicable, we found that although the zero vector is not used, the code for creating it still exists. The reason is that the dead code elimination has not been applied to this program. For applying this optimization we add the following line in `VectorCompiler` class:
`pipeline += DCE`

Ever after adding this optimization still the zero vector statement is not removed. The reason is that the compiler thinks that the creation of a zero vector is an effectful computation. In order to guide the pureness of this method, we use `@pure` annotation on this method. After rerunning the `embed` command in `vector-interpreter` project, the generated program will no longer contain any zero vector. This is done in [`Step6`](https://github.com/epfldata/sc-examples/tree/Step6).

_TO BE CONTINUED_
