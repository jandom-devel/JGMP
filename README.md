# JGMP
Java bindings for the GMP Library.

The GNU Multiple Precision Arithmetic Library ([GMP](https://gmplib.org/)) is a widely used
library for computing with arbitrary precision arithmetic. The library has
bindings for many programming languages, including .NET, OCaml, Perl,
PHP, Python, R, Ruby, and Rust, with the notable exception of Java.

The JGMP library provides Java bindings and wrappers for using GMP from within any JVM-based language.  It should work with all GMP versions from 5.1.0 onward. The JGMP library has been tested on Linux, Windows 10 and mac OS (on the x86-64 architecture) with GMP 6.2.1.

The documentation is available on [javadoc.io](https://javadoc.io/doc/it.unich.jgmp/jgmp/latest/index.html).

A set of benchmarks and examples is available in [JGMPBenchmarks](https://github.com/jandom-devel/JGMPBenchmarks).

## Installation instructions

JGMP is available on [Maven Central Repository](https://central.sonatype.com/) using groupId ``it.unich.jgmp`` and artifactId ``jgmp``.

Before working with JGMP you need to install the native GNU Multiple Precision Arithmetic Library ([GMP](https://gmplib.org/)). The native library is loaded by a static class initializer using the <code>[NativeLibrary](https://java-native-access.github.io/jna/5.13.0/javadoc/com/sun/jna/NativeLibrary.html).getInstance</code> method of JNA, and it must be placed in a path where it is discoverable.

In Linux and macOS you may generally install GMP with your preferite package manager. In Windows you can put the ``gmp.dll`` file directly in the main folder of the application using JGMP.

----

Authors:
Gianluca Amato and Francesca Scozzari
