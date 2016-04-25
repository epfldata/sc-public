# Multi-Stage Programming & Quasiquotes

Multi-Stage Programming (MSP) is a robust and reliable way to perform program generation,
either at run time or at compile time (although the original vision focused only on runtime code generation).
We recommend one to read <a name="Taha2004">[1]</a> to get an idea of that technique.
Similar to MetaOCaml, SC implements quasiquotes, but as a Scala library (no need to modify the Scala compiler). 
The syntax uses Scala's generalization of string interpolation – for example `dsl"42"` represents a program fragment for the constant integer `42`.
The type of that expression is `Rep[Int]`, similar to MetaOCaml's `int code`.


## Construction \& Extraction

SC quasiquotes, which manipulate SC IR nodes, support both their _construction_ and their _extraction_ (via pattern-matching).
For example, 
the following 
code demonstrates the construction of a program fragment that adds two constant integers `2` and `42`,
and then matches that fragment to extract to what constant `2` is added, printing the result:

```scala
val n = dsl"42";  val m = dsl"2 + $n"
val extr: Int = m match {
  case dsl"2 + (${Constant(k)}: Int)" => k }
println(extr) // prints 42
```

## Effectful Reification

Following LMS <a name="LMS">[2]</a> and other systems before it, we use effectful ANF node reification,
which means that any non-trivial subexpression is assigned to an intermediate local variable.
In particular, the reification of effectful DSL expressions like `dsl"x.print"` will add a corresponding node in the generated program,
even if the result of the `dsl` expression is not stored or returned.
 
For example, consider the following rewrite rule for some fictive `Stack`-related DSL:

```scala
// logging for Stack `add':
rewrite += rule {
  case dsl"($s: Stack[A]).add($e)" =>
    dsl""" println("Adding "+$e+" to "+$s) """
    dsl"$s.add($e)" }
```

The result of the `dsl` statement introducing a call to `println` is lost,
but the statement will be registered by SC as a stateful expression that should execute before the call to `s.add(e)`.

As a result of this effectful node reification,
one can write programs that mix `dsl` and non-`dsl` code in liberal ways.
For example, it becomes easy to write static loop unrolling.
Consider the following example of a `dsl` program representing a loop over some collection `xs`, of type `Rep[Seq[MyClass]]`:


```scala
dsl"""
  var sum = 0
  for (x <- $xs) sum += x.field
  sum * 2
"""
```

If the size of the list `xs` is statically known, say if we have a `Seq[Rep[MyClass]]` instead, we can write the more efficient:


```scala
val sum = newVar(dsl"0")
for (x <- xs) dsl"$sum += $x.field"
dsl"$sum * 2"
```

In this second version, \emph{no loop} is generated, as the loop is executed during code generation (we can say it is partially-evaluated).


## Remarks

As hinted above, it is important to understand the difference between a `Rep[Array[T]]` and an `Array[Rep[T]]`,
and similarly
between a `Rep[A => B]` and a `Rep[A] => Rep[B]`.
 
SC performs automatic conversion of meta-functions. This means that in any place where the code for a function, of type `Rep[A => B]`, is expected,
one can splice in a `Rep[A] => Rep[B]` instead.

Thus one can write programs like:

```scala
val gen = (f: Rep[Int] => Rep[Int]) =>
  dsl"ArrayBuffer(1,2,3) map $f"
gen { x => dsl"$x + 1" }
```


## References

<sup>[[1]](#Taha2004)</sup> W. Taha. Domain-Specific Program Generation: International Seminar, Dagstuhl Castle, Germany, March 23-28, 2003. Revised Papers, chapter [A Gentle Introduction to Multi-stage Programming](https://www.cs.rice.edu/~taha/publications/journal/dspg04a.pdf), pages 30–50. Springer Berlin Heidelberg, Berlin, Heidelberg, 2004. 

<sup>[[2]](#LMS)</sup> T. Rompf and M. Odersky. [Lightweight modular staging: a pragmatic approach to runtime code generation and compiled DSLs](http://infoscience.epfl.ch/record/150347/files/gpce63-rompf.pdf). In Generative Programming and Component Engineering, pages 127–136, 2010.


