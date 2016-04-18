Development Process
===================


## DSL Implementation in SC

One of the most common ways of implementing a Domain-Specific Language (DSL, referred to as _the **object** language_) is to embed it in a **host** language [1]. This class of DSLs is known as Embedded DSLs (EDSL). With this approach, the object language reuses the frontend (parser and typechecker) of the compiler developed for the host language. 

The first step to define an EDSL is to implement it as a library written using the Scala programming language. It will use the Scala compiler and its runtime system (i.e., the JVM). However, there are cases in which the object language needs to override the behaviour of Scala constructs. For example, `if (...) ... else ...` (*if-then-else*) expressions in the object language may have a different semantics than in Scala. **Language virtualization** [2] is the tool that allows one to override the definition of these core constructs.

Scala-Virtualized [3] is a modified version of the Scala compiler that provides such virtualization features for embedding DSLs. Yin-Yang [4] is another effort which uses Scala macros for the same purpose. In our approach, the libraries written in Scala use Yin-Yang whenever language virtualization is needed.

Defining the DSL as a library inside Scala provides numerous advantages, but prevents us from what really makes DSLs interesting in the first place:
 * Domain-specific optimizations.
 * Removal of interpretation overhead (Scala and its core library, running on the JVM).
 * Different target language (i.e., compiling to C).

The second step in defining our EDSL uses *SC* (Systems Compiler) and solves those issue in the following way. The constructs of the object language are converted into intermediate representation (IR) nodes. IR nodes are manipulated using the *SC Pardis Compiler*, which rewrites a program encoded with them into a more efficient one. Furthermore, the IR nodes of the rewritten program can be converted into IR nodes of another target language (such as C). As a result, for any program written using the EDSL, we generate a new more optimal program written in the chosen target language.

For automatically converting the constructs of an EDSL defined as a library into IR nodes, we developed a compiler plugin, known as *SC Purgatory*. In order to provide precise information about the semantics of generated nodes, the EDSL library should be annotated with a set of predefined annotations. For example, the `@pure` annotation denotes the absence of side-effects for the method that it annotates.

In summary, there are two main phases for defining an EDSL in Scala using SC. First, the EDSL should be defined as a normal Scala library. This way, we develop an **interpreter** for that EDSL using the Scala programming language. This approach is also known in the literature as the *shallow embedding* of the EDSL. Second, this interpreter should be annotated, which allows the automatic generation of a **compiler** that is able to remove the interpretation overhead of the host language, perform optimizations, and target different languages. This approach is known as the *deep embedding* of the EDSL.


## Transformations

*SC* provides two approaches for performing transformations on a program.

 - Performing transformations while reifying (creating) IR nodes, 
which we refer to as *online transformations*. 
To do so, we use an approach known as *tagless final* [5] in which DSL constructs are encoded as functions.
This approach is in essence the same as *polymorphic embedding* [6] which is also used in *LMS* [7].
The transformations are applied when the functions corresponding to DSL constructs are invoked. The invocation results in reifying the optimized IR nodes.

 - Performing rewritings on already-reified IR nodes, which we refer to as *offline trasformations*. These transformations
are used whenever a global program analysis is required for an optimization to be applied properly, or when performing lowerings.


## References

[1] Hudak, Paul. "Building domain-specific embedded languages." ACM Computing Surveys (CSUR) 28.4es (1996): 196.

[2] Chafi, Hassan, et al. "Language virtualization for heterogeneous parallel computing." ACM Sigplan Notices. Vol. 45. No. 10. ACM, 2010.

[3] Moors, Adriaan, et al. "Scala-virtualized." Proceedings of the ACM SIGPLAN 2012 workshop on Partial evaluation and program manipulation. ACM, 2012.

[4] Jovanovic, Vojin, et al. "Yin-Yang: Concealing the deep embedding of DSLs." Proceedings of the 2014 International Conference on Generative Programming: Concepts and Experiences. ACM, 2014.

[5] Carette, Jacques, et al. "Finally tagless, partially evaluated: Tagless staged interpreters for simpler typed languages." Journal of Functional Programming 19.05 (2009): 509-543.

[6] Hofer, Christian, et al. "Polymorphic embedding of DSLs." Proceedings of the 7th international conference on Generative programming and component engineering. ACM, 2008.

[7] Rompf, Tiark, and Martin Odersky. "Lightweight modular staging: a pragmatic approach to runtime code generation and compiled DSLs." Communications of the ACM 55.6 (2012): 121-130.
