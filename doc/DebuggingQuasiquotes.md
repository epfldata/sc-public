# Debugging Quasiquotes

## Introduction

Quasiquotes rely on macros that generate node construction/extraction code, depending on whether they are in expression or pattern position.

### `apply` quasiquotes

For a construction QQ, for example `val x = dsl"$n + 1"` (assuming a value `x: Rep[Int]` is in scope), what happens is as follows:
 42. The quoted expression is reconstructed and parsed with unique identifiers in place of the holes (here, `$n` would be replaced with something like `n$macro$1`)
 42. The parsed expression is type-checked
 42. The typed shallow tree is virtualized (converted to deep embedding) and spliced inside a suitable context (an instantiation of the deep DSL trait), and the result is returned by the macro

### `unapply` quasiquotes

For a construction QQ, for example `val dsl"($n: Int) + 1 = x"`, what happens is as follows:
 42. The quoted expression is reconstructed and parsed with unique identifiers in place of the holes (here, `$n` would be replaced with something like `n$macro$1`)
 42. The parsed expression is type-checked
 42. The typed shallow tree is virtualized (converted to deep embedding)
 42. It is then spliced inside the deep DSL trait, type-checked (by the macro itself), and the types corresponding to each hole is retrieved in order to generate a Scala `unapply` function with the right types (this will ensure the visible type of `n` is `Rep[Int]`)
 42. Finally, the result is spliced inside the deep *extraction* DSL trait and returned by the macro


## Common Failures

### Missing Deep Representation

QQ can fail when the deep version of an object is not present in the DSL trait. For `apply` QQ, this means incorrect code will be generated and returned by the macro, which will not have type-checked it for performance reasons.

This manifests with type-mismatch error saying type `T` is required, but `Rep[T]` is found.
For example, consider the program `dsl"""Symbol("ok")"""`. It generates the following error, which is hard to understand:
```
Error:(67, 3) type mismatch;
 found   : QQGenerated.this.Rep[String]
    (which expands to)  ch.epfl.data.sc.pardis.ir.Expression[String]
 required: String
  dsl"""Symbol("ok")"""
  ^
```
The problem was that in the deep embedding DSL trait, there is no deep `Symbol` object, so the object used is the shallow one, that expects a `String` instead of a `Rep[String]`. This is a hygiene problem that will hopefully be fixed in future versions.

Note: another manifestation of the problem can be errors like:
```
[error] not found: value __newHashMap
[error]     dsl" new HashMap[String, Int] "
[error]     ^
```



### Typing Problems

When compiling an extraction (or `unapply`) QQ, the compiler cannot always infer the right types.
For example, in `case dsl"$n + 1 => ...`, the compiler has no way of guessing which overloaded `+` operation is matched (is it `+` from `Int`, `String`, `Double`, etc.?).

However, sometimes the type is not really relevant. For example, consider the rewriting `case dsl"List($a,$b)" => dsl"List($b,$a)`. This is why the QQ macro will type-check such extractor as `case dsl"List[?A]($a,$b)" =>`, where `?A` is a *synthetic*, existential type parameter generated on the fly for this purpose. This mechanism is limited, however, and may not always infer all possible combinations of types that would make the pattern type-check.


## Solutions to Help Debugging

### Alternative Syntax

This is the recommended way to solve a QQ problem.

To understand what's happening with a particular quasiquote, change the `dsl` to `dbgdsl`. Its compilation will then generate information such as the code that is parsed, typed and virtualized, and the final generated code. In addition, it will make `apply` quasiquotes check that the program they generate is well-typed, catching potential errors and giving a better error message.

Example: Instead of `dsl"""Symbol("ok")"""`, write `dbgdsl"""Symbol("ok")"""`. It will produce the better error message:
```
Error:(68, 3) Type-checking of deep embedding failed: type mismatch;
 found   : QQGenerated.this.Rep[String]
    (which expands to)  ch.epfl.data.sc.pardis.ir.Expression[String]
 required: String
In generated code: `Symbol.apply(this.lift("ok"))`
In macro application:
  dbgdsl"""Symbol("ok")"""
  ^
```

As well as a view of the generated code (printed at compilation), that you can copy and paste this code in your program, and inspect it manually to see what went wrong:
:
```
Warning:scalac: 0> (A) Parsed:   Symbol("ok")
Warning:scalac: 1> Typed: scala.Symbol.apply("ok")
Warning:scalac: 2> (A) Virtualized:   Symbol.apply(this.lift("ok"))
Warning:scalac: 3> (A) Generated:   {
  class QQGenerated extends ch.epfl.data.sc.pardis.deep.DSLExpOpsClass {
    override val ForwardedIR = {
      import ch.epfl.data.sc.pardis.quasi.anf.BaseQuasiExp._;
      scala.Option(implicitly[ch.epfl.data.sc.pardis.quasi.anf.BaseQuasiExp])
    };
    def quoteTerm() = Symbol.apply(this.lift("ok"))
  };
  new QQGenerated().quoteTerm()
}
```


### Implicit Value

It is also possible to set the debugging level in any local context, by defining an implicit of a type that inherits from `ch.epfl.data.sc.pardis.quasi.MacroUtils.DebugInfo`.

Example:
```scala
{
  implicit val _ = ch.epfl.data.sc.pardis.quasi.MacroUtils.ApplyDebug
  dsl"""Symbol("ok")"""
}
```

Possible useful values are:
 - `MacroDebug` to debug everything
 - `ApplyDebug` to debug `apply` QQ
 - `UnapplyDebug` to debug `unapply` QQ
 - `TypeInferDebug` to debug the inference of synthetic type parameters
 - `TypeParamDebug` to debug type parameters 
 - `NoDebug` to turn off debugging info


### Scalac Options

It is also possible to configure the Scala compiler to output the result of macro applications, but the result will be more difficult to parse and understand.
