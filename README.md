# Java Virtual Machine (JVM) Performance Benchmarks

This repository contains different JVM benchmarks for the C2/Graal JIT Compilers and the Garbage Collectors.

Each benchmark focuses on a specific execution pattern that is (potentially fully) optimized under ideal conditions (i.e., clean profiles). Such conditions might differ in real-life applications, so the benchmarks results are not always a good predictor on a larger scale. Even though the artificial benchmarks might not reveal the entire truth, they tell enough if properly implemented.

For the full copyright and license information, please view the [LICENSE](LICENSE) file distributed with the source code.

## Authors

- [Ionut Balosin](https://www.ionutbalosin.com)
- [Florin Blanaru](https://twitter.com/gigiblender)

## Purpose

The goal of the project is to assess:

1. different Compiler optimizations by following specific code patterns. At a first glance, even though some of these patterns might rarely appear directly in the user programs, they could occur after a few optimizations (e.g., inlining of high-level operations)
2. different Garbage Collectors' efficiency in both allocating but also reclaiming objects

In this regard, all benchmarks are relatively simple but focused on specific goals.

The benchmarks are written using [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh) which is an excellent tool for measuring the throughput and sampling latencies end to end.

We left **out of scope** benchmarking any "syntactic sugar" language feature (e.g., records, sealed classes, pattern matching for the switch, local-variable type inference, etc.) as well as larger applications (e.g., web-based microservices, etc.).

## Infrastructure baseline
We provide a baseline benchmark for the infrastructure, [InfrastructureBaselineBenchmark](./benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/InfrastructureBaselineBenchmark.java), that can be used to assess the infrastructure overhead for the code to measure.

It measures the performance of empty methods (w/ and w/o explicit inlining) but also the performance of returning an object versus consuming it via blackholes. All of these mechanisms are used inside the real suite of tests.

This benchmark is particularly useful in case of a comparison between different JVMs and JDKs, and it should be run before any other real benchmark to check the default costs. 
In that regard, if the results of the infrastructure baseline benchmark are not the same, it does not make sense to compare the results of the other benchmarks between different
JVMs and JDKs.

## JMH caveats

### HotSpot-specific compiler hints

> JMH uses HotSpot-specific compiler hints to control the Just-in-Time (JIT) compiler. 

For that reason, at the moment, the fully supported JVMs are all the HotSpot-based VMs, including vanilla OpenJDK and Oracle JDK builds. 
GraalVM is also supported.
For more details please check the [compiler hints](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/runner/CompilerHints.java#L37) and [supported VMs](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/runner/format/SupportedVMs.java#L31).

### Blackholes

> Using JMH Blackhole.consume() might dominate the costs, obscuring the results, in comparison to a normal Java-style source code.

Starting OpenJDK 17 the compiler supports blackholes [JDK-8259316](https://bugs.openjdk.org/browse/JDK-8259316). 
This optimization is available in [HotSpot](https://github.com/openjdk/jdk/blob/master/src/hotspot/share/opto/library_call.cpp#L7843) and the [Graal compiler](https://github.com/oracle/graal/blob/master/compiler/src/org.graalvm.compiler.nodes/src/org/graalvm/compiler/nodes/debug/BlackholeNode.java).

In order to perform a fair comparison between OpenJDK 11 and OpenJDK 17, compiler blackholes should be manually disabled in the benchmarks. 

The cost of `Blackhole.consume()` is zero (the compiler will not emit any instructions for the call) when compiler blackholes are enabled and supported by the top-tier JIT compiler of the underlying JVM. 

In case a benchmark annotated method returns a value instead of consuming it via `Blackhole.consume()` 
(e.g., the case of non-void benchmark methods), JMH will wrap the return value of the benchmark 
method in a blackhole, in order to avoid dead code elimination. 
In this case, blackholes are generated by the test infrastructure even though there is no explicit use of them in the benchmarks.  

> Explicitly using `Blackhole.consume()` (in hot loops) can result in misleading benchmark results, especially when compiler blackholes are disabled.

For that reason, to focus on broader benchmarks reusability (i.e., across different JDK versions and distributions) and increased test fidelity we recommend avoiding (whenever it is possible) the explicit usage of `Blackhole.consume()`.

## Supported JVMs

No. | JVM distribution   | JDK versions |  Supported
-------------- |--------------------|--------------| -------------------------------
1 | OpenJDK HotSpot VM | 11, 17       | [Yes](https://projects.eclipse.org/projects/adoptium.temurin/downloads/)
2 | GraalVM CE        | 11, 17       | [Yes](https://www.graalvm.org/downloads/)
3 | GraalVM EE        | 11, 17       | [Yes](https://www.graalvm.org/downloads/)
4 | Eclipse OpenJ9 VM  | N/A         | No, see the resons below
5 | Azul Prime (Zing)  | N/A         | No, see the resons below

### Eclipse OpenJ9 VM

JMH may functionally work with [Eclipse OpenJ9](https://www.eclipse.org/openj9). However, all the [compiler hints](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/annotations/CompilerControl.java) will not apply to Eclipse OpenJ9 and this might lead to different results (i.e., unfair advantage or disadvantage, depending on the test).

For more details please check [JMH with OpenJ9](https://github.com/eclipse-openj9/openj9/issues/4649) and [Mark Stoodley on Twitter](https://twitter.com/mstoodle/status/1532344345524936704)

At the moment we leave it **out of scope** Eclipse OpenJ9 until we find a proper alternative.

### Azul Prime (Zing)

Publishing benchmark results for Azul Prime (Zing) is prohibited by [Azul's Evaluation Agreement](https://www.azul.com/wp-content/uploads/Azul-Platform-Prime-Evaluation-Agreement.pdf). 
It is only allowed to publish results for Azul Prime if prior consent is obtained from Azul.

As of now, we decided to skip this JVM in order to avoid the additional overhead of obtaining such consent.

## OS tuning: how to get consistent results

When doing benchmarking, it is recommended to disable potential sources of performance non-determinism.

### Linux

The Linux OS tuning script [configure-linux-os.sh](./configure-linux-os.sh) enables all the configurations listed below:
- set CPU(s) isolation (with `isolcpus` or `cgroups`)
- disable address space layout randomization (ASLR)
- disable turbo boost mode
- set CPU governor to performance
- disable CPU hyper-threading

>Note: all tuning configurations are tested on Ubuntu (i.e., a Debian based) Linux distribution.

For further references please check:
- [LLVM benchmarking tips](https://llvm.org/docs/Benchmarking.html#linux)
- [How to get consistent results when benchmarking on Linux?](https://easyperf.net/blog/2019/08/02/Perf-measurement-environment-on-Linux) 

### macOS

For macOS, some of the tunings described for Linux are not applicable. For example, the Apple M1/M2 chips (ARM-based) do not have hyper-threading, nor a turbo-boost mode (i.e., these are specific to the Intel chips). In addition,  [disabling ASLR](https://opensource.apple.com/source/lldb/lldb-76/tools/darwin-debug/darwin-debug.cpp) looks more cumbersome, etc.

Due to these reasons, the script [configure-mac-os.sh](./configure-mac-os.sh) does not enable any specific macOS tuning configuration.

### Windows

Windows is not our main focus therefore the script [configure-win-os.sh](./configure-win-os.sh) does not enable any specific Windows tuning configuration.

## Prerequisites

### Install JDK/JVM

To run the benchmarks on different JVM distributions / JDK versions, please install the corresponding build:

No. | JVM distribution   | JDK versions |  Build
-------------- |--------------------|--------------| -------------------------------
1 | OpenJDK HotSpot VM | 11, 17       | [download](https://projects.eclipse.org/projects/adoptium.temurin/downloads/)
2 | GraalVM CE        | 11, 17       | [download](https://www.graalvm.org/downloads/)
3 | GraalVM EE        | 11, 17       | [download](https://www.graalvm.org/downloads/)

At the moment we support only JDK Long-Term Support (LTS) versions. If there is a need for a JDK feature release, please configure it by yourself.

Additionally, if you decide to install a different OpenJDK build, we recommend to take one with [Shenandoah GC](https://wiki.openjdk.org/display/shenandoah/Main) available.

### Configure JDK/JVM

Open the [configure-jvm.sh](./configure-jvm.sh) script file and update the corresponding **JAVA_HOME** property (as per your system path):
```
export JAVA_HOME="<path_to_jdk>"
```

#### A few examples

Linux OS:
```
export JAVA_HOME="/usr/lib/jvm/openjdk-17.0.5"
```

Mac OS:
```
export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-17.0.5/Contents/Home"
```

Windows OS:
```
export JAVA_HOME="/c/Program_Dev/Java/openjdk-17.0.5"
```

To properly execute bash scripts on Windows there are a few alternatives:
- [GIT bash](https://git-scm.com/downloads)
- [Cygwin](https://www.cygwin.com/)
- Windows Subsystem for Linux (WSL)

## Compile and package the benchmarks

### JDK 11

To compile the benchmarks using JDK 11 please run the below command:
```
./mvnw -P jdk11_profile clean spotless:apply package
```

### JDK 17

To compile and package the benchmarks using JDK 17 please run the below command:
```
./mvnw clean spotless:apply package
```
or (using the default, explicit profile):
```
./mvnw -P jdk17_profile clean spotless:apply package
```

## Run the benchmarks (including the setup)

### Dry run

This will generate and print all the commands but without executing any real benchmark. 
```
./run-benchmarks.sh --dry-run | tee run-benchmarks.out
```

### Normal run

```
sudo ./run-benchmarks.sh | tee run-benchmarks.out
```

> sudo is needed to properly apply the OS configuration settings.

You can also redirect the output to a file for later analysis:

```
sudo ./run-benchmarks.sh | tee run-benchmarks.out
```
Each benchmark test suite result is saved under `results/jdk-$JDK_VERSION/$ARCH/$JVM_NAME/$BENCHMARK_NAME.json`

## Generate the benchmarks plots

### Install R/ggplot2

The benchmarks plot generation is based on [R/ggplot2](https://ggplot2.tidyverse.org/) that needs to be installed upfront.

### Generate the plots

To generate all benchmark plots corresponding to one `<jdk-version>` and (optionally,) a specific `<arch>`, run the below command:
```
./plot-benchmarks.sh <jdk-version> [<arch>]
```
If the `<arch>` parameter is omitted, it is automatically detected based on the current target system architecture.

Each benchmark plot is saved under `results/jdk-$JDK_VERSION/$ARCH/$BENCHMARK_NAME.svg`.