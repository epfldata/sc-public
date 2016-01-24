Vector Tutorial
===============

As we discussed in the development process, there are two main phases for defining an EDSL in Scala. First, we should define an interpreter for the DSL in Scala. Second, a compiler should be defined for this DSL in Scala. The compiler definition involves defining IR nodes, optimizations and code generation for the given DSL.

## Vector Interpreter (Step 1)

Defining an interpreter does not require Pardis compiler. In other words, its definition can be written standalone without any 
dependancy on SC. However, for overriding language constructs of Scala, we propose using Yin-Yang. This interpreter is defined as a
project named as `vector-interpreter`. As we do not override any language feature of Scala, this project is not dependent on Yin-Yang.

The `Vector` data structure is implemented by defining the [corresponding class](step-1/vector-interpreter/src/main/scala/ch/epfl/data/vector/shallow/Vector.scala) for it. Different operations on this data-structure are defined as methods of this class. [`Step1`](step-1) shows how to define `vector-interpreter` and the `Vector` class.

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

For converting this interpreter to a compiler, we use the Purgatory compiler plugin to automatically generate the boilerplate code
related to the compilation process. For using this compiler plugin, an sbt plugin is provided by SC. After adding `generatorSettings` to the settings of this project, 3 parameters should be configured for the Purgatory compiler plugin:
 * `outputFolder`: The common parent folder of generated deep classes
 * `inputPackage`: The common parent package of shallow classes
 * `outputPackage`: The common parent package of generated deep classes
By running `embed` command in the sbt console of the `vector-interpreter` project, the deep classes are generated.
The classes that we are interested to generate their corresponding deep classes should be annotated with `@deep` annotation. These classes will be placed in the corresponding DSL compiler project.

The project `vector-compiler` is the compiler project for this Vector DSL. `DeepVector` file represents the deep embedding interface defined for the shallow class `Vector`. In order to use the deep interfaces, one should combine all deep interfaces which are needed for applications. This task is done in `VectorDSL` trait, which combines `VectorComponent`, which is the deep embedding interface for the `Vector` class, with the `ScalaPredefOps` interface which represents the deep embedding interface of the methods defined in the `Predef` module of Scala, such as `println`. 

The core part of the compiler is the `VectorCompiler` class. This class receives an input and applies optimizations and finally peforms code generation. By adding a transformation to the pipeline, we can specify different optimizations which should be applied. Furthermore, for the compiler we should specify a code generator which is defined in `VectorScalaGenerator` class.

[`Step2`](step-2) shows how to define `vector-compiler` and how to annotate the `Vector` class.

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

The project `vector-application` is the project which uses both projects `vector-interpreter` and `vector-compiler` in order to either interpret or compile the input applications. For interpreting the program, the program uses the Vector library in a normal manner. This way the program produces the result of computation whenever it is executed. For compiling the program, we use the polymorphic embedding approach to lift the program. However, as we need operations over `Seq` and `Int`, the `VectorDSL` should mix-in the corresponding deep interfaces (`IntOps` and `SeqOps`). Then, we pass the lifted program to the `compile` method of `VectorCompiler`. This will generate a Scala program out of this program in the `generator-out` folder. 

`Example1` file shows the interpretation under the name `Example1Shallow` and the compilation under the name `Example1Deep`. [`Step3`](step-3) shows how we implemented this example.

An alternative for compiling a program would be to write the program as we write in the shallow interface, and use Yin-Yang to lift it to the corresponding deep program. The shallow program is written inside the `dsl` macro. This macro uses Yin-Yang in order to convert the block inside it into the corresponding block in the deep embedding interface. This process is done in [`Step4`](step-4). 

## Vector Optimization (Steps 5 & 6)

There are two approaches for defining optimizations. 1) Online transformations: which are using smart constructors in order to check if some rewrite can be applied or not. This transformation is applied while the IR nodes of a program are being reified. 2) Offline transformations: which examine the application of different rewrite rules whenever the IR nodes are already reified. This class of transformations are chained using the `pipeline` construct defined in the `VectorCompiler` class.

In the file `VectorOpt` we have defined a domain-specific optimization for addition of two vectors using online transformation. Whenever a vector addition node is reified, we check if one of two vectors is zero. In such a case, there is no more need to create an addition node and returning the non-zero node is sufficient. To combine the optimized interfaces, we define `VectorDSLOpt` which combines all optimization interfaces.

For applying these optimizations for input applications, there are two ways. 1) Using the polymorphic embedding trait `VectorDSLOpt` and compile the lifted program. 2) Use the `dslOpt` macro defined using Yin-Yang. [`Step5`](step-5) defines `Example2` which adds a zero vector with another vector.

After generating the code for one of the applications in which the mentioned optimization is applicable, we found that although the zero vector is not used, the code for creating it still exists. The reason is that the dead code elimination has not been applied to this program. For applying this optimization we add the following line in `VectorCompiler` class:
`pipeline += DCE`

Ever after adding this optimization still the statement for creating a zero vector is not removed. This is because the compiler thinks that the creation of a zero vector is an effectful computation. In order to guide the pureness of this method, we use the `@pure` annotation on this method. After rerunning the `embed` command in `vector-interpreter` project, the generated program will no longer contain any zero vector. This is done in [`Step6`](step-6).

## Inlining and Lowering (Step 7)

In order to get rid of the Vector abstraction in the generated code, one has to *lower* the Vector abstraction. 
This is achieved by *inlining* the Vector operations to their corresponding implementation. Similar to other 
transformations, inlining can be performed in both online and offline manner. 

The `@onlineInliner` is for generating an online transformation trait called `VectorImplementations`. By mixing in 
this trait, every Vector operation is inlined to its corresponding implementation. Although this transformation
removes one level of indirection, it causes missing the opportunities for the high-level Vector optimizations.

The `@transformation` generates an offline transformation for inlining the Vector operations. In the compilation 
pipeline this transformation should be placed in an appropriate place. This is done by adding the following line in the
`VectorCompiler` class:
`pipeline += new VectorTransformation(DSL)`

Furthermore, as the operations on the lower level data structures are needed, one has to import these data structures.
`@needs` is the annotation for specifying the list of types that the current data structure is dependent on.
Further optimizations can be added to the compilation pipeline in the `VectorCompiler` class. All these tasks
are done in [`Step7`](step-7).
