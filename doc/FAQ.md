# Frequently Asked Questions

## General

#### How do I use a value of type `T`, known at compile-time, as a value of type `Rep[T]` part of the generated program?

If this value is of a type that can be lifted as a constant (like `Int`, `String`, etc.),
you can directly splice it in a `dsl` block, as in `val cst = 42; dsl"$cst"`.

For example, if you have a `Schema` object `sch`,
you can write `val s = sch.size; dsl"$s + 1"`, or equivalently `dsl"${sch.size} + 1"`.
However, note that you cannot write `dsl"$sch.size + 1"`,
as that would try to make **`sch`** a constant in the generated program,
instead of its bare size.



## Project 2

#### Why does the `relationProject` method take two `Schema` parameters?

This is a minor oversight on our side: there should only be one `Schema` parameter to the `relationJoin` function: the **_final_** schema. Currently, we pass the **_final_** schema twice.
The code in `RecordsLowering` that uses the first `Schema` parameter does behave correctly.


#### Why does the `print` function not work?

If you try using `print` in `dsl` quasiquotes, you will end up with
[strange errors ](https://github.com/epfldata/sc-public/issues/13).
This is because we do not provide a deep embedding for `print`.
This is intended, as it forces you to use string operations before printing out with `println`.


#### What do the `sbt relation-deep/run` and `sbt relation-gen/run` commands do exactly?

The first command, `sbt relation-deep/run`, _compiles_ one program specified in the file `relation-deep/compiler/Main.scala` using our relation-DSL compiler. For this reason, you have to run that command every time you change the program _or_ the compiler (for example, if you change `ColumnStoreLowering.scala`).

The resulting code is generated [by default](https://github.com/epfldata/sc-public/blob/master/relation-dsl/relation-deep/src/main/scala/relation/compiler/RelationCompiler.scala#L52) in `generator-out/src/main/scala/GenApp.scala`.
The `relation-gen` project is just another sbt project defined to run this code easily, using the command `sbt relation-gen/run`.
This is the command that will make the query defined in the compiled program actually execute.


