SC Public
===============

This repository contains example Domain-Specific Languages (DSLs) defined using SC framework.

In order to learn how to develop DSLs using SC, first you have to familiarize yourself with some programming language concepts. 
To do so, have a look at the [development process](doc/DevProcess.md) of an embedded DSL.

We concretely demonstrate this development process through three toy examples:
 * the [List](list-dsl) tutorial: using quasiquotes to define lowerings and offline optimization on a basic `List` class, and converting it to simple C-like memory management
 * the [Relation](relation-dsl) tutorial: using quasiquotes to define transformations for compiling relational algebra and specializing Schema information
 * the [Vector](vector-dsl) tutorial: defining a DSL for working with Vectors
 
Some additional docs are provided in the [`doc`](doc) folder.

You can download the binaries of SC from [here](https://github.com/epfldata/sc-public/releases).
