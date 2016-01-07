List Tutorial
===============

An introduction to Embedded Domain-Specific Language (EDSL) developement can be found on the [Development process](../doc/DevProcess.md) page.
Here, we give an example of that methodology, with a simple library for manipulating lists (dubbed `list-dsl`).

## Step 1: Defining the Library

The EDSL is first defined as a standard Scala library, that we will place in package `list.shallow`. It provides a `List` data type, and ways to apply common manipulations on it (mainly mapping, filtering and folding).


```scala
class List[A](val data: Seq[A]) {
  
  def map[B](f: A => B): List[B] =
    new List(data map f)
  
  def filter(f: A => Boolean): List[A] =
    new List(data filter f)
  
  def fold[B](init: B)(f: (B,A) => B): B =
    (init /: data)(f)
  
  def size: Int = fold(0)((s, _) => s + 1)
  
  override def toString: String = s"List(${data mkString ", "})"
}
object List {
  def apply[A](data: A*): List[A] = new List[A](data.toSeq)
  def zip[A,B](as: List[A], bs: List[B]) = new List(as.data zip bs.data)
  
  val empty = List()
}
```

See the full file [here](src/main/scala/list/shallow/List.scala).

The EDSL can already be used like a normal Scala library.
It can be tested by typing `sbt console`, and importing `list.shallow._`, as in the following sbt session example:
```scala
sc-examples/list-dsl$ sbt console
[info] ...
Welcome to Scala version 2.11.7 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_65).

scala> import list.shallow._
import list.shallow._

scala> List(1,2,3) map (_ + 1) map (_.toDouble)
res0: list.shallow.List[Double] = List(2.0, 3.0, 4.0)
```

One can also create a Scala application that uses it, by creating a file containing:

```scala
package list.shallow

object Main extends App {
  val ls = List(1,2,3) map (_ + 1) map (_.toDouble)
  println(ls)
}
```

Such an application can be run by typing `sbt run`.

Now, we'd like to have more power and be able to optimize/transform programs written in the DSL,
as well as compile it to different target languages.
These require _deep embedding_.


## Step 2: Generating the Deep Embedding

### Specifying the Generation

SC provides a way to deeply embed our shallow DSL _automatically_, via an SBT plugin called Purgatory. We just need to add a few annotations to the library, and Purgatory will handle the rest:

```scala
@deep
@needs[(_,_) :: Seq[_]]
class List[A](val data: Seq[A]) {
  ...
```

Here, the `@deep` annotation tells Purgatory that we would like deep embedding of this class both for expression (_Exp_, the normal deep embedding) and extraction (_Ext_, used for pattern-matching with quasiquotes).
In case you do not want to generate the _Ext_ or _Exp_ part, add the `@noDeepExt`/`@noDeepExp` annotation.

`@needs` specifies which DSL features our DSL requires. Since our library uses 2-element tuples and they are not present in the default `Base` DSL embedded with SC, we need to add this requirement. If we omitted it, the generated code would not be able to compile.  
Note that `(_,_)` is just syntax sugar for `Tuple2[_,_]`, and that a special `::` type, defined in `pardis.annotations`, is used to separate the types passed to `@needs`.

<!-- Finally, `@noImplementation` tells Purgatory that for now we only need it to generate nodes for the definitions of our library/DSL, but not for their implementations. -->

Now, we can also add effect annotations to help SC reason about transformations (e.g., what code can be reordered).
In their current state, effect annotations are relatively coarse-grained:
`@pure` indicates that the metod will only read its arguments (excluding `this`) and will not modify any non-local state;
`@read` is used to indicate the method may read from the `this` parameter, and `@global` that it may read globally-defined variables;
when applied to a method, `@write` means that the function may write to `this`, and when applied to a parameter, that it may write to that parameters .

The following shows how we annotated one method of our `List` DSL:
```scala
@pure
def map[B](f: A => B): List[B] = ...
```

**Remark**: From a theoretical point of view, `map` is only conditionally pure, i.e., it is only pure if the function argument `f` it is passed is also pure.
If one knows, however, that no impure functions may be passed to `map` in the context of one's DSL, one may still mark it `@pure`. A more elaborate effect system will be introduced in a future version of SC (cf. [sc/issues/144](https://github.com/epfldata/sc/issues/144)).


### Recommended SBT Configuration

In order to use Purgatory, we need to add it to the `plugins.sbt` file:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("ch.epfl.data" % "sc-purgatory-plugin" % "0.1")
```

We recommend that one create an SBT sub-project for the deep embedding of the library.
This is to prevent a wrongly-generated deep embedding from breaking compilation of the main project (which would prevent re-generating the deep embedding).

The important generation settings needed in the SBT configuration are the following:

```scala
generatorSettings ++ Seq(
    generatePlugins += "ch.epfl.data.sc.purgatory.generator.QuasiGenerator",
    pluginLibraries += "ch.epfl.data" % "sc-purgatory-quasi_2.11" % "0.1-SNAPSHOT",

    outputFolder := "list-deep/src/main/scala/list/deep",
    inputPackage := "list.shallow",
    outputPackage := "list.deep"
)
```

One should not forget the `generatePlugins` line to be able to use quasiquotation, and to specify a correct `outputFolder` (the folder for generated files), `inputPackage` and `outputPackage` (package of the shallow library and package for the deep embedding to be generated).

See [here](project/Build.scala) for the full build file of our example.

Now that SBT is set up, we can proceed to the actual code generation, by going to the main project root directory, and typing `sbt embed`.

In our example, a file `DeepList.scala` will be generated inside the folder `list-deep/src/main/scala/list/deep`.

### Simple Compilation

In order to use this deep embedding, we can still use the normal shallow syntax, but we have to do so inside of a quasiquotation block `dsl"..."`.
To use `dsl` quasiquotes, we need to extend the `QuasiAPI[ListDSLOpsPackaged, ListDSLExtOpsPackaged]` trait and import its content (alternatively, we can make the current package object extend it), where `ListDSLOpsPackaged` and `ListDSLExtOpsPackaged` correspond to the deep embeddings of our EDSL (the second one is the one used for extraction).

These are automatically generated by Purgatory after defining our DSL and its dependent constructs using the following trait:

```scala
@language
@deep
@quasiquotation
@needs[List[_] :: ScalaCore]
trait ListDSL
```

`ScalaCore` contains the deep embedding of the core part of the Scala standard library provided by SC. `@quasiquotation` specifies that we would like to use quasiquotation to construct and perform pattern matching on the IR nodes. `@language` specifies that the current trait specifies a (domain-specific) language in the 
deep embedding.

An example of quasiquote use to define a program follows:
```scala
implicit object Context extends ListDSLOpsPackaged // required by the `dsl` string interpolator

def pgrm = dsl"""  
  val ls = List(1, 2, 3)
  ls map (_ + 1)
"""
```

What the `dsl` macro does is typecheck the shallow expression that we pass it, and transform it to deep embedding nodes (_ListDSLOps_ if it is used in an expression, _ListDSLExtOps_ if it is used in a pattern).

**Note**: If you have trouble with a quasiquote, there is a page dedicated to [debugging quasiquotes](../doc/DebuggingQuasiquotes.md).

Finally, in order to generate a program from this deep embedding, we have to extend `pardis.compiler.Compiler[ListDSLOps]` and define a code generator for it. This is going in the opposite direction as the `dsl` macro, i.e., from deep embedding to shallow embedding.

```scala
object MyCompiler extends Compiler[ListDSLOps] {
  val DSL: ListDSLOps = Context
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = ...
}

// Compiling our program to proper Scala code:
MyCompiler.compile(pgrm, "src/main/scala/GeneratedApp")
```

(See file [MyCompiler.scala](list-deep/src/main/scala/list/compiler/MyCompiler.scala) for an example definition of `codeGenerator`.)





## Step 3: Defining Optimizations

### Difference Between Online and Offline Optimizations

There are two main kinds of optimizations: online and offline.
Online (or _local_) optimizations are applied continuously, as soon as a node in the deep embedding is created, while offline optimizations are only applied after constructing the full program in the deep embedding (and thus after online optimizations have been applied), in a predefined order.

SC allows both kinds to be defined, but offline optimizations are simpler to start with.
This is because they require no knowledge of the deep embedding, being able to be fully described using quasiquotes.
Therefore, we will focus on them in this tutorial.
For an explanation of online optimizations and an example, please refer to the appendix.




### The Compilation Pipeline

In order to define offline transformations, we need to add them to the pipeline of the `MyCompiler` object defined earlier.
In the following example, we extend `MyCompiler`'s pipeline with a transformer `MyTransformer`, which definition is detailed below.

```scala
import sc.pardis.optimization.RecursiveRuleBasedTransformer

object MyCompiler extends Compiler[ListDSLOps] {

  ...
  
  pipeline += new MyTransformer[ListDSLOps](DSL)
  
}
```

Offline analysis and rewrite rules can be written in the following general syntax:

```scala
class MyTransformer extends RuleBasedTransformer {
    analysis += rule {
      case dsl" .. to match .. " => // store gathered information
    }
    rewrite += rule {
      case dsl" .. to match .. " => dsl" .. to construct .."
    }
  } 
}
```


### A Simple Optimization

Following is an optimization that rewrites any application of `size` to lists of one elements with the literal `1`.
To do so, it matches program fragments of the form `List(x).size` and generates a program fragment consisting of `1`.

```scala
import sc.pardis.optimization.RecursiveRuleBasedTransformer

class MyTransformer(DSL: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](DSL) {  
  rewrite += rule {
    case dsl"List($x).size" => dsl"1"
  }
}
```

In the body of the transformation above, the extracted variable `x` has type `Rep[?A]`, where `?A` is a type generated automatically by the quasiquote engine, representing the (unknown) type of the constructed list's elements.
We could also specify the type explicitly, as in
`case dsl"List[Int]($x).size"`, in  which case `x` would have had type `Rep[Int]`.
(Note that we could also write `$_` instead of `$x`, because we never use `x`.)

**Caveat**: in its current implementation, quasiquotation will not check that the type of an extracted object matches the concrete type specified,
so extarctor `dsl"List[Int]($x)` could extract a `List[String]`, and the type of `x` (`Rep[Int]`) would be lying. (This will be corrected in the future, cf. [sc/issues/99](https://github.com/epfldata/sc/issues/99).)

Note that a manual approach can be used to define the transformation, ie: without using quasiquotes and by manipulating IR nodes directly.
However, this requires special knowledge about the compiler's internals, and is out of the scope of this tutorial.
Quasiquotes will be enough for most use cases.


### Generalizing the `size` Optimization

In order to generalize that `size` optimization to any list size, we can use the vararg extraction syntax of quasiquotes:
```scala
  import IR.Predef._
  rewrite += rule {  case dsl"List($xs*).size" => `  }
```

In the code above, `xs` has type `Seq[Rep[A]]`, because it will match any actual list of arguments passed to the `List` constructor.
Notice that `xs.size` has type `Int`, so it can be spliced into a quasiquote. Writing `unit(xs.size)` would have the same effect as `unit(xs.size)`:
the function `unit` (from `IR.Predef`) takes a plain `Int` object and returns a `Rep[Int]` (of underlying class `Constant[Int]`).
It is necessary to return a `Rep[Int]` and not an `Int`, since any transformation is expected to return a value of type `Rep[_]`.

In any case, lifting constants require the `TypeRep` type class; in our case, it expects an implicit value of type `TypeRep[Int]`.
This value is also imported from `IR.Predef`. Other related implicits can be found in `sc.pardis.types.PardisTypeImplicits`.

Finally, note that matching `case dsl"List($xs: _*).size"` is also possible, but has a different meaning.
It will extract nodes created by Scala syntax `f(x: _*)`, where `x` is any sequence, of size not necessarily known statically.
As a consequence, the type of `xs` will be `Rep[Seq[A]]` (and not `Seq[Rep[A]]`).


### Explicitly Polymorphic Transformations

It is often useful to be able to refer to the generic types found in a pattern-matched expression.
In addition, extraction quasiquotes will not always be able to generate existential types (like `?A` above), and may need help to typecheck the quasiquoted expressions.

For this purpose, SC provides a macro `newTypeParams` that generates types and implicits whose purpose is to be used for extraction and construction of expressions through quasiquotes.
It is used as follows:

```scala
val params = newTypeParams('A, 'B, 'C); import params._

rewrite += rule {
  case dsl"($ls: List[A]).map($f: A => B).map($g: B => C)" =>
    f : Rep[A => B]  // typechecks
    dsl"($ls).map(x => $g($f(x)))"
}
```

Notice how we don't need to specify the types again in the construction of the new expression.
Scala infers all types for us in this context, i.e., as if we had written `dsl"($ls: List[A]).map[C]((x: A) => $g($f(x)))"`.



**Caveat**: Because of limitations in what extractor macros can do (and because we need to propagate implicit type representations from the extraction to the construction of expressions),
the `newTypeParams` macro relies on some shared state (instantiated with its application). For this reason, one should never share type parameters generated through this macro across transformations that may apply concurrently. For example, in an extraction of the form `case dsl"XXX" => x match { case dsl"YYY" => ... }`, the extractor `YYY` should use type parameters distinct from the ones used in `XXX`.
This also means that recursive extractors should define their `newTypeParams` parameters locally (so each recursive invocation has a distinct state).








## Step 4: Defining Lowerings

### Definition

Program transformations fall into two categories:

 - **Optimizations**, described in the above section.

 - **Lowerings**, wherein a program representation is translated to a lower level of abstraction, using constructs not necessarily available in the source language.


Unlike optimizations, which are optional, each lowering leading to the target language must be applied exactly once.
Lowerings usually lower the program abstraction by ways of inlining, specialization and partial evaluation.


### Lowering `List` constructions

In our example, we would like to lower the high-level, immutable List data structure to a construct more amenable to performant compilation.
We chose to use mutable `ArrayBuffer`s, available in the default Scala constructs of SC, as the lower-level representation.

The first thing to do is to rewrite all constructions of `List[T]` to an appropriate construction of `ArrayBuffer[T]`.

```scala
rewrite += symRule {
  case dsl"List[A]($xs*)" =>
    val buffer = dsl"new ArrayBuffer[A](${unit(xs.size)})"
    for (x <- xs) dsl"$buffer append $x"
    buffer
}
```

In the code above, we construct a `new ArrayBuffer[A](s)`, where `s` is the size to reserve for the elements it will contain.
We then loop through the arguments `xs` and independently append them one by one to the buffer we just defined.

Every execution of `dsl"$buffer append $x"` creates corresponding IR nodes in the current block and return a `Rep[Unit]`,
that there is no need to do anything with.


### Lowering `List` operations

Lowering the operations on lists is done in a similar fashion, but with a catch:
we have to match patterns such as `dsl"($ls: List[A]).map($f: A => B)"`, but such a pattern will expose an `ls` object of type `List[A]`,
whereas we would like an `ArrayBuffer[A]`. Indeed, we know we have converted list construction nodes to array construction nodes,
and would like to leverage this information (we want to use the `append` method available in `ArrayBuffer`).

Here, we have to force SC to believe us and use an `asInstanceOf`.
An elegant way of doing it is to define an auxilliary extractor that does the dirty job once and for all, limiting the potential for errors:

```scala
object ArrFromLs {
  def unapply[T](x: Rep[List[T]]): Option[Rep[ArrayBuffer[T]]] = x match {
    case dsl"$ls: List[T]" => Some(ls.asInstanceOf[Rep[ArrayBuffer[T]]])
    case _ => None
  }
}
```


We can now write the `List.map` lowering as follows:

```scala
  case dsl"(${ArrFromLs(arr)}: List[A]).map($f: A => B)" =>
    dsl"""
      val r = new ArrayBuffer[B]($arr.size)
      for (x <- $arr) r append $f(x)
      r
    """
```


Notice that contrary to the previous case (`List` construction), here the loop is _inside_ the quasiquotation block,
because we do not know the size statically and wish to generate a loop with a statement inside,
and not to loop and generate single statements (which corresponds to loop unrolling).


### Keeping Types Consistent

In several places, SC has to keep track of what are the types of the program's subexpressions.
We have to tell it of the transformation we implemented, so typing information can be properly maintained,
and the generated code will be consistent.

This is done by overriding the `transformType` function to make it handle our type transformation:

```scala
override def transformType[T](implicit tp: TypeRep[T]): TypeRep[Any] = tp match {
  case lst: IR.ListType[t] => IR.ArrayBufferType[t](lst.typeA).asInstanceOf[TypeRep[Any]]
  case _ => super.transformType(tp)
}
```


**Note**: It will not always be the case that a type transformation be performed throughout a program.
Sometimes, the decision of which low-level construct to use to lower a higher-level one will be contextual.
In such cases, it is necessary to construct a mapping during the analysis phase of the transformation
that remembers to which type each symbol is converted to.
This is outside the scope of this tutorial.




## Step 5: Compilation to C

### Memory Management

#### Memory Management Nodes

In order to compile our DSL to C, we must take into account the fact that C does not have garbage collection,
and uses instead stack allocation and explicit dynamic allocation/deallocation using the `malloc`/`free` primitives.

Conversion of garbage-collected programs to explicit memory management is a hard problem in its most general form.
Here, we will only describe an ad-hoc technique to manage the memory of our `List` objects:
for each `List` creation, we will convert it to an array allocation, and a corresponding deallocation (free) at the end of the current block.

**Caveat**: This simplistic scheme is only sound as long as `List` objects do not escape the blocks they are defined in
(for example, by being returned).
The next logical step would be to perform escape analysis (which is an existing analysis in SC),
either to prevent escape or to use a more elaborate scheme handling escapes,
but this is out of the scope of this tutorial.

In order to represent allocation/deallocation in the DSL, we add an object `Mem` that provide the corresponding nodes:
```scala
@deep
@needs[ClassTag[_]]
class Mem
object Mem {
  def alloc[T](size: Int): Array[T] = ???
  def free[T](obj: Array[T]): Unit = ???
}
```

Notice that these methods have no shallow implementation,
because they are only used as an intermediate representation before generating C code.

One should not forget to extend the requirements of the main DSL trait to `@needs[List[_] :: Mem :: ScalaCore]`,
in order for the deep representation of `Mem` to be included in the deep embedding.

We split the corresponding lowering in two parts: one to lower our `ArrayBuffer`s to simple `Array`s,
and one to implement explicit memory manegement of the `Array`s.


#### Lowering to Array

The problem is to rewrite a sequence of the form `val a = new ArrayBuffer[A](n); a append x; a append y ...`
into something of the form `val a = new Array[A](n); var size = 0; a(size) = x; size += 1; a(size) = y; size += 1; ...`.
We can ensure this transformation is safe and semantics-preserving based on our domain-specific knowledge.

In order to remember which `size` variable is associated with each array, we maintain a mapping `arraySizes`
and update it as we encounter statements corresponding to array creations:
```scala
val arraySizes = mutable.Map[Rep[_], IR.Var[Int]]()

rewrite += statement {
  case sym -> (x @ dsl"new ArrayBuffer[A]($size)") => 
    val res = dsl"new Array[A]($size)"
    arraySizes(res) = newVar(unit(0))
    res
}
```

Now, we can define normal rewriting rules to leverage this information.
We use the `subst` mapping automatically defined and updated by `RecursiveRuleBasedTransformer`,
that keeps a mapping of the symbols transformed thus far:

```scala
rewrite += symRule {
  case dsl"($arr: ArrayBuffer[A]).size" =>
    dsl"${arraySizes(subst(arr))}"
    
  case dsl"($arr: ArrayBuffer[A]) append $e" =>
    val myarr = subst(arr).asInstanceOf[Rep[Array[A]]]
    val v = arraySizes(myarr)
    dsl"$myarr($v) = $e; $v = $v + 1"
}
```


#### Allocation

The main idea is to use a buffer `arrays` containing all arrays created within the current block,
and to deallocate them at the end of the block:

```scala
case class Arr[T](arr: Rep[Array[T]])(implicit val tp: TypeRep[T])
var arrays = mutable.ArrayBuffer[Arr[_]]()

rewrite += statement {
  case sym -> dsl"new Array[A]($size)" => 
    val e = dsl"Mem.alloc[A]($size)"
    arrays += Arr(e)
    e
}

def postProcessBlock[T](b: Block[T]): Unit = {
  arrays foreach {
      case a =>
        import a.tp
        dsl"Mem.free(${a.arr})"
    }
}
```

The reason for using an `Arr` case class is to easily capture and reuse implicit `TypeRep` evidences
extracted by `unapply` quasiquotes in one place and spliced back by `apply` quasiquotes in another place.


### Converting Scala nodes to C nodes

In addition to lowering `ArrayBuffer` to `Array` and constructing memory management nodes, there are still
language mismatches between Scala and C. For example, Scala `if` statements can return values, whereas
in C `if` statements can only have `Unit` (`void`) type (to be more precise, conditionals `cond ? thenp : elsep`
can return values other than `void` but cannot have proper blocks containing local variables in the branches). Furthermore, the data structures 
of Scala core library should also be transformed to their corresponding C version. 
To do so SC provides the `ScalaCoreToC` transformer, which should be added in the transformation pipeline. 


### Generating C Syntax

In order to stringify C code, we need to provide a C code generator in addition to the existing Scala code generator.
To do so, whenever the user wants to use the C code generator, an instance of `CASTCodeGenerator` should be used.
In order to stringify some core constructs of Scala core library to C code, SC already provides the `ScalaCoreCCodeGen` interface. In addition, we should guide the code generator how to generate C code for `Mem.alloc` and `Mem.free`. This is achieved by overriding the `functionNodeToDocument` in the C code generator object,
as shown in the following code:

```scala
override def functionNodeToDocument(fun: FunctionNode[_]) = fun match {
  case dsl"Mem.alloc[A]($size)" => {
    val tp = implicitly[TypeRep[A]]
    doc"($tp *)malloc($size * sizeof($tp))"
  }
  case dsl"Mem.free($mem)" => doc"free($mem)"
  case _ => super.functionNodeToDocument(fun)
}
```

Finally, by providing appropriate flags for switching between Scala code generation and C code generation, we
can have a modular compiler able to target the two languages.

## Annex: Online Optimizations

Online optimizations are achieved through "Smart Constructors": overriding deep embedding node constructors so as to change their behavior depending on the subnodes they are given.

### Using Quasiquotes

A typical example is a constructor that directly generates the literal `0` instead of a node for calling `size` on an empty list.

This can be defined easily by overriding the `listSize` method found in `ListOps`, and giving it this body:

```scala
override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
    case dsl"shallow.List()" => dsl"0"
    case _ => super.listSize(self)
  }
```

The example above is similar to the one we described as an offline transformation, but applies to lists of size `1` instead.

Notice that we use a `shallow.` prefix in front of `List` in order not to capture the `List` object defined in `ListOps`, bur the actual shallow-embedding `List` object.

The overriding method should be defined in a trait (named as `ListExpOptimizations`) which extends the 
`ListOps` trait. In order to benefit from this online transformation, instead of using the `ListDSLOps` 
polymorphic embedding interface, one should use another interface which extends both `ListDSLOps` and `ListExpOptimizations`.

### Manually

Note that the transformation above is essentially equivalent to the following, manual definition:

```scala
   override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
     case Def(ListApplyObject(Def(PardisLiftedSeq(Seq())))) => unit(0)
     case _ => super.listSize(self)
   }
```

As one can see, it is easier to use quasiquotes to match deep embedding nodes, because they hide implementation details of the Intermediate Representation by providing the same language as the shallow library.
