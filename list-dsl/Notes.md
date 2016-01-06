# Internal Notes About the New Tutorial

## Organization of the repository (for the list tutorial)

The master branch essentially contains:

 - The `README.md`, representing the tutorial text. Changes to that file can be committed to master.
 
 - Folders containing each associated step. The folders are generated automatically, so no change should be made to their content directly.
 
In order to change the tutorial code, do the modification in the `source` branch,
then check out the `list-steps` branch and rebase it from source (`git rebase source`).

`list-steps` contains commits that incrementally strip the tutorial code,
going from step5 (the final state) to state1 (where there is only the shallow code).
Rebasing it will apply the changes in source and try to update the commits accordingly.
You might get conflicts, but they should be easy to fix, and they should _appear only once_ during the rebasing.

You will need `--force` to push that branch to github. Only use `--force` (or `-f`) for **this branch only**.
The history of the other branches should never need to be rewritten.



## Context problems with quasiquotes

Several things don't work when using QQ to write the DSL program to compile:
 * The Online trait can no more be mixed in from `Main`, it has to be mixed in `MyLibDSL`.
 ```
 implicit object Context extends MyLibDSL with Optim.Online
 ```
 * Online (and offline?) transformations using `Def` won't match anything.

These are because QQ defines its own `QQGenerated` trait that extends `MyLibDSL`. Indeed, it won't see the overloaded defs in `Optim.Online`, and it won't register the symbols in the right place, which is `Main.Context` (the last problem should be solved soon).

This is how to define the program without QQ:

```scala
  def pgrm = {
    import Context._
    
    val zero = List[Int]().size
      
    val ls = List(unit(1), unit(2), unit(3))
    
    val r = ls map (__lambda { _ + unit(1) } ) map (__lambda {_.toDouble })
    
    Tuple2(r, zero)
  }
```


## Other Known Problems

 * Making `List` static methods `@pure` creates an error (see code)
 
 * values don't seem to be ambedded (see `List.empty`)





