List Tutorial
===============

An introduction to EDSL developement can be found in the [Development process](../doc/DevProcess.md) page.
Here, we give an example of that methodology, with a simple library for manipulating lists (dubbed `list-dsl`).

## Step 1: Defining the Library

The EDSL is first defined as a standard Scala library, that we will place in package `list/shallow`. It provides a `List` data type, and ways to apply common manipulations on it (mainly mapping, filtering and folding).


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

See the full file [here](https://github.com/epfldata/sc-examples/blob/master/list-dsl/src/main/scala/list/shallow/List.scala).

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

One can also create an application using it, by adding a file with content similar to:

```scala
package list.shallow

object Main extends App {
  val ls = List(1,2,3) map (_ + 1) map (_.toDouble)
  println(ls)
}
```


Now, we'd like to have more power and be able to optimize/transform programs written in the DSL, as well as compile it to different targets. These require _deep embedding_.


## Step 2: Generating the Deep Embedding

### Specifying the Generation

SC provides a way to deeply embed our shallow EDSL _automatically_, via an SBT plugin called Purgatory. We just need to add a few annotations to the library, and Purgatory will handle the rest:

```scala
@deep
@needs[Numeric[_] :: (_,_) :: Seq[_]]
class List[A](val data: Seq[A]) {
  ...
```

Here, the `@deep` annotation tells Purgatory that we would like deep embedding of this class both for expression (_Exp_, the normal deep embedding) and extraction (_Ext_, used for pattern-matching with quasiquotes). In case you do not want to generate the _Ext_ or _Exp_ part, add the `@noDeepExt`/`@noDeepExp` annotation.

`@needs` specifies which DSL features our DSL requires. Since our library uses 2-element tuples and they are not present in the default `Base` DSL embedded with SC, we need to add this requirement. If we omitted it, the generated code would not be able to compile.  
Note that `(_,_)` is just syntax sugar for `Tuple2[_,_]`, and that a special `::` type, defined in `pardis.annotations`, is used to separate the types passed to `@needs`.

<!-- Finally, `@noImplementation` tells Purgatory that for now we only need it to generate nodes for the definitions of our library/DSL, but not for their implementations. -->

Now, we can also add effect annotations to help SC reason about transformations (e.g.: what code can be reordered).
In their current state, effect annotations are relatively coarse-grained:
`@pure` indicates that the metod will only read its arguments (excluding `this`) and will not modify any non-local state;
`@read` is used to indicate the method may read from the `this` parameter, and `global` that it may read globally-defined variables;
when applied to a method, `@write` means that the function may write to `this`, and when applied to a parameter, that it may write to that parameters .

The following shows how we annotated one method of our `List` DSL:
```scala
@pure
def map[B](f: A => B): List[B] = ...
```

**Remark**: From a theoretical point of view, `map` is only conditionally pure, i.e., it is only pure if the function argument `f` it is passed is also pure.
If you know, however, that no impure functions may be passed to `map` in the context of your DSL, you may still mark it `@pure`.


### Recommended SBT Configuration

In order to use Purgatory, you need to add it to your `plugins.sbt`:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("ch.epfl.data" % "sc-purgatory-plugin" % "0.1")
```

We recommend that you create an SBT sub-project for the deep embedding of the library. This is to prevent a wrongly-generated deep embedding from breaking compilation of the main project (which would prevent re-generating the deep embedding).

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

Don't forget the `generatePlugins` line to be able to use quasiquotation, and to specify correct `outputFolder` (the folder for generated files), `inputPackage` and `outputPackage` (package of the shallow library and package for the deep embedding to be generated).

See [here](https://github.com/epfldata/sc-examples/blob/master/list-dsl/project/Build.scala) for the full build file of our example.

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

What the `dsl` macro does is to typecheck the shallow expression that we pass it, and transform it to deep embedding (_ListDSLOps_ if it is used in an expression, _ListDSLExtOps_ if it is used in a pattern).

**Note**: If you have trouble with a quasiquote, there is a page dedicated to [debugging quasiquptes](https://github.com/epfldata/sc-examples/blob/master/doc/DebuggingQuasiquotes.md).

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

(See file [MyCompiler](https://github.com/epfldata/sc-examples/blob/master/list-dsl/list-deep/src/main/scala/list/compiler/MyCompiler.scala) for an example of definition for `codeGenerator`.)




## Step 3: Defining Optimizations

### Difference Between Online and Offline Optimizations

There are two main kinds of optimizations: online and offline.
Online (or _local_) optimizations are applied continuously, as soon as a node in the deep embedding is created, while offline optimizations are only applied after constructing the full program in the deep embedding (and thus after online optimizations have been applied), in a predefined order.

SC allows both kinds to be defined, but offline optimizations are significantly simpler as they require no knowledge of the deep embedding, and can be fully described using quasiquotes.
Therefore, we will focus on them in the rest of this tutorial.
For an explanation of online optimizations and an example, please refer to in the appendix.




### The Compilation Pipeline

In order to define offline transformations, we need to add them to the pipeline of the `MyCompiler` object defined earlier.
In the following example, we extend `MyCompiler`'s pipeline with a transformer `MyTransformer` which definition is detailed later on:

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
To do so, it matches program fragments of the form `List(x).size` (where `x` could be replaced by `_`, because we never use it), and generates a program fragment consisting of `1`:

```scala
import sc.pardis.optimization.RecursiveRuleBasedTransformer

class MyTransformer(DSL: ListDSLOps) extends RecursiveRuleBasedTransformer[ListDSLOps](DSL) {  
  rewrite += rule {
    case dsl"List($x).size" => dsl"1"
  }
}
```

In the body of the transformation above, the extracted variable `x` has type `Rep[?A]`, where `?A` is a type generated automatically by the quasiquote engine, representing the (unknown) type of the constructed list's elements. We could also specify the type explicitly, as in
`case dsl"List[Int]($x).size"`, in  which case `x` would have had type `Rep[Int]`.

**Caveat**: in its current implementation, quasiquotation will not check that the type of an extracted object matches the concrete type specified, so extarctor `dsl"List[Int]($x)` could extract a `List[String]`, and the type of `x` would be lying. (This will be corrected in the future.)

Note that a manual approach can be used to define the transformation, ie: without using quasiquotes and by manipulating IR nodes directly.
However, this requires special knowledge about the compiler's internal details, and is out of the scope of this tutorial.
Quasiquotes should be enough for most use cases.


### Generalizing the `size` Optimization

In order to generalize that `size` optimization to any list size, we can use the vararg extraction syntax of quasiquotes:
```scala
  import IR.Predef._
  rewrite += rule {  case dsl"List($xs*).size" => unit(xs.size)  }
```

In the code above, `xs` has type `Seq[Rep[A]]`, because it will match any actual list of arguments passed to the `List` constructor.
Notice the call to `unit` (from `IR.Predef`), that takes a plain `Int` object and returns a `Rep[Int]` (of underlying class `Constant[Int]`). It is necessary, since a transformation is expected to return a value of type `Rep[_]`.
This function expects an argument with the `TypeRep` type class; in our case, it expects an implicit value of type `TypeRep[Int]`. This value is also imported from `IR.Predef`. Other related implicits can be found in `sc.pardis.types.PardisTypeImplicits`.


### Explicitly Polymorphic Transformations

It is often useful to be able to refer to the (unknown) types found in a pattern-matched expression. In addition, quasiquotes will not always be able to generate existential types (like `?A` above), and may need help to typecheck the quasiquoted expressions.

For this purpose, SC provides a macro `newTypeParams` that generates types which purpose is to be used for extraction and construction of expressions through quasiquotes.
It is used as follows:

```scala
val params = newTypeParams('A, 'B, 'C); import params._
rewrite += rule {
  case dsl"($ls: List[A]).map($f: A => B).map($g: B => C)" =>
    f : Rep[A => B]  // typechecks
    dsl"($ls).map(x => $g($f(x)))"
}
```

Notice how we don't need to specify the types again in the construction of the new expression. Scala infers all types for us in this context, i.e. as if we had written
`dsl"($ls: List[A]).map[C]((x: A) => $g($f(x)))"`.



**Caveat**: Because of limitations in what extractor macros can do (and because we need to propagate implicit type representations from the extraction to the construction of expressions), the `newTypeParams` macro relies on some shared state (instantiated with its resulting object). For this reason, one should never share type parameters generated through this macro across transformations that may apply concurrently. For example, in an extraction of the form `case dsl"XXX" => x match { case dsl"YYY" => ... }`, the extractor `YYY` should use type parameters distinct from the ones used in `XXX`.
This also means that recursive extractors should define their `newTypeParams` parameters locally (so each recursive invocation has a distinct state).






## Step 4: Compilation to C

### Memory Management

#### Memory Management Nodes

In order to compile our DSL to C, we must take into account the fact that C does not have garbage collection,
and uses instead explicit allocation and and deallocation of memory using the `malloc`/`free` primitives.

Conversion of garbage-collected programs to explicit memory management is a very hard problem in its most general form.
Here, we will only describe an ad-hoc technique to manage the memory of our `List` objects:
for each `List` creation, we will convert it to an array allocation, and a corresponding deallocation (free) at the end of the current block.

**Caveat**: This simplistic scheme is only sound as long as `List` objects do not escape the blocks they are defined in
(for example, by being returned).
The next logical step would be to perform escape analysis (which is an existing analysis in SC),
either to prevent escape or to use a more elaborate scheme to handle escapes,
but this is out of the scope of this tutorial.

In order to represent allocation/deallocation in the DSL, we add an object `Mem` that provide the corresponding nodes:
```scala
@deep
@needs[Numeric[_] :: ClassTag[_]]
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

The main idea is to use an `arrays` buffer containing all arrays created within the current block,
and to deallocate them at the end of the block:

```scala
case class Arr[T](arr: Rep[Array[T]])(implicit val tp: TypeRep[T])

rewrite += statement {
  case sym -> (x @ dsl"new Array[A]($size)") => 
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


### Generating C Syntax

[TODO]






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

As you can see, it is easier to use quasiquotes to match deep embedding nodes, because they hide implementation details of the Intermediate Representation by providing the same language as the shallow library.