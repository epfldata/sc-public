SC Public
===============

This repository contains example Domain-Specific Languages (DSLs) defined using the SC framework.

To learn and develop optimized DSLs using SC, one should first get familiar with some programming language fundamentals,
explained in the [development process](doc/DevProcess.md) page.

We concretely demonstrate the development process through three toy examples:
 * the [List](list-dsl) tutorial: using quasiquotes to define lowerings and offline optimizations on a basic `List` class, and converting it to use simple C-like memory management
 * the [Relation](relation-dsl-sc) tutorial (for SC): using quasiquotes to define transformations for compiling relational algebra queries, specializing them based on Schema information.
 * the [Relation](relation-dsl-squid) tutorial (for [squid](https://github.com/epfldata/squid)): using quasiquotes to define transformations for compiling relational algebra queries to a push-based query engine, specializing them based on Schema information, specializing hash-table data-structures, and converting row layout to columnar layout representation. 
 * the [Vector](vector-dsl) tutorial: defining a DSL for working with Vectors
 
Some additional docs are provided in the [`doc`](doc) folder.

You can download the binaries of SC [here](https://github.com/epfldata/sc-public/releases).
