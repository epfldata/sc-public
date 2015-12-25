Development Process
===================

One of the most common ways of implementing a Domain-Specific Language (DSL, referred to as _the **object** language_) is to embed it in a **host** language. This class of DSLs is known as Embedded DSLs (EDSL). With this approach, the object language reuses the frontend (parser and typechecker) of the compiler developed for the host language. 

The first step to define an EDSL is to implement it as a library written using the Scala programming language. It will use the Scala compiler and its runtime system (i.e., the JVM). However, there are cases in which the object language needs to override the behaviour of Scala constructs. For example, `if (...) ... else ...` (*if-then-else*) expressions in the object language may have a different semantics than in Scala. **Language virtualization** is the tool that allows one to override the definition of these core constructs.

Scala-Virtualized is a modified version of the Scala compiler that provides such virtualization features for embedding DSLs. Yin-Yang is another effort which uses Scala macros for the same purpose. In our approach, the libraries written in Scala use Yin-Yang whenever language virtualization is needed.

Defining the DSL as a library inside Scala provides numerous advantages, but prevents us from what really makes DSLs interesting in the first place:
 * Domain-specific optimizations.
 * Removal of interpretation overhead (Scala and its core library, running on the JVM).
 * Different target language (i.e., compiling to C).

The second step in defining our EDSL uses *SC* (Systems Compiler) and solves those issue in the following way. The abstractions defined for the object language are converted into intermediate representation (IR) nodes. These IR nodes are manipulated using the *SC Pardis Compiler*, which rewrites the program into a more optimal one. Furthermore, the IR nodes of the rewritten program can be converted into IR nodes of another target language (such as C). As a result, for any program written using the EDSL, we generate a new more optimal program written in the chosen target language.

In particular, for converting the abstractions of the EDSL (defined in library) to IR nodes, we developed a compiler plugin, known as *SC Purgatory*. In order to provide more precise information about IR nodes, the EDSL library should be annotated with a set of predefined annotations. For example, the `@pure` annotation denotes the absence of side-effects for the method that it annotates.

In summary, there are two main phases for defining an EDSL in Scala using SC. First, the EDSL should be defined as a normal Scala library. This way, we develop an **interpreter** for that EDSL using the Scala programming language. This approach is also known in the literature as the *shallow embedding* of the EDSL. Second, this interpreter should be annotated, which allows the automatic generation of a **compiler** that is able to remove the interpretation overhead of the host language, perform optimizations, and target different languages. This approach is known as the *deep embedding* of the EDSL.