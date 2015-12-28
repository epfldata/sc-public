# Internal Notes About the New Tutorial

## Context problems with quasiquotes

Several things don't work when using QQ to write the DSL program to compile:
 * The Online trait can no more be mixed in from `Main`, it has to be mixed in `MyLibDSL`.
 ```
 implicit object Context extends MyLibDSL with Optim.Online
 ```
 * Online (and offline?) transformations using `Def` won't match anything.

These are because QQ defines its own `QQGenerated` trait that extends `MyLibDSL`. Indeed, it won't see the overloaded defs in `Optim.Online`, and it won't register the symbols in the right place, which is `Main.Context` (the last problem should be solved soon).

This is how to define the program without QQ:

```
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





