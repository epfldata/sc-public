SC New Tutorial
===============

An introduction to EDSL developement can be found in the [Development process](https://github.com/epfldata/sc-examples/wiki/Development-process) wiki page.
Here, we give an example of that methodology, with a simple library for manipulating lists (dubbed `mylib`).


## Step 1: Defining the Library

The EDSL is first defined as a standard Scala library, that we will place in package `mylib/shallow`. It provides a `List` data type, and ways to apply common manipulations on it (mainly mapping, filtering and folding).


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

See the full file [here](https://github.com/epfldata/sc-examples/blob/master/newtuto/src/main/scala/mylib/shallow/List.scala).

The EDSL can already be used like a normal Scala library.

Now, we'd like to have more power and be able to optimize/transform it, as well as compile it to different targets. These require _deep embedding_.


## Step 2: Generating the Deep Embedding

### Specifying the Generation

SC provides a way to deeply embed our shallow EDSL _automatically_. This is done by adding a few annotations to the library. Purgatory (the automatic embedding tool of SC) will handle the rest:

```scala
@deep
@quasi
@needs[Char :: (_,_)]
@noImplementation
class List[A](val data: Seq[A]) {
  ...
```

Here, the `@deep` and `@quasi` annotation tells Purgatory that we would like deep embeddings of this class both for expression (_Exp_, the normal deep embedding) and extraction (_Ext_, used for pattern-matching with quasiquotes).

`@needs` specifies which DSL features our DSL requires. Since our library uses 2-tuples, and they are not present in the default `Base` DSL embedded with SC, we need to add this requirement. If we omitted it, the generated code would not be able to compile.  
Note that `(_,_)` is just syntax sugar for `Tuple2[_,_]`, and that a special `::` type, defined in `pardis.annotations`, is used to separate the types passed to `@needs`.

Finally, `@noImplementation` tells Purgatory that for now we only need it to generate nodes for the definitions of our library/DSL, but not for their implementations.



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

    outputFolder := "mylib-deep/src/main/scala/mylib/deep",
    inputPackage := "mylib.shallow",
    outputPackage := "mylib.deep"
)
```

Don't forget the `generatePlugins` line to be able to use quasiquotation, and to specify correct `outputFolder` (the folder for generated files), `inputPackage` and `outputPackage` (package of the shallow library and package for the deep embedding to be generated).

See [here](https://github.com/epfldata/sc-examples/blob/master/newtuto/project/TutoBuild.scala) for the full build file of our example.

Now that SBT is set up, we can proceed to the generation, by going to the main project directory, and typing `sbt embed`.

In our example, a file `DeepList.scala` will be generated inside the folder `mylib-deep/src/main/scala/mylib/deep`.

### Simple Compilation

In order to use this deep embedding, we can still use the normal shallow syntax, but we have to do so inside of a quasiquotation block `dsl`.

For example:
```scala
implicit object Context extends MyLibDSL // required by `dsl`

def pgrm = dsl"""  
  val ls = List(1, 2, 3)
  ls map (_ + 1)
"""
```

In order to use `dsl` quasiquotes, you need to extend the `QuasiAPI[MyLibDSL, MyLibDSLExt]` trait (you can make the package object extend it), where `MyLibDSL` and `MyLibDSLExt` correspond to the deep embedding of your EDSL (the second one is the one used for extraction).

These can be defined as follows:

```scala
class MyLibDSL extends DSLExpOpsClass with ListOps

class MyLibDSLExt extends DSLExtOpsClass with ListExtOps
```

`DSLExpOpsClass` contains the default deep embedding provided by SC and `ListOps` corresponds to the one generated by Purgatory for our `List` class.

What the `dsl` macro will do is to typecheck the shallow expression that we pass it, and transform it to deep embedding (_MyLibDSL_ if it is used in an expression, _MyLibDSLExt_ if it is used in a pattern).

Finally, in order to generate a program from this deep embedding, we have to extend `pardis.compiler.Compiler[MyLibDSL]` and define a code generator for it. This going in the opposite direction as the `dsl` macro, i.e., from deep embedding to shallow embedding.

```scala
object MyCompiler extends Compiler[MyLibDSL] {
  val DSL: MyLibDSL = Context
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = ...
}
MyCompiler.compile(pgrm, "src/main/scala/GeneratedApp")
```

(See file [MyCompiler](https://github.com/epfldata/sc-examples/blob/master/newtuto/mylib-deep/src/main/scala/mylib/compiler/MyCompiler.scala) for an example of definition for `codeGenerator`.)




## Step 3: Defining Optimizations

### Online Optimizations

#### Using Quasiquotes

Online optimizations are achieved through "Smart Constructors", deep embedding node constructors that may change their behavior depending on the subnodes they are given.

A typical example is a constructor that directly generates the literal `0` instead of a node for calling `size` on an empty list.

This can be defined easily by overriding the `listSize` method found in `ListOps`, and giving it this body:

```scala
override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
    case dsl"shallow.List()" => dsl"0"
    case _ => super.listSize(self)
  }
```

Notice that we use a `shallow.` prefix in front of `List` in order not to capture the `List` object defined in `ListOps`, bur the actual shallow-embedding `List` object.


#### Manually

Note that the transformation above is essentially equivalent to the following, manual definition:

```scala
   override def listSize[A: TypeRep](self : Rep[List[A]]) : Rep[Int] = self match {
     case Def(ListApplyObject(Def(PardisLiftedSeq(Seq())))) => unit(0)
     case _ => super.listSize(self)
   }
```

As you can see, it is easier to use quasiquotes to match deep embedding nodes, because they hide implementation details of the Intermediate Representation by providing the same language as the shallow library.



### Offline Optimizations

Offline optimizations are applied after constructing the full programin the deep embedding (and thus after online optimizations have been applied through smart constructors).

In order to define such transformations, you need to add them to the pipeline of `MyCompiler`. The analysis and rewrite rules can be written in the following general syntax:

```scala
object MyCompiler extends Compiler[MyLibDSL] {
  ...
  pipeline += new pardis.optimization.RecursiveRuleBasedTransformer[MyLibDSL](DSL) {
    rewrite += rule {
      case dsl"List($_).size" => dsl"1"
    }
  }
  
}
```

The example above is similar to the one we described as an online transformation, but applies to lists of size `1` instead.

Again, both Quasiquotes and a manual approach can be used to define transformations.


---

[TODO] explain where to define the smart ctor overrides

[TODO] more advanced example: map.map -> map

[TODO] talk about inlining

[TODO] generalization of size optim with `dsl"List(..$args).size"`




## Step 4: Compilation to C

[TODO]


