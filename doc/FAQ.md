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


