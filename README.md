# Java Virtual Machine (JVM) Performance Benchmarks

This repository contains different Java Virtual Machine (JVM) micro-benchmarks for the C2/Graal Just-In-Time (JIT) Compilers.

In addition, there is a small set of benchmarks (i.e., a macro category) covering larger programs (e.g., Fibonacci, Huffman coding/encoding, factorial, palindrome, etc.) using some high-level Java APIs (e.g., streams, lambdas, fork-join, etc.). Nevertheless, this is only complementary but not the main purpose of this work.

The micro-benchmarks are written using [Java Microbenchmark Harness (JMH)](https://github.com/openjdk/jmh).

## Content

- [Authors](#authors)
- [Purpose](#purpose)
- [JMH caveats](#jmh-caveats)
- [OS tuning](#os-tuning)
- [JVM coverage](#jvm-coverage)
- [JDK coverage](#jdk-coverage)
- [Benchmarks suites](#benchmarks-suites)
- [Infrastructure baseline benchmark](#infrastructure-baseline-benchmark)
- [Run the benchmarks suite](#run-the-benchmarks-suite)
- [Benchmark plots](#benchmark-plots)
- [Contribute](#contribute)
- [License](#license)

## Authors

Ionut Balosin
- Website: www.ionutbalosin.com
- Twitter: @ionutbalosin
- Mastodon: ionutbalosin@mastodon.social

Florin Blanaru
- Twitter: @gigiblender
- Mastodon: gigiblender@mastodon.online

## Purpose

The main goal of the project is to assess different JIT Compiler optimizations that are generally available in compilers, such as inlining, loop unrolling, escape analysis, devirtualization, null-check, range-check elimination, dead code elimination, etc. 

Each benchmark focuses on a specific execution pattern that is (potentially fully) optimized under ideal conditions (i.e., clean profiles). Even though some of these patterns might rarely appear directly in the user programs, they could occur after a few optimizations (e.g., inlining of high-level operations). Such conditions might differ in real-life applications, so the benchmarks results are not always a good predictor on a larger scale. Even though the artificial benchmarks might not reveal the entire truth, they tell just enought if properly implemented.

**Out of scope**:
- micro-benchmarking any "syntactic sugar" language feature (e.g., records, sealed classes, local-variable type inference, etc.) 
- micro-benchmarking any Garbage Collector
- benchmarking large applications (e.g., web-based microservices, etc.)
 
> Using micro-benchmarks to gauge the performance of the Garbage Collectors might result in misleading conclusions

## JMH caveats

### HotSpot-specific compiler hints

> JMH uses HotSpot-specific compiler hints to control the Just-in-Time (JIT) compiler. 

For that reason, the fully supported JVMs are all the HotSpot-based VMs, including vanilla OpenJDK and Oracle JDK builds. 
GraalVM is also supported.
For more details please check the [compiler hints](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/runner/CompilerHints.java#L37) and [supported VMs](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/runner/format/SupportedVMs.java#L31).

## OS tuning

When doing benchmarking, it is recommended to disable potential sources of performance non-determinism. Below are described the tuning configurations the benchmark provides for each specific OS.

### Linux

The Linux tuning script [configure-linux-os.sh](./configure-linux-os.sh) triggers all the following:
- set CPU(s) isolation (with `isolcpus` or `cgroups`)
- disable address space layout randomization (ASLR)
- disable turbo boost mode
- set CPU governor to performance
- disable CPU hyper-threading

>Note: these configurations are tested on Ubuntu 22.04 LTS (i.e., a Debian based) Linux distribution.

For further references please check:
- [LLVM benchmarking tips](https://llvm.org/docs/Benchmarking.html#linux)
- [How to get consistent results when benchmarking on Linux?](https://easyperf.net/blog/2019/08/02/Perf-measurement-environment-on-Linux)

### macOS

For macOS, the Linux tunings described above are not applicable. For example, the Apple M1/M2 (ARM-based) chips do not have hyper-threading, nor a turbo-boost mode (i.e., these are specific to the Intel chips). In addition,  [disabling ASLR](https://opensource.apple.com/source/lldb/lldb-76/tools/darwin-debug/darwin-debug.cpp) looks more cumbersome, etc.

Due to these reasons, the script [configure-mac-os.sh](./configure-mac-os.sh) does not enable any specific macOS tuning configuration.

### Windows

Windows is not our main focus therefore the script [configure-win-os.sh](./configure-win-os.sh) does not enable any specific Windows tuning configuration.

## JVM coverage

The table below summarizes the JVM distributions included in the benchmark. For transparency reasons, we provide a short explanation of why the others are not supported.

No. | JVM distribution   | JDK versions |  Included
--- |--------------------|--------------| ----------
1   | OpenJDK HotSpot VM | 11           | Yes
2   | GraalVM CE         | 11           | Yes
3   | GraalVM EE         | 11           | Yes
4   | Eclipse OpenJ9 VM  | N/A          | No, see the resons below
5   | Azul Prime (Zing)  | N/A          | No, see the resons below

### Eclipse OpenJ9 VM

JMH may functionally work with [Eclipse OpenJ9](https://www.eclipse.org/openj9). However, all the [compiler hints](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/annotations/CompilerControl.java) will not apply to Eclipse OpenJ9 and this might lead to different results (i.e., unfair advantage or disadvantage, depending on the test).

For more details please check [JMH with OpenJ9](https://github.com/eclipse-openj9/openj9/issues/4649) and [Mark Stoodley on Twitter](https://twitter.com/mstoodle/status/1532344345524936704)

At the moment we leave it **out of scope** Eclipse OpenJ9 until we find a proper alternative.

### Azul Prime (Zing)

Publishing benchmark results for Azul Prime (Zing) is prohibited by [Azul's Evaluation Agreement](https://www.azul.com/wp-content/uploads/Azul-Platform-Prime-Evaluation-Agreement.pdf). 
It is only allowed to publish results for Azul Prime if prior consent is obtained from Azul.

As of now, we decided to skip this JVM in order to avoid the additional overhead of obtaining such consent.

## JDK coverage

At the moment the benchmark is configured to work only with the JDK Long-Term Support (LTS) versions.

No.| JVM distribution     | JDK versions |  Build
---|----------------------|--------------| -------------------------------
1  | OpenJDK HotSpot VM   | 11           | [download](https://projects.eclipse.org/projects/adoptium.temurin/downloads/)
2  | GraalVM CE           | 11           | [download](https://www.graalvm.org/downloads/)
3  | GraalVM EE           | 11           | [download](https://www.graalvm.org/downloads/)

If there is a need for another JDK LTS version (or feature release), you have to configure it by yourself. 

### Configure JDK

After the JDK was installed, the JDK path needs to be updated in the benchmark configuration scripts.  To do so, open the [configure-jvm.sh](./configure-jvm.sh) script file and update the corresponding **JAVA_HOME** property:
```
export JAVA_HOME="<path_to_jdk>"
```

#### A few examples

Linux OS:
```
export JAVA_HOME="/usr/lib/jvm/openjdk-11.0.5"
```

Mac OS:
```
export JAVA_HOME="/Library/Java/JavaVirtualMachines/openjdk-11.0.5/Contents/Home"
```

Windows OS:
```
export JAVA_HOME="/c/Program_Dev/Java/openjdk-11.0.5"
```

## Benchmarks suites

The benchmarks are organized in suites (i.e., benchmarks suites). To run a benchmark suite on a JDK version it needs a very specific configuration. There are predefined benchmarks suites (in JSON configuration files) for each supported JDK LTS version:

- [benchmarks-suite-jdk11.json](./benchmarks-suite-jdk11.json)

The benchmark will sequentially pick up and execute all the tests from the configuration file.

There are a few reasons why such a custom configuration is needed:

- selectively pass different JVM arguments to subsequent runs of the same benchmark  (e.g., first run with biased locking enabled, second run with biased locking disabled, etc.) 
- selectively pass different JMH options to subsequent runs of the same benchmark (e.g., first run with one thread, second run with two threads, etc.)
- selectively control what benchmarks to include/exclude to/from one JDK version

## Infrastructure baseline benchmark

We provide a baseline benchmark for the infrastructure, [InfrastructureBaselineBenchmark](./benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/InfrastructureBaselineBenchmark.java), that can be used to assess the infrastructure overhead for the code to measure.

It measures the performance of empty methods (w/ and w/o explicit inlining) but also the performance of returning an object versus consuming it via blackholes. All of these mechanisms are used inside the real suite of tests.

This benchmark is particularly useful in case of a comparison between different JVMs and JDKs, and it should be run before any other real benchmark to check the default costs.
In that regard, if the results of the infrastructure baseline benchmark are not the same, it does not make sense to compare the results of the other benchmarks between different
JVMs and JDKs.

## Run the benchmarks suite

Running one benchmark suite triggers the full setup (in a very interactive way, so that the user can choose what steps to skip), as follows:
- configure the OS
- configure the JVM (e.g., set JAVA_HOME, etc.)
- configure the JMH (e.g., choose the benchmark suite for the specific JDK, etc.)
- compile the benchmarks (using a JDK Maven profile)

**Note**: Only for benchmarks compilation please run the below command:
```
./mvnw -P jdk$<jdk-version>_profile clean package
```
where `<jdk-version>` is {11}. If the profile is omitted, JDK profile 11 is implicitly selected.  

Examples:
```
./mvnw clean package
```
```
./mvnw -P jdk11_profile clean package
```

## Elapsed amount of time for each benchmark suite

Each benchmarks suite take a significant amount of time to fully run. For example:

 Benchmark suite            |  Elapsed time
----------------------------|--------------
benchmarks-suite-jdk11.json | ~ 38 hours

### Dry run

Dry run mode goes through and simulates all the commands, but without changing any OS setting, or executing any benchmark. We recommend this as a preliminary check before running the benchmarks.
```
./run-benchmarks.sh --dry-run
```

**Note:** Launch this command with `sudo` to simulate the OS configuration settings. This is needed, even in dry run mode, to read some system configuration files, otherwise not accessible. Nevertheless, it has no side effect on the actual OS settings.

### Normal run

```
./run-benchmarks.sh | tee run-benchmarks.out
```

**Note:** Launch this command with `sudo` to apply the OS configuration settings.

The benchmark results are saved under `results/jdk-$JDK_VERSION/$ARCH/jmh/$JVM_IDENTIFIER` directory.

### Bash scripts on Windows

To properly execute bash scripts on Windows there are a few alternatives:
- [GIT bash](https://git-scm.com/downloads)
- [Cygwin](https://www.cygwin.com/)
- Windows Subsystem for Linux (WSL)

## Benchmark plots

### Install R/ggplot2

The benchmark plot generation is based on [R/ggplot2](https://ggplot2.tidyverse.org/) that needs to be installed upfront.

### Generate the benchmark plots

To generate all benchmark plots corresponding to one `<jdk-version>` and (optionally,) a specific `<arch>`, run the below command:
```
./plot-benchmarks.sh <jdk-version> [<arch>]
```
If the `<arch>` parameter is omitted, it is automatically detected based on the current system architecture.

Before plotting the benchmarks, the script triggers, in addition, a few steps:
- pre-process (e.g., merge, split) some benchmark result files. This is needed to avoid either too fragmented or too sparse generated benchmark plots
- calculate the normalized geometric mean for each benchmark category (e.g., jit, macro). The normalized geometric mean results are saved under `results/jdk-$JDK_VERSION/$ARCH/geomean` directory.

The benchmark plots are saved under `results/jdk-$JDK_VERSION/$ARCH/plot` directory.

# Contribute

If you would like to contribute code (or any other type of support, **including sponsorship**) you can do so through GitHub by sending a pull request, raising an issue with an attached patch, or directly contacting us.

# License 

Please see the [LICENSE](LICENSE) file for full license.

```
JVM Performance Benchmarks

Copyright (C) 2019 - 2023 Ionut Balosin

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at
   
http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
```