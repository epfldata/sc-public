Relation Tutorial
===============


An introduction to Embedded Domain-Specific Language (EDSL) developement can be found on the [Development process](../doc/DevProcess.md) page.
Here, we give an example of that methodology, with a simple library for relational algebra (dubbed `relation-dsl`).

## Step 1: Defining the Library

The EDSL is first defined as a standard Scala library, that we will place in package `relation.shallow`.
It provides a `Relation` data type and associated classes, and ways to apply common manipulations on it (selection, projection, join and aggregation).


```scala
class Relation(val schema: Schema, val underlying: List[Row]) {
  def select(p: Row => Boolean): Relation = ...
  def project(schema: Schema): Relation = ...
  def join(o: Relation, leftKey: String, rightKey: String): Relation = ...
  def aggregate(key: Schema, agg: (Double, Row) => Double): Relation = ...
}
object Relation {
  def scan(filename: String, schema: Schema, delimiter: String): Relation = ...
}

class Row(val values: List[String]) {
  def getField(schema: Schema, fieldName: String): String = ...
}

class Schema(val columns: List[String])
object Schema {
  def apply(columns: String*): Schema = new Schema(columns.toList)
}
```

See the full file [here](src/main/scala/relation/shallow/Relation.scala).

The reason we have a `Relation` "companion object" defined beside the `Relation` class is to provide the factory method `scan` (it's equivalent to a static method in Java).



The EDSL can already be used like a normal Scala library.
It can be tested by typing `sbt console`, and importing `list.shallow._`, or by creating a Scala application that uses it, as in:

```scala
package relation.shallow

object Main extends App {
  val s = Schema("number", "digit")
  val r = Relation.scan("data/R.csv", s, "|")
  r.print
}
```

Such an application can be run by typing `sbt run`.

Now, we'd like to have more power and be able to optimize/transform programs written in the DSL,
as well as compile it to different target languages.
These require _deep embedding_.


## Step 2: Generating the Deep Embedding

### Specifying the Generation

SC provides a way to deeply embed our shallow DSL _automatically_, via an SBT plugin called Purgatory.
We just need to add a few annotations to the library, and Purgatory will handle the rest.
In the case of `Relation`, we have:

```scala
@deep
@needs[Row :: Schema :: List[_]]
class Relation(val schema: Schema, val underlying: List[Row]) {
  ...
```

Here, the `@deep` annotation tells Purgatory that we would like deep embedding of this class both for expression (_Exp_, the normal deep embedding) and extraction (_Ext_, used for pattern-matching with quasiquotes).
In case you do not want to generate the _Ext_ or _Exp_ part, add the `@noDeepExt`/`@noDeepExp` annotation.

`@needs` specifies which DSL features our DSL requires. Since our library uses 2-element tuples and they are not present in the default `Base` DSL embedded with SC, we need to add this requirement. If we omitted it, the generated code would not be able to compile.  
Note that a special `::` type, defined in `pardis.annotations`, is used to separate the types passed to `@needs`.

### Recommended SBT Configuration

In order to use Purgatory, we need to add it to the `plugins.sbt` file:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("ch.epfl.data" % "sc-purgatory-plugin" % "0.1.1-SNAPSHOT")
```

We recommend that one create an SBT sub-project for the deep embedding of the library.
This is to prevent a wrongly-generated deep embedding from breaking compilation of the main project (which would prevent re-generating the deep embedding).

The important generation settings needed in the SBT configuration are the following:

```scala
generatorSettings ++ Seq(
    generatePlugins += "ch.epfl.data.sc.purgatory.generator.QuasiGenerator",
    pluginLibraries += "ch.epfl.data" % "sc-purgatory-quasi_2.11" % "0.1.1-SNAPSHOT",

    outputFolder := "relation-deep/src/main/scala/relation/deep",
    inputPackage := "relation.shallow",
    outputPackage := "relation.deep"
)
```

One should not forget the `generatePlugins` line to be able to use quasiquotation, and to specify a correct `outputFolder` (the folder for generated files), `inputPackage` and `outputPackage` (package of the shallow library and package for the deep embedding to be generated).

See [here](project/Build.scala) for the full build file of our example.

Now that SBT is set up, we can proceed to the actual code generation, by going to the main project root directory, and typing `sbt embed`.

In our example, files like `DeepRelation.scala` will be generated inside the folder `relation-deep/src/main/scala/relation/deep`.


### Simple Compilation

In order to use this deep embedding, we can still use the normal shallow syntax, but we have to do so inside of a quasiquotation block `dsl"..."`.
To use `dsl` quasiquotes, we need to extend the `QuasiAPI[RelationDSLOpsPackaged, RelationDSLExtOpsPackaged]` trait and import its content (alternatively, we can make the current package object extend it), where `RelationDSLOpsPackaged` and `RelationDSLExtOpsPackaged` correspond to the deep embeddings of our EDSL (the second one is the one used for extraction).

These are automatically generated by Purgatory after defining our DSL and its dependent constructs using the following trait:

```scala
@language
@deep
@quasiquotation
@needs[Relation :: RelationScanner :: Array[_] :: ScalaCore]
trait RelationDSL
```

`ScalaCore` contains the deep embedding of the core part of the Scala standard library provided by SC. `@quasiquotation` specifies that we would like to use quasiquotation to construct and perform pattern matching on the IR nodes. `@language` specifies that the current trait specifies a (domain-specific) language in the 
deep embedding.

An example of quasiquote use to define a program follows:
```scala
implicit object Context extends RelationDSLOpsPackaged // required by the `dsl` string interpolator

def pgrm = dsl"""  
  val s = Schema("number", "digit")
  val r = Relation.scan("data/R.csv", s, "|")
  r.print
"""
```

What the `dsl` macro does is typecheck the shallow expression that we pass it, and transform it to deep embedding nodes (_RelationDSLOpsPackaged_ if it is used in an expression, _RelationDSLExtOpsPackaged_ if it is used in a pattern).

**Note**: If you have trouble with a quasiquote, there is a page dedicated to [debugging quasiquotes](../doc/DebuggingQuasiquotes.md).

Finally, in order to generate a program from this deep embedding, we have to extend `pardis.compiler.Compiler[RelationDSLOpsPackaged]` and define a code generator for it. This is going in the opposite direction as the `dsl` macro, i.e., from deep embedding to shallow embedding.

```scala
object MyCompiler extends Compiler[RelationDSLOpsPackaged] {
  val DSL: RelationDSLOpsPackaged = Context
  
  import sc.pardis.prettyprinter._
  
  val codeGenerator = ...
}

// Compiling our program to proper Scala code:
MyCompiler.compile(pgrm, "src/main/scala/GeneratedApp")
```

(See file [RelationCompiler.scala](relation-deep/src/main/scala/relation/compiler/RelationCompiler.scala) for an example definition of `codeGenerator`.)





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

object MyCompiler extends Compiler[RelationDSLOpsPackaged] {

  ...
  
  pipeline += new MyTransformer[RelationDSLOpsPackaged](DSL)
  
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






### Partial Evaluation of Schema

We have a nice and simple language for writing queries, but the execution is inefficient because it has to constantly
read from the fields of a `Schema` and manipulate each row as a `List[String]` with no static size information.
It's unfortunate, because we notice that in most applications, the schema is static, and thus the application could be
specialized for specific schemas and avoid all that overhead.

The first step is to match schemas that are actually static. For this, we write a Scala extractor that uses quasiquotes:

```scala
object StaticSchema {
  def unapply(schema: Rep[Schema]): Option[Schema] = schema match {
    case dsl"Schema($xs*)" =>
      val names = xs map { case Constant(x) => x case _ => return None }
      Some(new Schema(names.toList))
    case _ => None
  }
}
```

It takes as input a `Rep[Schema]` and returns a `Schema` (notice: not a `Rep[Schema]`) if successful; otherwise `None.`
For it to the extractor to be successful, every part of the schema has to be a constant.

Having static `Schema`s is useful because we can now associate them with tailored record types that more efficiently store and access row data.
Creating a record type is done through the `__new[Rec](fields)` function, and can be used with a special syntax inside of quasiquotes.
In the same transformation, we also _lower_ the representation of relations to simple arrays (see the [list tutorial](../list-dsl) for more info on lowerings).

For implementing joins, we will also need to keep a static (compile-time) mapping of schema objects to their corresponding schema:

```scala
val symbolSchema = mutable.Map[Rep[_], Schema]()
```

Here is the code for the specialized implementation of `project`, implemented as a transformation:

```scala
rewrite += symRule {
  case rel @ dsl"(${ArrFromRelation(arr)}: Relation).project(${StaticSchema(schema)})" => 
    symbolSchema += rel -> schema
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    def copyRecord(e: Rep[Any]): Rep[Rec] =
      __new[Rec](schema.columns.map(column => (column, false, field[String](e, column))): _*)
    val newArr = dsl"new Array[Rec]($arr.length)"
    dsl"""
      (0 until $arr.length) foreach ${ __lambda[Int,Unit]((x: Rep[Int]) =>
        dsl"$newArr($x) = ${ copyRecord( dsl"$arr($x)" ) }"
      )}
    """
    newArr
}
```

Notice how the `(0 until $arr.length)` part is a dsl block used merely for its side effects (which translate to side effects in the generated program).
The `ArrFromRelation` extractor is defined in a way analogous to `ArrFromList` in the [list tutorial](../list-dsl).

The code above can be simplified thanks to meta-function splicing, which allows us to splice in a meta function, of type `Rep[A] => Rep[B]` as if it was a `Rep[A => B]`, removing the need for the ugly `__lambda` conversion function:


```scala
rewrite += symRule {
  case rel @ dsl"(${ArrFromRelation(arr)}: Relation).project(${StaticSchema(schema)})" => 
    symbolSchema += rel -> schema
    implicit val recTp: TypeRep[Rec] = new RecordType[Rec](getClassTag, None)
    val copyRecord = (e: Rep[Any]) =>
      __new[Rec](schema.columns.map(column => (column, false, field[String](e, column))): _*)
    dsl"""
      val newArr = new Array[Rec]($arr.length)
      for (i <- 0 until $arr.length) newArr(i) = $copyRecord($arr(i))
      newArr
    """
}
```

The simplified solution above does apply `opyRecord` in the object language (as opposed to applying it in the meta language as in the initial code), so it looks like it would add some runtime overhead. Fortunately, in the compiler described here we apply beta reduction automatically by mixing the trait `BasePartialEvaluation` in the DSL IR, so the overhead will be removed before the program is generated.



### Conclusion

We have defined a simple Scala DSL for relational algebra, and implemented optimizations to specialize relations that have static schemas
and to use low-level array constructs, thereby getting rid of execution overhead.






