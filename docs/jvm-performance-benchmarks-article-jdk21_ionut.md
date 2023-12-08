# JVM Performance Comparison for JDK 21

## Authors

Ionut Balosin
- Website: www.ionutbalosin.com
- X: @ionutbalosin
- Mastodon: ionutbalosin@mastodon.social

Florin Blanaru
- X: @gigiblender
- Mastodon: gigiblender@mastodon.online

# Content

- [Context](#context)
- [SetUp](#setup)
- [JIT Compiler](#jit-compiler)
    - [Benchmarks](#jit-benchmarks)
    - [Geometric Mean](#jit-geometric-mean)
- [API](#api)
    - [Benchmarks](#api-benchmarks)
    - [Geometric Mean](#api-geometric-mean)
- [Miscellaneous](#miscellaneous)
    - [Benchmarks](#miscellaneous-benchmarks)
    - [Geometric Mean](#miscellaneous-geometric-mean)
- [Overall Geometric Mean](#overall-geometric-mean)
- [Final Thoughts](#final-thoughts)
- [References](#references)

# Context

The current article describes a series of Java Virtual Machine (JVM) benchmarks with a primary focus on top-tier Just-In-Time (JIT) Compilers, such as C2 JIT, and Graal JIT.  The benchmarks are structured in three distinct categories:
1. **JIT Compiler**: This category is dedicated to assessing JIT compiler optimizations by following specific handwritten code patterns. It assesses common optimizations found in compilers, including inlining, loop unrolling, escape analysis, devirtualization, null-check elimination, range-check elimination, dead code elimination, etc.
2. **Api**: This category includes benchmarks targeting common APIs from both the Java Platform, Standard Edition (Java SE) (e.g., `java.io`, `java.nio`, `java.net`, `java.security`, `java.util`, `java.text`, `java.time`, etc.) and the Java Development Kit (JDK) (e.g., `jdk.incubator.vector`, etc.).
3. **Miscellaneous**: This category covers a broader spectrum of classical programs (e.g., Dijkstra's shortest path, factorial, Fibonacci, Game of Life, image rotation, knapsack problem, N queens, palindrome, Huffman coding/encoding, Lempel-Ziv-Welch compression, etc.) using different techniques (e.g., dynamic programming, greedy algorithms, backtracking, divide and conquer, etc.), various programming styles (e.g., iterative, functional), and high-level Java APIs (e.g., streams, lambdas, fork-join, collections, etc.).

For some of the benchmarks (i.e., where there are notable differences) we provide an in-depth analysis (i.e., optimized generated assembly code, flame graphs, etc.).

The list of included JIT compilers is:
- C2 (Server) JIT
- Graal JIT

The list of included JMVs is:
- OpenJDK 21
- GraalVM Community Edition (CE) 21+35.1
- Oracle GraalVM Edition 21+35.1

The list of included architectures is:
- x86_64
- arm64

This article is based on the [jvm-performance-benchmarks](https://github.com/ionutbalosin/jvm-performance-benchmarks) project. For any further information (not explicitly mentioned here) including the OS tuning settings, the deliberate decision on choosing these JVMs and/or specific JDKs, etc., please check out the repository.

> The reference architecture (and, subsequently, the benchmarks analysis) for this article is x86_64, and arm64 is only included in the geometric mean score section for comparison.

# SetUp

- Java Development Kit (JDK) 21
- Java Microbenchmark Harness (JMH) v1.37
- each benchmark uses 5x10s warm-up iterations, 5x10s measurement iterations, and 5 JVM forks, both single-threaded but also multi-threaded workloads (depending on the test case)
- the benchmarks were run on the below machines:
    1. Apple MacBook Pro, M1 Chip 10-Core, 16-Core Neural Engine, 32GB RAM, macOS Ventura 13.6.1
    2. Dell XPS 15 7590, Intel Core i7-9750H 6-Core, 32GB RAM, Ubuntu 20.04 LTS
- to eliminate potential sources of performance non-determinism, the below OS tunings were performed on the Intel machine:
    1. disabled the turbo-boost mode
    2. set CPU governor to _performance_
    3. disabled CPU hyper-threading

# JIT Compiler

This section describes the results obtained from running the JIT benchmarks. The current benchmarks focus on various optimizations that are generally available in compilers, such as inlining, loop unrolling, escape analysis, devirtualization, null-check, and range-check elimination, dead code elimination, etc.

The list of JIT compilers (including the JVM and the architecture) is:

JIT       | JVM                    | Arcitecture 
----------|------------------------|----------------
C2 JIT    | OpenJDK 21             | x86_64, arm64         
Graal JIT | GraalVM CE 21+35.1     | x86_64, arm64      
Graal JIT | Oracle GraalVM 21+35.1 | x86_64, arm64      

## JIT Benchmarks

The JIT benchmarks are measured in [average time per operation](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/annotations/Mode.java#L52), which is the score reported by the JMH.

## InfrastructureBaselineBenchmark

This benchmark is used as a baseline (i.e., a preliminary check) to assess the infrastructure overhead for the code to measure.
Since no magical infrastructures are incurring no overhead, it is essential to know what default overheads might occur in our setup.

It measures the calls performance of empty methods (w/ and w/o explicit inlining) but also the performance of returning an object versus consuming it via black holes. All of these mechanisms are used by the benchmark suite.

This is particularly useful in case of a comparison between different types of JVMs, and it should be run before any other real benchmark to check the default costs.

> A comparison between different JVMs might not be further relevant unless, at least, the baseline is the same.

Source code: [InfrastructureBaselineBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/InfrastructureBaselineBenchmark.java)

[![InfrastructureBaselineBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/InfrastructureBaselineBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/InfrastructureBaselineBenchmark.svg?raw=true)

## DeadMethodCallStoreBenchmark

The benchmark assesses how the compiler could remove code (i.e., a dead method call store) that does not affect the program results.

```
  @Benchmark
  public double method_call_dse() {
    double pi;
    pi = computePi();
    pi = computePi();
    pi = computePi();
    pi = computePi();
    pi = computePi();
    return circleRadius * circleRadius * pi;
  }
  
  @Benchmark
  public double method_call_baseline() {
    double pi = computePi();
    return circleRadius * circleRadius * pi;
  }
  
  // Leibniz formula for π
  private static double computePi() {
    double pi = 4;
    boolean sign = false;

    for (int i = 3; i < 1_000; i += 2) {
      if (sign) {
        pi += 4.0 / i;
      } else {
        pi -= 4.0 / i;
      }
      sign = !sign;
    }

    return pi;
  }  
```

Source code: [DeadMethodCallStoreBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/compiler/DeadMethodCallStoreBenchmark.java)

[![DeadMethodCallStoreBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/DeadMethodCallStoreBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/DeadMethodCallStoreBenchmark.svg?raw=true)

### Analysis

The analysis below pertains to the `method_call_dse` method, which is the primary focus of the benchmark.

#### C2 JIT Compiler

The C2 JIT compiler optimizes the code by eliminating dead store method calls and applying inlining to the `computePi` method. Inside the loop, there's a boolean flag determining whether to add or subtract the value `4.0 / i` to the result. However, the compiler optimizes this logic, removing the conditional statements.

Additionally, it unrolls the main loop by a factor of 16, thereby handling 16 operations per unrolled loop cycle. To compute the results, the compiler leverages double-precision scalar floating-point instructions with SIMD capabilities, utilizing SSE/AVX extensions.

```
  // Main loop (from 0x5 ... to 0x3e2)
  
  0x7f044c636b7a:   mov    $0x5,%r9d                         ; r9d = 5 (loop counter)
                                                             ; <--- loop begin
  0x7f044c636ba0:   vaddsd %xmm1,%xmm0,%xmm0                 ; xmm0 = xmm0 + xmm1
  0x7f044c636ba4:   mov    %r10d,%r9d                        ; r9d = r10d 
  0x7f044c636ba7:   lea    0x1e(%r9),%r11d                   ; r11d = r9 + 0x1e (i.e., offset 30)
  0x7f044c636bab:   lea    0x1a(%r9),%r8d                    ; r8d = r9 + 0x1a (i.e., offset 26)
  0x7f044c636baf:   vcvtsi2sd %r11d,%xmm1,%xmm1              ; convert r11d to double and store in xmm1
  0x7f044c636bb4:   vdivsd %xmm1,%xmm2,%xmm3                 ; xmm3 = xmm2 / xmm1
                                                             ; xmm2 stores the 4.0 value
  0x7f044c636bb8:   vcvtsi2sd %r8d,%xmm1,%xmm1               ; convert r8d to double and store in xmm1
  0x7f044c636bbd:   vdivsd %xmm1,%xmm2,%xmm4                 ; xmm4 = xmm2 / xmm1
  ...
  <--- similar for offsets 0x1c, 0x18, 0x10, 0x8 -->
  <--- store results in xmm5, xmm6, xmm7, xmm8 -->
  ...
  0x7f044c636bf5:   lea    0x2(%r9),%r11d // 2               ; set r11d = r9 + 0x2 (i.e., offset 2)
  0x7f044c636bf9:   lea    0x4(%r9),%r8d  // 4               ; set r8d = r9 + 0x4 (i.e., offset 4)
  0x7f044c636bfd:   vcvtsi2sd %r11d,%xmm1,%xmm1              ; convert r11d to double and store in xmm1
  0x7f044c636c02:   vdivsd %xmm1,%xmm2,%xmm1                 ; xmm1 = xmm2 / xmm1
  0x7f044c636c06:   vsubsd %xmm1,%xmm0,%xmm0                 ; xmm0 = xmm0 - xmm1
  0x7f044c636c0a:   vcvtsi2sd %r8d,%xmm1,%xmm1               ; convert r8d to double and store in xmm1
  0x7f044c636c0f:   vdivsd %xmm1,%xmm2,%xmm1                 ; xmm1 = xmm2 / xmm1
  0x7f044c636c13:   vaddsd %xmm1,%xmm0,%xmm0                 ; xmm0 = xmm0 + xmm1
  ...
  <--- similar for offsets 0x6, 0xa, 0xc, 0xe, 0x12, 0x14, 0x16, 0x20 -->
  <--- the results are alternatively stored in xmm0, xmm1 -->
  ...
  0x7f044c636ca6:   lea    0x20(%r9),%r10d                   ; set r10d = r9 + 0x20 (i.e., offset 32)
  0x7f044c636caa:   vcvtsi2sd %r10d,%xmm1,%xmm1              ; convert r10d to double and store in xmm1
  0x7f044c636caf:   vdivsd %xmm1,%xmm2,%xmm1                 ; xmm1 = xmm2 / xmm1
  0x7f044c636cc0:   cmp    $0x3e2,%r10d                      ; compare against 994
  0x7f044c636cc7:   jl     0x7f044c636ba0                    ; <--- loop end (loop back if less)
  ; xmm1 stores the result of the main loop
```

The post-loop processes the remaining elements individually, without unrolling:

```
  // Post loop (0x3e2 ... 0x3e8)
  
  0x7f044c636ccd:   add    $0x20,%r9d            ; r9d = r9d + 0x20 (loop counter)
  0x7f044c636cd1:   mov    $0x1,%r10d            ; r10d = 1
                                                 ; <--- loop begin
  0x7f044c636cd7:   test   %r10d,%r10d           ; check if r10d is zero
  0x7f044c636cda:   je     0x7f044c636ce2        ; jump if r10d == 0
  0x7f044c636cdc:   vaddsd %xmm1,%xmm0,%xmm0     ; xmm0 = xmm0 + xmm1
  0x7f044c636ce0:   jmp    0x7f044c636ce6
  0x7f044c636ce2:   vsubsd %xmm1,%xmm0,%xmm0     ; xmm0 = xmm0 - xmm1
  0x7f044c636ce6:   add    $0x2,%r9d             ; r9d = r9d + 2 (increment by loop stride)
  0x7f044c636cea:   cmp    $0x3e8,%r9d           ; compare against 1000
  0x7f044c636cf1:   jge    0x7f044c636d02        ; exit loop if greater or equal
  0x7f044c636cf3:   vcvtsi2sd %r9d,%xmm1,%xmm1   ; convert r9d to double and store in xmm1
  0x7f044c636cf8:   vdivsd %xmm1,%xmm2,%xmm1     ; xmm1 = xmm2 / xmm1
  0x7f044c636cfc:   xor    $0x1,%r10d            ; r10d = r10d XOR 1 (i.e., toggle r10d between 0 and 1)
  0x7f044c636d00:   jmp    0x7f044c636cd7        ; <--- loop end (jump loop back)
  ; xmm1 stores the result of the post loop
```

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM compiler optimizes code by eliminating redundant method calls and applying inlining to the `computePi` method, much like the C2 JIT.

It achieves optimization by unrolling the main loop by a factor of 8, allowing 8 operations to be handled per unrolled loop cycle using double-precision scalar floating-point instructions.

```
  // Main loop (0x5 ... 0x3d8)
  
  0x7f0ac2d7d86a:   mov    $0x5,%r10d                   ; r10d = 5 (loop counter)
                                                        ; <--- loop begin
  0x7f0ac2d7d880:   lea    0x10(%r10),%r11d             ; r11d = r10 + 0x10 (i.e., offset 16)
  0x7f0ac2d7d884:   vxorpd %xmm2,%xmm2,%xmm2            ; zero out xmm2
  0x7f0ac2d7d888:   vcvtsi2sd %r10d,%xmm2,%xmm2         ; convert r10d to double and store in xmm2
  0x7f0ac2d7d88d:   vdivsd %xmm2,%xmm0,%xmm2            ; xmm2 = xmm0 / xmm2
  0x7f0ac2d7d891:   vaddsd %xmm2,%xmm1,%xmm1            ; xmm1 = xmm1 + xmm2
  
  0x7f0ac2d7d895:   lea    0x2(%r10),%r8d               ; r8d = r10 + 0x2 (i.e., offset 2)
  0x7f0ac2d7d899:   vxorpd %xmm2,%xmm2,%xmm2            ; zero out xmm2
  0x7f0ac2d7d89d:   vcvtsi2sd %r8d,%xmm2,%xmm2          ; convert r8d to double and store in xmm2
  0x7f0ac2d7d8a2:   vdivsd %xmm2,%xmm0,%xmm2            ; xmm2 = xmm0 / xmm2
  0x7f0ac2d7d8a6:   vsubsd %xmm2,%xmm1,%xmm1            ; xmm1 = xmm1 - xmm2
  ...
  <--- similar for offsets 0x4, 0x6, 0x8, 0xa, 0xc, 0xe -->
  <--- the results are alternatively stored in xmm0, xmm1 -->
  ...
  0x7f0ac2d7d928:   mov    %r11d,%r10d                  ; r10d = r11d 
  0x7f0ac2d7d92b:   cmp    $0x3d8,%r10d                 ; compare against 984 
  0x7f0ac2d7d932:   jb     0x7f0ac2d7d880               ; <--- loop end (loop back if below)
  ; xmm1 stores the result of the main loop
```

The post-loop processes the remaining elements individually, without unrolling:

```
  // Post loop (0x3d8 ... 0x3e8)

  0x7f0ac2d7d938:   mov    %r10d,%r11d            ; r11 = r10 (loop counter)
  0x7f0ac2d7d93b:   mov    $0x1,%r10d             ; r10d = 0x1
                                                  ; <--- loop begin
  0x7f0ac2d7d950:   cmp    $0x3e8,%r11d           ; compare against 1000
  0x7f0ac2d7d957:   jae    0x7f0ac2d7d996         ; exit loop if greater or equal
  0x7f0ac2d7d95d:   lea    0x2(%r11),%r8d         ; r8d = r11 + 2 (increment by loop stride)
  0x7f0ac2d7d961:   mov    %r10d,%r9d             ; r9d = r10d
  0x7f0ac2d7d964:   xor    $0x1,%r9d              ; toggle the least significant bit of r9d (i.e., r10d)
  0x7f0ac2d7d968:   vxorpd %xmm2,%xmm2,%xmm2      ; zero out xmm2
  0x7f0ac2d7d96c:   vcvtsi2sd %r11d,%xmm2,%xmm2   ; convert r11d to double and store in xmm2
  0x7f0ac2d7d971:   vdivsd %xmm2,%xmm0,%xmm2      ; xmm2 = xmm0 / xmm2
  0x7f0ac2d7d975:   test   %r10d,%r10d            ; check r10d is zero
  0x7f0ac2d7d978:   jne    0x7f0ac2d7d98a         ; jump if r10d != 0 (for subtraction)
  0x7f0ac2d7d97e:   vsubsd %xmm2,%xmm1,%xmm1      ; xmm1 = xmm1 - xmm2
  0x7f0ac2d7d982:   mov    %r8d,%r11d             ; r11d = r8d
  0x7f0ac2d7d985:   mov    %r9d,%r10d             ; r10d = r9d
  0x7f0ac2d7d988:   jmp    0x7f0ac2d7d950         ; <--- loop end (subtract loop)
  0x7f0ac2d7d98a:   vaddsd %xmm2,%xmm1,%xmm1      ; xmm1 = xmm1 + xmm2
  0x7f0ac2d7d98e:   mov    %r8d,%r11d             ; r11d = r8d
  0x7f0ac2d7d991:   mov    %r9d,%r10d             ; r10d = r9d
  0x7f0ac2d7d994:   jmp    0x7f0ac2d7d950         ; <--- loop end (addition loop)
  ; xmm1 stores the result of the post loop
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler cannot completely remove the loops associated with the dead store method calls; hence, they persist as dead loops.

```
  Dead loop (appears four times; i.e., equivalent to the number of dead method calls)

  0x7f94ab23c48d:   mov    $0x5,%r10d                ; r10 = 5  (loop counter)
                                                     ; <--- loop begin
  0x7f94ab23c4a0:   lea    0x2(%r10),%r10d           ; r10d = r10 + 2 (increment by loop stride)
  0x7f94ab23c4a4:   cmp    $0x3e8,%r10d              ; compare against 1000
  0x7f94ab23c4ab:   jl     0x7f94ab23c4a0            ; <--- loop end (loop back if less than 1000)
```

Subsequently, the loop corresponding to the non-dead store method call is not unrolled.

```
  0x7f94ab23c4fd:   mov    $0x1,%r11d               ; r11 = 1
  0x7f94ab23c503:   mov    $0x5,%r10d               ; r10 = 5  (loop counter)
                                                    ; <--- loop begin
  0x7f94ab23c510:   cmp    $0x3e8,%r10d             ; compare against 1000
  0x7f94ab23c517:   jge    0x7f94ab23c54c           ; exit loop if greater or equal
  0x7f94ab23c51d:   vcvtsi2sd %r10d,%xmm2,%xmm2     ; convert r10d to double and store in xmm2
  0x7f94ab23c522:   vdivsd %xmm2,%xmm0,%xmm2        ; xmm2 = xmm0 / xmm2
  0x7f94ab23c526:   mov    %r11d,%r8d               ; r8 = r11
  0x7f94ab23c529:   xor    $0x1,%r8d                ; r8 = r8 XOR 1 (toggle the least significant bit of r8)
  0x7f94ab23c52d:   lea    0x2(%r10),%r10d          ; r10d = r10 + 2 (increment by loop stride)
  0x7f94ab23c531:   test   %r11d,%r11d              ; check r11d is zero
  0x7f94ab23c534:   jne    0x7f94ab23c543           ; jump if r11d != 0 (for subtraction)
  0x7f94ab23c53a:   vsubsd %xmm2,%xmm1,%xmm1        ; xmm1 = xmm1 - xmm2
  0x7f94ab23c53e:   mov    %r8d,%r11d
  0x7f94ab23c541:   jmp    0x7f94ab23c510           ; <--- loop end (subtract loop)
  0x7f94ab23c543:   vaddsd %xmm1,%xmm2,%xmm1        ; xmm1 = xmm1 + xmm2
  0x7f94ab23c547:   mov    %r8d,%r11d
  0x7f94ab23c54a:   jmp    0x7f94ab23c510           ; <--- loop end (addition loop)
  ; xmm1 stores the result of the loop
```

### Conclusions

- The GraalVM CE JIT exibits suboptimal optimizations in this scenario, lacking loop unrolling and unable to eliminate loops related to dead method calls.
- Oracle GraalVM JIT successfully eliminates the dead method calls and triggers loop unrolling by a factor of 8, handling the remaining elements in a post loop without unrolling.
- C2 JIT also eliminates dead method calls, performing loop unrolling by a factor of 16 for the main loop and handling the remaining elements in a post loop without unrolling.

Despite employing a more aggressive unrolling approach, C2 JIT does not necessarily outperform Oracle GraalVM JIT. Even though both JITs rely on double-precision scalar floating-point instructions with SIMD capabilities via SSE/AVX extensions, Oracle GraalVM JIT may issue more optimized instructions for the hardware used. However, a thorough examination involving advanced profiling and detailed code analysis could provide a better understanding of these differences.

## EnumValuesLookupBenchmark

This benchmark iterates through the enum values list and returns the enum constant matching a lookup value. It emphasizes the cost of calling the values() method on an eum versus caching the values and using the cached version.

This pattern is often seen in real business applications where, for example, the microservices RESTful APIs defined in OpenAPI/Swagger use enums.
The input request parameters are deserialized and wrapped to enum values.

In order to match the input parameter to the enum value, `String.equals()` comparison is used.

```
  private static final Car[] cachedCars = Car.values();
  
  @Benchmark
  public Car cached_enum_values() {
    return Car.fromCachedValues(lookUpValue);
  }

  @Benchmark
  public Car enum_values() {
    return Car.fromValues(lookUpValue);
  }
  
  public enum Car {
    private String carValue;
    // ...
  }   
  
  public static Car[] cachedValues() {
    return cachedCars;
  }
  
  public static Car fromValues(String targetValue) {
    for (Car b : Car.values()) {
      if (b.carValue.equals(targetValue)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + targetValue + "'");
  }

  public static Car fromCachedValues(String targetValue) {
    for (Car b : Car.cachedValues()) {
      if (b.carValue.equals(targetValue)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + targetValue + "'");
  }
```

Source code: [EnumValuesLookupBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/api/EnumValuesLookupBenchmark.java)

[![EnumValuesLookupBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/EnumValuesLookupBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/EnumValuesLookupBenchmark.svg?raw=true)

### Analysis of cached_enum_values

The analysis below pertains to the `cached_enum_values` method, which is the primary focus of the benchmark.

#### C2 JIT Compiler

The C2 JIT Compiler iterates through the array of cached enum values, attempting to locate a match for the `lookUpValue` by utilizing the `String::equals` method.

```
  0x7f2fe44fa213:   mov    $0x2,%r10d                   ; Initialize loop counter r10d = 2
  0x7f2fe44fa219:   jmp    0x7f2fe44fa22d
                                                        ; <--- loop begin
  0x7f2fe44fa220:   inc    %r10d                        ; increment loop counter
  0x7f2fe44fa223:   cmp    $0x29,%r10d                  ; compare against 41 (enum values array length)
  0x7f2fe44fa227:   jge    0x7f2fe44fa388               ; jump if greater than or equal to exit loop
  0x7f2fe44fa22d:   mov    0x10(%rdx,%r10,4),%ebx       ; load Car object from enum values array at index [rdx + r10 * 4 + 16]
  0x7f2fe44fa232:   mov    0x18(%r12,%rbx,8),%edi       ; load 'carValue' field from the Car object at offset 0x18
  ...
  <--- Trigges the comparison between 'carValue' and 'lookUpValue' value -->
  ...
  0x7f2fe44fa269:   jne    0x7f2fe44fa220               ; <--- loop end (loop back if not equal)
                                                        ; - java.lang.String::equals
```

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM JIT iterates through the array of cached enum values. However, when attempting to compare the `lookUpValue` value against the enum values (i.e., String comparisons), it utilizes an intrinsic candidate for the byte array values.

```
                                                         ; <--- loop begin
  0x07f9bb6d9c2e0:   cmp    $0x29,%r10d                  ; compare against 41 (enum values array length)
  0x07f9bb6d9c2e4:   jae    0x07f9bb6d9c42c              ; jump if greater than or equal to 41 (exit the loop)
  0x07f9bb6d9c2ea:   mov    0x10(%rbx,%r10,4),%edi       ; load Car object from enum values array at index [rbx + r10 * 4 + 0x10]
  0x07f9bb6d9c2ef:   mov    0x18(,%rdi,8),%esi           ; load 'carValue' field from the Car object at offset 0x18
  0x07f9bb6d9c2f6:   movsbl 0x10(,%rsi,8),%ecx           ; extract the String coder at offset 0x10
  0x07f9bb6d9c2fe:   mov    0x14(,%rsi,8),%r8d           ; get the String byte array at offset 0x14
  ...
  0x07f9bb6d9c312:   inc    %eax
  ...
  0x07f9bb6d9c329:   cmp    0xc(,%r8,8),%ebp             ; compare the String byte array lengths
  0x07f9bb6d9c331:   jne    0x07f9bb6d9c37d              ; jump if lengths are not the same
  ...
  <--- Trigger the comparison of two byte array regions using an intrinsic stub -->
  0x07f9bb6d9c359:   call   0x07f9bb6910580              ; call the runtime function for byte array region comparison
                                                         ; {runtime_call Stub&lt;IntrinsicStubsGen.arrayRegionEqualsS1S1&gt;}
  0x07f9bb6d9c360:   test   %eax,%eax                    ; test the result of the byte array region comparison
  0x07f9bb6d9c362:   jne    0x07f9bb6d9c385              ; jump if ZF is not set (i.e., arrays are equal)
  ...
  0x07f9bb6d9c37d:   mov    %eax,%r10d                   ; r10d = eax
  0x07f9bb6d9c380:   jmp    0x07f9bb6d9c2e0              ; <--- loop end (loop back if arrays are not equal)
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler employs a similar approach to the Oracle GraalVM JIT Compiler for comparing Strings, using the intrinsic stub to compare byte arrays. However, the reported average response is marginally slower. A more comprehensive analysis, including profiling, may be required to provide additional insights.

### Analysis of enum_values

Regarding the other `enum_values` benchmark, it is impacted by the object allocations triggered by the `Enum::values` call that is an invoke virtual the to clone() method.

The following snapshot is from the C2 JIT Compiler output, although the same pattern occurs for the Oracle GraalVM JIT Compiler and GraalVM CE JIT Compiler.

```
                                                         ; <--- (TLAB) Allocation (and initialization)
  0x07f77984f8d83:   mov    0x1b8(%r15),%r13             ; r13 = r15 + 0x1b8 (offset calculation)
  0x07f77984f8d8a:   mov    %r13,%r10                    ; r10 = r13 (store calculated offset)
  0x07f77984f8d8d:   add    $0xb8,%r10                   ; r10 = r10 + 0xb8 (offset for comparison)
  0x07f77984f8da0:   cmp    0x1c8(%r15),%r10             ; compare r10 with r15 + 0x1c8
  0x07f77984f8da7:   jae    0x07f77984f9091              ; jump if above or equal, indicating no more space for allocation
  0x07f77984f8dad:   mov    %r10,0x1b8(%r15)             ; store the offset back into memory for future allocations
  0x07f77984f8db4:   prefetchw 0xc0(%r10)                ; prefetch memory
  0x07f77984f8dbc:   movq   $0x1,0x0(%r13)               ; store mark word header (initializing object)
  0x07f77984f8dc4:   prefetchw 0x100(%r10)               ; prefetch memory
  0x07f77984f8dcc:   movl   $0x10299f0,0x8(%r13)         ; store klass word header (initializing object)
                                                         ; {metadata(&apos;EnumValuesLookupBenchmark$Car&apos;[])}
  0x07f77984f8dd4:   prefetchw 0x140(%r10)               ; prefetch memory for future use
  0x07f77984f8ddc:   movl   $0x29,0xc(%r13)              ; store the enum values array length
  0x07f77984f8de4:   prefetchw 0x180(%r10)               ; prefetch memory
                                                         ; <--- (TLAB) Allocation end
  ...
  0x07f77984f8dec:   mov    %r13,%rsi
  0x07f77984f8def:   add    $0x10,%rsi
  0x07f77984f8df3:   mov    $0x15,%edx
  0x07f77984f8df8:   movabs $0x62ae275c0,%r10            ; load the enum values array address into register r10
                                                         ; {oop(a &apos;EnumValuesLookupBenchmark$Car&apos;[41] {0x062ae275c0})}
  0x07f77984f8e02:   movabs $0x62ae275c0,%rbx            ; load the enum values array address into register rbx
                                                         ; {oop(a &apos;EnumValuesLookupBenchmark$Car&apos;[41] {0x062ae275c0})}
  0x07f77984f8e0c:   add    $0x10,%rbx
  0x07f77984f8e10:   mov    %rbx,%rdi
  0x07f77984f8e16:   movabs $0x7f7797f6a400,%r10
  0x07f77984f8e20:   call   *%r10                        ; function call to handle enum values
  0x07f77984f8e2b:   mov    (%rbx),%r11d                 ;*invokevirtual clone
                                                         ; - EnumValuesLookupBenchmark$Car::values
```

The hottest regions in the report shows the `StubRoutines::jlong_disjoint_arraycopy`, which is the stub generator for copying long arrays when the source and destination arrays do not overlap. This routine is a result of an `Enum::values` call.

```
  ....[Hottest Regions]...............................................................................
  62.25%           c2, level 4  EnumValuesLookupBenchmark::enum_values, version 2, compile id 643
  13.47%           c2, level 4  EnumValuesLookupBenchmark::enum_values, version 2, compile id 643
  9.31%           runtime stub  StubRoutines::jlong_disjoint_arraycopy
  3.94%            c2, level 4  EnumValuesLookupBenchmark::enum_values, version 2, compile id 643
```

### Conclusions

- Try to avoid calling `Enum::values`, especially within a loop, as it allocates a new array and assigns references to the enum values as elements. This can potentially generate a considerable amount of garbage.
- When comparing enum values to strings, which involves string comparisons in essence, the Oracle GraalVM JIT outperforms the C2 JIT Compiler in this benchmark. One significant factor is the utilization of an intrinsic method by the Oracle GraalVM JIT, enabling it to check if two strings are equal within a defined region, specified by a codepoint-based offset and length. Further details regarding this type of optimization can be found in the document [TruffleStrings: a Highly Optimized Cross-Language String Implementation](https://graalworkshop.github.io/2022/slides/4_TruffleStrings.pdf).

## IfConditionalBranchBenchmark

This benchmark tests the conditional branch optimizations within a loop using:
- a predictable branch pattern
- an unpredictable branch pattern
- no branch at all

```
  // All array[] values are randomly generated within [0, THRESHOLD)

  @Benchmark
  public int no_if_branch() {
    int sum = 0;

    for (final int value : array) {
      sum += value;
    }

    return sum;
  }
  
  @Benchmark
  public int predictable_if_branch() {
    int sum = 0;

    for (final int value : array) {
      if (value < THRESHOLD) {
        sum += value;
      }
    }

    return sum;
  }
  
  @Benchmark
  public int unpredictable_if_branch() {
    int sum = 0;

    for (final int value : array) {
      if (value <= (THRESHOLD / 2)) {
        sum += value;
      }
    }

    return sum;
  }  
```

Source code: [IfConditionalBranchBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/compiler/IfConditionalBranchBenchmark.java)

[![IfConditionalBranchBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/IfConditionalBranchBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/IfConditionalBranchBenchmark.svg?raw=true)

### Analysis of no_if_branch

#### C2 JIT Compiler

The C2 JIT Compiler unrolls the main loop by a factor of 16, thereby handling 16 additions per unrolled loop cycle.

```
  // Main loop
  
  0x7f374863acda:   mov    0x18(%rsi),%ebp              ; get the array pointer
  0x7f374863ace0:   mov    0xc(%r12,%rbp,8),%r11d       ; get the array length
  0x7f374863ad06:   mov    0x10(%r12,%rbp,8),%eax       ; load the value of the first element
  0x7f374863ad0b:   lea    (%r12,%rbp,8),%r8            ; get the array address
  0x7f374863ad13:   mov    $0x1,%ecx                    ; initialize loop counter
  0x7f374863ad2f:   mov    %r11d,%edx                   ; store array length
                                                        ; <--- loop begin
  0x7f374863ad50:   add    0x10(%r8,%rcx,4),%eax        ; add the value of the 1st element
  0x7f374863ad55:   add    0x14(%r8,%rcx,4),%eax        ; add the value of the 2nd element
  ...
  <--- additional lines for adding other elements -->
  ...
  0x7f374863ad96:   add    0x48(%r8,%rcx,4),%eax        ; add the value of the 15th element
  0x7f374863ad9b:   add    0x4c(%r8,%rcx,4),%eax        ; add the value of the 16th element
  0x7f374863ada0:   add    $0x10,%ecx                   ; increment loop counter by 16
  0x7f374863ada3:   cmp    %edx,%ecx                    ; compare loop counter with array length
  0x7f374863ada5:   jl     0x7f374863ad50               ; <--- loop end (loop back if less)
  ; eax stores the result of the main loop  
```

The post-loop processes the remaining elements individually, without unrolling:

```
  // Post loop
                                                        ; <--- loop begin
  0x7f374863adc0:   add    0x10(%r8,%rcx,4),%eax        ; add value of an element to eax
  0x7f374863adc5:   inc    %ecx                         ; increment loop counter
  0x7f374863adc7:   cmp    %r11d,%ecx                   ; compare loop counter with array length
  0x7f374863adca:   jl     0x7f374863adc0               ; <--- loop end (loop back if less)
  ; eax stores the result of the post loop  
```

#### Oracle GraalVM JIT Compiler

Oracle GraalVM JIT Compiler does to sum of elements array using vectorized instructions that operate on 256-bit wide AVX (Advanced Vector Extensions) registers, thereby handling 8 additions per unrolled loop cycle.

```
  // Main loop

  0x7fa68ad81b3f:   mov    0x18(%rsi),%eax              ; get the array pointer
  0x7fa68ad81b42:   mov    0xc(,%rax,8),%r10d           ; get the array length
  0x7fa68ad81b53:   shl    $0x3,%rax                    ; compressed oops (shift left by 3 for addressing)
  0x7fa68ad81b57:   lea    0x10(%rax),%rax              ; get the address of the array
  0x7fa68ad81b6a:   lea    -0x8(%r10),%r11              ; calculate (array length - 8)
  0x7fa68ad81b6e:   mov    $0x0,%r8                     ; initialize loop counter
                                                        ; <--- Loop begin
  0x7fa68ad81b80:   vmovdqu (%rax,%r8,4),%ymm1          ; load 256 bits (8 integers) into %ymm1
  0x7fa68ad81b86:   vpaddd %ymm1,%ymm0,%ymm0            ; packed integer addition: %ymm0 = %ymm0 + %ymm1
  0x7fa68ad81b8a:   lea    0x8(%r8),%r8                 ; increment loop counter by 8
  0x7fa68ad81b8e:   cmp    %r11,%r8                     ; compare loop counter with (array length - 8)
  0x7fa68ad81b91:   jle    0x7fa68ad81b80               ; <--- loop end (jump loop back if less)
  ; ymm0 stores the result of the main loop  
```

The post-loop processes the remaining elements individually, without unrolling.

```
  // Post loop
  
  ...
  <--- transfers data from ymm0 (256-bit AVX register) into r11d (32-bit register) -->
  ...
  ;                                                     <--- Loop begin
  0x7fa68ad81bc0:   add    (%rax,%r8,4),%r11d           ; add the value of an element to r11d
  0x7fa68ad81bc4:   inc    %r8                          ; increment the loop counter
  0x7fa68ad81bc7:   cmp    %r10,%r8                     ; compare the loop counter with the array length
  0x7fa68ad81bca:   jle    0x7fa68ad81bc0               ; <--- Loop end (Jump back if less)
  ; r11d stores the result of the post loop  
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler optimizes the main loop by unrolling it with a factor of 16, enabling 16 additions to be processed per unrolled loop cycle. However, some registers are spilled onto the stack.

```
  // Main loop
  0x7f22eb23d83f:   mov    0x18(%rsi),%eax              ; get the array pointer
  0x7f22eb23d842:   mov    0xc(,%rax,8),%r10d           ; get the array length
  0x7f22eb23d85c:   mov    %r10d,%r8d                   ; copy array length to r8d
  0x7f22eb23d889:   shl    $0x3,%rax                    ; compressed oops (shift left by 3 for addressing)
  ...
  0x7f22eb23d890:   mov    $0x1,%r11d                   ; initialize r11d for loop count
  <--- Loop peeling -->
  0x7f22eb23d8a5:   inc    %r11d                        ; increment loop counter (r11d)
  ...
  0x7f22eb23d8c0:   mov    %r11d,%r9d                   ; initialize loop counter r9d from r11d
  ...
                                                            ; <--- Loop beginning
  0x7f22eb23d8e0:   add    0x10(%rax,%r9,4),%r11d       ; add value of the 1st element to r11d
  0x7f22eb23d8e5:   movslq %r9d,%rcx                    ; sign-extend r9d to rcx for addressing
  0x7f22eb23d8e8:   mov    0x14(%rax,%rcx,4),%ebx       ; store the value of the 2nd element in ebx
  0x7f22eb23d8ec:   mov    0x18(%rax,%rcx,4),%edi       ; store the value of the 3rd element in edi
  ...
  0x7f22eb23d910:   mov    %r9d,0x1c(%rsp)              ; spill r9d (the loop counter) onto the stack
  0x7f22eb23d915:   mov    0x38(%rax,%rcx,4),%r9d       ; store the value of the 11th element in r9d
  0x7f22eb23d91a:   mov    %r9d,0x18(%rsp)              ; spill the 11th element (r9d) onto the stack
  0x7f22eb23d91f:   mov    0x3c(%rax,%rcx,4),%r9d       ; store the value of the 12th element in r9d
  0x7f22eb23d924:   mov    %r9d,0x14(%rsp)              ; spill the 12th element (r9d) onto the stack
  ...
  0x7f22eb23d93d:   mov    0x48(%rax,%rcx,4),%r9d       ; store the value of the 15th element in r9d
  0x7f22eb23d942:   add    %ebx,%r11d                   ; add the 2nd element to r11d
  0x7f22eb23d945:   add    %edi,%r11d                   ; add the 3rd element to the previous sum (r11d)
  ...
  0x7f22eb23d95d:   add    0x18(%rsp),%r11d             ; load the 11th element from stack and add to r11d
  0x7f22eb23d962:   add    0x14(%rsp),%r11d             ; load the 12th element from stack and add to r11d
  ...
  0x7f22eb23d971:   add    %r9d,%r11d                   ; add the 15th element to the sum (r11d)
  0x7f22eb23d974:   add    0x4c(%rax,%rcx,4),%r11d      ; add the 16th element to the sum (r11d)
  0x7f22eb23d979:   mov    0x1c(%rsp),%r9d              ; restore the loop counter from the stack
  0x7f22eb23d97e:   lea    0x10(%r9),%r9d               ; increment loop counter by 16
  0x7f22eb23d982:   mov    0x20(%rsp),%r8d              ; move saved value (i.e., array length) from stack to r8d
  0x7f22eb23d987:   cmp    %r9d,%r8d                    ; compare loop counter with saved value
  0x7f22eb23d98a:   jg     0x7f22eb23d8e0           ; <--- loop end (jump loop back if greater)
  ; r11d stores the result of the main loop
```

The post-loop processes the remaining elements individually, without unrolling.

```
  // Post loop
                                                        ; <--- loop begin
  0x7f22eb23d9a0:   add    0x10(%rax,%r9,4),%r11d       ; add the value of an element to r11d
  0x7f22eb23d9a5:   inc    %r9d                         ; increment the loop counter
  0x7f22eb23d9a8:   cmp    %r9d,%r10d                   ; compare the loop counter with the array length
  0x7f22eb23d9ab:   jg     0x7f22eb23d9a0               ; <--- loop end (jump loop back if greater)
  ; r11d stores the result of the main loop
```

### Analysis of unpredictable_if_branch

#### C2 JIT Compiler

The C2 JIT Compiler unrolls the main loop by a factor of 8, effectively handling 8 additions per unrolled loop cycle. To manage comparisons against a specified threshold value, the compiler employs the `cmovle` instruction.

```
  // Main loop
  
  0x7f510463a25a:   mov    0x18(%rsi),%ebp              ; get the array pointer
  0x7f510463a260:   mov    0xc(%r12,%rbp,8),%ecx        ; get the array length
  0x7f510463a281:   lea    (%r12,%rbp,8),%r14           ; get the array address
  ...
  0x7f510463a2b4:   mov    %ecx,%r13d                   ; move the array length to r13d
                                                        ; <--- loop begin
  0x7f510463a2f0:   mov    0x10(%r14,%rbp,4),%r10d      ; load the value of the 1st element into r10d
  ...
  0x7f510463a318:   lea    (%rax,%r10,1),%edx           ; calculate an address offset
  0x7f510463a31c:   cmp    $0x800,%r10d                 ; compare against 2048 (THRESHOLD / 2)
  0x7f510463a323:   cmovle %edx,%eax                    ; conditional move: move the value to eax if less than or equal
  ...
  <--- similar for the 2nd, 3rd, 4th, 5th, 6th, 7th, and 8th elements -->
  ...
  0x7f510463a38a:   add    $0x8,%ebp                    ; increment the loop counter
  0x7f510463a38d:   cmp    %r13d,%ebp                   ; compare the loop counter with the array length
  0x7f510463a390:   jl     0x7f510463a2f0               ; <--- loop end (jump loop back if less)
  ; eax stores the result of the main loop
```

The post-loop handles the remaining elements individually, without loop unrolling, employing the conditional move (`cmovle`) instruction to evaluate if the array values exceed the specified threshold.

#### Oracle GraalVM JIT Compiler

Oracle GraalVM JIT Compiler does to sum of elements array using vectorized instructions that operate on 256-bit wide AVX (Advanced Vector Extensions) registers, thereby handling 8 additions per unrolled loop cycle.

```
  // Main loop
  
  0x7fd01ad7e9df:   mov    0x18(%rsi),%eax              ; get the array pointer
  0x7fd01ad7e9e2:   mov    0xc(,%rax,8),%r10d           ; get the array length
  0x7fd01ad7e9f3:   shl    $0x3,%rax                    ; compressed oops (shift left by 3 for addressing)
  0x7fd01ad7e9f7:   lea    0x10(%rax),%rax              ; get the array address     
  ...
  0x7fd01ad7ea0e:   vmovdqa -0x96(%rip),%ymm1           ; load the value 'threshold'
  0x7fd01ad7ea16:   mov    $0x0,%r8                     ; initialize loop counter r8 to 0x0
  ...                                                   ; <--- loop begin
  0x7fd01ad7ea20:   vmovdqu (%rax,%r8,4),%ymm2          ; load 256 bits (8 integers) into ymm2
  0x7fd01ad7ea26:   vpaddd %ymm2,%ymm0,%ymm3            ; packed integer addition ymm3 = ymm0 + ymm2
  0x7fd01ad7ea2a:   vpcmpgtd %ymm2,%ymm1,%ymm2          ; compare elements against 'threshold', setting 1s for greater elements in ymm2
  0x7fd01ad7ea2e:   vpblendvb %ymm2,%ymm3,%ymm0,%ymm0   ; blend bytes based on ymm0 mask using ymm2 and ymm3, result in ymm0
  0x7fd01ad7ea34:   lea    0x8(%r8),%r8                 ; increment loop counter by 8
  0x7fd01ad7ea38:   cmp    %r11,%r8                     ; compare the loop counter with the array length
  0x7fd01ad7ea3b:   jle    0x7fd01ad7ea20               ; <--- loop end (jump loop back if less or equal)
  ; ymm0 stores the result of the main loop
```

#### GraalVM CE JIT

The GraalVM CE JIT Compiler processes elements individually without loop unrolling, utilizing comparison and jump instructions to assess whether the array values surpass the predefined threshold.

```
  0x7f91d723d773:   mov    0x10(,%rax,8),%r11d          ; load the value of the first element
  0x7f91d723d77b:   cmp    $0x801,%r11d                 ; compare the first element against the 'threshold'
  0x7f91d723d782:   mov    $0x0,%r8d                    ; if greater, set r8d (i.e., the sum) to 0x0
  0x7f91d723d788:   cmovl  %r11d,%r8d                   ; if less, set r8d to the first element
  0x7f91d723d78c:   shl    $0x3,%rax                    ; compressed oops (shift left by 3 for addressing)
  0x7f91d723d790:   mov    $0x1,%r11d                   ; initialize loop counter
                                                        ; <--- Loop begin
  0x7f91d723d7a0:   cmp    %r11d,%r10d                  ; compare loop counter with the array length
  0x7f91d723d7a3:   jle    0x7f91d723d7c2               ; jump outside if less or equal
  0x7f91d723d7a9:   mov    0x10(%rax,%r11,4),%r9d       ; load the value of an element in r9d
  0x7f91d723d7ae:   inc    %r11d                        ; increment loop counter
  0x7f91d723d7b1:   cmp    $0x801,%r9d                  ; compare against 'threshold'
  0x7f91d723d7b8:   jge    0x7f91d723d7a0               ; jump loop back if greater or equal
  0x7f91d723d7ba:   add    %r8d,%r9d                    ; add the element to the sum (r9d)
  0x7f91d723d7bd:   mov    %r9d,%r8d                    ; move the sum back to r8d
  0x7f91d723d7c0:   jmp    0x7f91d723d7a0               ; <--- Loop end (jump loop back)
  ; r8d stores the result of the loop
```

### Conclusions

- Oracle GraalVM's JIT Compiler utilizes vectorized instructions, leveraging 256-bit wide AVX (Advanced Vector Extensions) registers for efficiently summing array elements.
- The C2 JIT Compiler unrolls the main loop by a factor of 16 (`no_if_branch` scenario) or by a factor of 8 (`unpredictable_if_branch` scenario). However, due to its use of scalar operations instead of vectorized instructions, it does not achieve performance levels comparable to the Oracle GraalVM JIT Compiler.
- The GraalVM CE JIT Compiler exhibits suboptimal behavior in these specific cases. For instance, it experiences register spills during the unrolling of the main loop by a factor of 16 (`no_if_branch` scenario). Alternatively, it may refrain from unrolling altogether (`unpredictable_if_branch` scenario) due to conditional comparisons within the loop body.

## LockCoarseningBenchmark

Testing how the compiler effectively coarsens or merges several adjacent synchronized blocks into one synchronized block can potentially reduce locking overhead. This optimization can be applied when the same lock object is used by multiple methods. However, it's important to note that while compilers can assist in coarsening or merging locks, it's not always guaranteed.

This benchmark operates on uncontended code-paths due to only one thread executing the benchmark methods. Consequently, the locks are never inflated to a full monitor. Similar scenarios may occur when utilizing older synchronized Java APIs (e.g., `Vector`, `Stack`, `Hashtable`, `StringBuffer`, etc.), or equivalents.

```
  @Benchmark
  public int nested_synchronized() {
    int result = defaultValue << 1;

    synchronized (this) {             // 1st synchronized
      result += incrementValue;
      synchronized (this) {           // 2nd synchronized
        result += incrementValue;
        ...                           
          synchronized (this) {       // 8th synchronized
            result += incrementValue;
          }
        ...
      }
    }

    return result;
  }
  
  @Benchmark
  public int conditional_nested_method_calls() {
    int result = defaultValue << 1;

    // all conditionals are evaluated to "true"
    if (result > 1 << 5) {             
      result = sum(result);           // 1st synchronized
      if (result > 1 << 5) {           
        result = sum(result);         // 2nd synchronized
        ...                            
          if (result > 1 << 5) {      
            result = sum(result);     // 8th synchronized
          }  
        ...
      }
    }

    return result;
  }  
```

Source code: [LockCoarseningBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/compiler/LockCoarseningBenchmark.java)

[![LockCoarseningBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/LockCoarseningBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/LockCoarseningBenchmark.svg?raw=true)

### Analysis of nested_synchronized

The analysis below pertains to the `nested_synchronized` method, which is more interesting due to the highest differences in performance.

#### C2 JIT Compiler

The C2 JIT Compiler fails to reduce the deoptimization rate and hits a recompilation limit. Consequently, the method is abandoned (i.e., the compilation is disabled), falling back to the Template Interpreter.

```
  <task_queued compile_id='510' method='nested_synchronized ()I' bytes='211' level='3' comment='tiered'/>
  <make_not_compilable level='3' reason='MethodCompilable_not_at_tier' method='nested_synchronized ()I' bytes='211'/>
  
  <task_queued compile_id='513' method='nested_synchronized ()I' bytes='211' comment='tiered'/>
  <make_not_compilable level='4' reason='MethodCompilable_not_at_tier' method='nested_synchronized ()I' bytes='211'/>
```

The breakdown of the hottest regions by source indicates that the Interpreter dominates the execution.

```
  ....[Distribution by Source]........................................................................
    91.17%           interpreter
     3.47%                kernel
     2.94%         perf-6990.map
     1.31%             libjvm.so
     0.90%           c2, level 4
     0.14%             libc.so.6
     0.03%        hsdis-amd64.so
     0.03%  ld-linux-x86-64.so.2
     ...
```

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM JIT Compiler optimizes code by performing lock coarsening, which involves eliminating redundant locks and aggregating the nested locks into a single synchronized block.

```
  0x7f994ad7c83f:   mov    %rsi,%r11
  0x7f994ad7c845:   mov    0x10(%r11),%eax          ; load the field 'defaultValue' into eax
  ...
  0x7f994ad7c84d:   mov    %eax,%r9d                ; copy the value of eax to r9d (i.e., defaultValue)
  0x7f994ad7c850:   shl    %r9d                     ; perform a left shift on the value in r9d by 1
  <--- coarsened section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f994ad7c886:   mov    0x14(%r11),%r8d          ; load the field 'incrementValue'
  ...
  0x7f994ad7c8c2:   shl    $0x3,%r8d                ; perform a left shift on the value in r8d by 0x3
  0x7f994ad7c8c6:   add    %r8d,%r9d                ; add the values in r8d and r9d and store the result in r9d
  0x7f994ad7c8c9:   mov    %r9d,%eax                ; move the value in r9d to eax
  ; eax stores the result
  <--- end of coarsened section --->
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler is able to compile the entire method, nevertheless it does not merge the locks.

```
  0x7f096323c87f:   mov    %rsi,%r11
  0x7f096323c885:   mov    0x10(%r11),%eax              ; load the field 'defaultValue' into eax
  0x7f096323c88e:   mov    %eax,%r9d                    ; copy the value of %eax to %r9d (i.e., defaultValue)
  0x7f096323c891:   shl    %r9d                         ; perform a left shift on the value in %r9d by 1
  <--- 1st synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c8d3:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to %r9d
  <--- 2nd synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 3rd synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 4th synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 5th synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 6th synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 7th synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  <--- 8th synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  ...
  0x7f096323c913:   add    0x14(%r11),%r9d              ; add the field 'incrementValue' to r9d
  ; r9d stores the result
```

### Analysis of conditional_nested_method_calls

The analysis below pertains to the `conditional_nested_method_calls` method.

#### C2 JIT Compiler

The C2 JIT Compiler successfully inlines the `sum` methods in the caller and compiles the entire method. However, it does not merge certain locks within the compiled code.

```
  0x7fd3804f941d:   mov    0x10(%rsi),%r13d             ; load the 'defaultValue' field into r13d
  0x7fd3804f9421:   shl    %r13d                        ; left shift the value in r13d by 1
  0x7fd3804f9424:   cmp    $0x20,%r13d                  ; compare r13d against the value '1 << 5'
  0x7fd3804f9428:   jle    0x7fd3804f9f37           ; jump if r13d is less than or equal to '1 << 5'
  ...
  <--- caller 'sum' method inlined --->
  <--- 1st synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->                                                                                                                                                                                                                       ; - com.ionutbalosin.jvm.performance.benchmarks.compiler.LockCoarseningBenchmark::conditional_nested_method_calls@10 (line 177)
  0x7fd3804f9496:   add    0x14(%rbp),%r13d             ; add the 'incrementValue' field to r13d
  ...
  0x7fd3804f9534:   cmp    $0x20,%r13d
  0x7fd3804f9538:   jle    0x7fd3804f9f50           ; jump if r13d is less than or equal to '1 << 5'
  <--- caller 'sum' method inlined --->
  <--- 2nd synchronized section (i.e., the monitor is not inflated; stack/fast-locking) --->
  0x7fd3804f95a8:   add    0x14(%rbp),%r13d             ; add the 'incrementValue' field to r13d
  ...
  <--- similar pattern for the 3rd, 4th, 5th, 6th, 7th, and 8th synchronized sections -->
  ...
  ; r13d stores the result of the main loop
  0x7fd3804f9cc6:   mov    %r13d,%eax                   ; move the sum result into eax
```

As a side note, the default settings in `-prof perfasm` display the hottest regions around the `lock cmpxchg` (compare-and-set) instructions, but this may not assemble the entire compiled method. By running `-prof perfasm:mergeMargin=1024` (default value is 32), all hot regions can be aggregated to provide a comprehensive view.
Here are further details about the [mergeMargin](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/profile/AbstractPerfAsmProfiler.java#L120) option.

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM JIT Compiler inlines the `sum` method calls, employs lock coarsening, and utilizes conditional instructions (e.g., `cmp`) to handle the additions.

```
  0x7ffa1ad7dc3f:   mov    %rsi,%r11
  0x7ffa1ad7dc42:   mov    0x10(%r11),%eax          ; load the field 'defaultValue' into eax
  ...
  0x7ffa1ad7dc46:   mov    %eax,%r8d                ; copy the value of eax to r8d (i.e., 'defaultValue')
  0x7ffa1ad7dc49:   shl    %r8d                     ; perform a left shift on the value in r8d by 1
  <--- coarsened section --->
  0x7ffa1ad7dc4c:   cmp    $0x21,%r8d               ; compare r8d against value '1 << 5 + 1'
  0x7ffa1ad7dc50:   jl     0x7ffa1ad7deed       ; jump if r8d is less
  ...
  0x7ffa1ad7dc90:   mov    0x14(%r11),%r10d         ; get field 'incrementValue'
  0x7ffa1ad7dc94:   mov    %r8d,%eax                ; eax = 'defaultValue'
  0x7ffa1ad7dc97:   add    %r10d,%eax               ; eax = eax + r10d
  ...
  0x7ffa1ad7dca0:   cmp    $0x21,%eax               ; compare eax against value '1 << 5 + 1'
  0x7ffa1ad7dca3:   jl     0x7ffa1ad7dec7       ; jump if eax is less
  0x7ffa1ad7dca9:   add    %r10d,%eax               ; eax = eax + r10d
  <--- similar pattern for the 3rd, 4th, 5th, 6th, 7th, and 8th additions -->
  ; eax stores the result
  ...
  <--- end of coarsened section --->
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler utilizes a similar approach to the Oracle GraalVM JIT Compiler in this benchmark.

### Conclusions

- The Oracle GraalVM JIT Compiler triggers lock coarsening and eliminates redundant locks, including the inlining of target method invocations. These optimizations lead to the shortest overall response time.
- The C2 JIT Compiler exhibits limitations in the `nested_synchronized` scenario, leading it to 'bail out' to the Template Interpreter. Within the `conditional_nested_method_calls`, although the callee method is inlined at various call sites, the failure to merge the locks remains an issue. Consequently, its performance tends to be slower even than the GraalVM CE JIT Compiler, in this benchmark.

## LockElisionBenchmark

Test how the compiler can elide/remove several adjacent locking blocks on non-shared objects, thus reducing the locking overhead.
Synchronization on non-shared objects is pointless, and runtime does not have to do anything there.

```
  private final int DEPTH = 8;

  @Benchmark
  public int nested_synchronized() {
    int result = defaultValue << 1;

    Object lock = new Object();

    synchronized (lock) {                   // 1st synchronized
      result += incrementValue;
      synchronized (lock) {                 // 1st synchronized
        result += incrementValue;
        ...
          synchronized (lock) {             // 8th synchronized 
            result += incrementValue;
          }
        ...
      }
    }

    return result;
  }
  
  @Benchmark
  public int recursive_method_calls() {
    int result = defaultValue << 1;
    result = recursiveSum(result, DEPTH);
    return result;
  }
  
  public int recursiveSum(int aValue, int depth) {
    Object lock = new Object();
    synchronized (lock) {
      if (depth == 0) {
        return aValue;
      }
      return incrementValue + recursiveSum(aValue, depth - 1);
    }
  }    
```

Source code: [LockElisionBenchmark.java](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/benchmarks/src/main/java/com/ionutbalosin/jvm/performance/benchmarks/compiler/LockElisionBenchmark.java)

[![LockElisionBenchmark.svg](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/LockElisionBenchmark.svg?raw=true)](https://github.com/ionutbalosin/jvm-performance-benchmarks/blob/main/results/jdk-21/x86_64/plot/LockElisionBenchmark.svg?raw=true)

### Analysis

### Analysis of nested_synchronized

The analysis below pertains to the `nested_synchronized` method, which is more interesting due to the highest differences in performance.

#### C2 JIT Compiler

The C2 JIT Compiler fails to reduce the deoptimization rate and hits a recompilation limit. Consequently, the method is abandoned (i.e., the compilation is disabled), falling back to the Template Interpreter. This is the same limitation as in the case of `LockCoarseningBenchmark`.

The breakdown of the hottest regions by source indicates that the Interpreter dominates the execution.

```
  ....[Distribution by Source]........................................................................
    89.45%           interpreter
     4.18%         perf-7796.map
     3.69%                kernel
     1.22%           c2, level 4
     1.17%             libjvm.so
     0.21%             libc.so.6
     0.04%        hsdis-amd64.so
     0.01%  ld-linux-x86-64.so.2
     ...
```

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM JIT Compiler optimizes code by removing the locks, and further optimizing the additions using shift operations, thereby performing strength reduction optimizations.

```
  0x7f9816d7e73a:   mov    0x10(%rsi),%eax        ; load the value of the 'defaultValue' field into eax
  0x7f9816d7e73d:   mov    0x14(%rsi),%r10d       ; load the value of the 'incrementValue' field into r10d
  0x7f9816d7e741:   shl    %eax                   ; perform a left shift on the value in eax by 1 (eax = eax << 1)
  0x7f9816d7e743:   shl    $0x3,%r10d             ; perform a left shift on the value in r10d by 3 (r10d = r10d << 3)
  0x7f9816d7e747:   add    %r10d,%eax             ; add the value in r10d to eax (eax = eax + r10d)
  ; eax stores the result
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT Compiler utilizes a similar approach to the Oracle GraalVM JIT Compiler in this benchmark.

### Analysis of recursive_method_calls

#### C2 JIT Compiler

The C2 JIT Compiler is capable of devirtualizing virtual calls and performs partial inlining up to a depth of 2.

```
  0x7f1290636b5a:   mov    0x14(%rsi),%ebp         ; load the value of the 'incrementValue' field into ebp
  0x7f1290636b5d:   mov    0x10(%rsi),%edx         ; load the value of the 'defaultValue' field into edx
  0x7f1290636b60:   shl    %edx                    ; perform a left shift on the value in edx by 1 (edx = 'defaultValue' << 1)
  0x7f1290636b62:   mov    $0x6,%ecx               ; move the value 0x6 into ecx
  0x7f1290636b67:   call   0x7f1290636740          ; <--- call to recursiveSum two layers deep
                                                   ; parameters passed: edx (result), ecx (stack depth)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursive_method_calls@11 (line 108)
                                                   ; {optimized virtual_call}
  0x7f1290636b74:   add    %ebp,%eax               ; add 'incrementValue' to eax
  0x7f1290636b76:   add    %ebp,%eax               ; add 'incrementValue' to eax
  ; eax stores the final result (defaultValue << 1 + 8 x incrementValue)
```

The `recursiveSum` is partially inlined but also includes a recursive call to itself.

```
  recursiveSum

    ↗ 0x7f1290636740:   mov    %eax,-0x14000(%rsp)      ; move the value in eax to the address -0x14000 in the stack
    │ 0x7f129063675e:   mov    0x14(%rsi),%ebp          ; load the value of the 'incrementValue' field into ebp
    │ 0x7f1290636761:   cmp    $0x1,%ecx                ; compare the value in ecx with 0x1
  ╭ │ 0x7f1290636764:   je     0x7f1290636793           ; jump to 0x7f1290636793 if the comparison is equal (zero flag is set)
  │ │ 0x7f1290636766:   add    $0xfffffffe,%ecx         ; decrement ecx by 2
  │ ╰ 0x7f129063676b:   call   0x7f1290636740           ; <--- recursive call to itself
  │                                                     ; invokevirtual recursiveSum
  │                                                     ; - LockElisionBenchmark::recursiveSum@31 (line 227)
  │                                                     ; - LockElisionBenchmark::recursiveSum@31 (line 227)
  │                                                     ;   {optimized virtual_call}
  │   0x7f1290636778:   add    %ebp,%eax                ; add 'incrementValue' to eax
  │↗  0x7f129063677a:   add    %ebp,%eax                ; add 'incrementValue' to eax
  ││  ...
  ││  0x7f129063678e:   ret                             ; return from the function
  ││  ...
  ↘│  0x7f1290636793:   mov    %edx,%eax                ; move 'defaultValue' into eax
   ╰  0x7f1290636795:   jmp    0x7f129063677a           ; jump to 0x7f129063677a (continuation)
```

#### Oracle GraalVM JIT Compiler

The Oracle GraalVM JIT Compiler eliminates all the virtual calls and further optimizes the code by eliminating locks and performing strength reduction optimizations to calculate the final result.

```
  0x7fa406d7c5ba:   mov    0x10(%rsi),%eax              ; load the value of the 'defaultValue' field into eax
  0x7fa406d7c5bd:   mov    0x14(%rsi),%r10d             ; load the value of the 'incrementValue' field into r10d
  0x7fa406d7c5c1:   shl    %eax                         ; perform a left shift on the value in eax by 1 (eax = eax << 1)
  0x7fa406d7c5c3:   shl    $0x3,%r10d                   ; perform a left shift on the value in r10d by 3 (r10d = r10d << 3)
  0x7fa406d7c5c7:   add    %r10d,%eax                   ; add the value in r10d to eax (eax = eax + r10d)
  ; eax stores the result
```

#### GraalVM CE JIT Compiler

The GraalVM CE JIT is capable of devirtualizing virtual calls and performing partial inlining up to a depth of 6.

```
  0x7f5c6b2399df:   mov    0x10(%rsi),%edx         ; Load the value of the 'defaultValue' field into edx
  0x7f5c6b2399e2:   mov    0x14(%rsi),%r10d        ; Load the value of the 'incrementValue' field into r10d
  0x7f5c6b2399e6:   shl    %edx                    ; Perform a left shift on the value in edx by 1 (edx = 'defaultValue' << 1)
  0x7f5c6b2399e8:   mov    $0x2,%ecx               ; Load the value 0x2 into ecx
  0x7f5c6b2399ed:   mov    %r10d,0x4(%rsp)         ; Put the value from r10d (i.e., 'incrementValue') on the stack
  0x7f5c6b2399f3:   call   0x7f5c6b239440          ; <--- call to recursiveSum six layers deep
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursiveSum@31 (line 227)
                                                   ; - LockElisionBenchmark::recursive_method_calls@11 (line 108)
                                                   ;   {optimized virtual_call}
  0x7f5c6b239a00:   mov    0x4(%rsp),%r10d         ; Get back the r10d (i.e., 'incrementValue') from the stack
  0x7f5c6b239a05:   mov    %r10d,%r11d             ; r11d = 'incrementValue'
  0x7f5c6b239a08:   shl    $0x2,%r11d              ; r11d = 'incrementValue' << 2
  0x7f5c6b239a0c:   shl    %r10d                   ; r10d = 'incrementValue' << 1
  0x7f5c6b239a0f:   add    %r10d,%r11d             ; r11d = r10d + r11d
  0x7f5c6b239a12:   add    %r11d,%eax              ; eax = eax + r11d
  ; eax stores the result
```

The `recursiveSum` looks like this:

```
  recursiveSum

  0x7f5c6b2394cc:   add    0x4(%rsp),%eax         ; add the value from the stack address + 0x4 (i.e., 'incrementValue') to eax
  0x7f5c6b2394d0:   add    0x4(%rsp),%eax
  0x7f5c6b2394d4:   add    0x4(%rsp),%eax
  0x7f5c6b2394d8:   mov    0x4(%rsp),%r10d        ; move the value from the stack address + 0x4 (i.e., 'incrementValue') to r10d
  0x7f5c6b2394dd:   add    %r10d,%eax             ; add the value from r10d (i.e., 'incrementValue') to eax
```

### Conclusions

## LoopFusionBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

## LoopInvariantCodeMotionBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

## LoopReductionBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

## RecursiveMethodCallBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

## ScalarReplacementBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

## TypeCheckBenchmark
### Analysis
#### C2 JIT Compiler
#### Oracle GraalVM JIT Compiler
#### GraalVM CE JIT Compiler
### Conclusions

The results are identical. This increases the confidence in the benchmark results, across the selected JVMs.

## JIT Geometric Mean

This section describes the normalized GM for the entire JIT-related benchmark category, having in total 301 benchmarks.
This is purely informative to have a high-level understanding of the overall benchmark scores.

The process of generating the normalized geometric mean is:
- all benchmark scores are converted to the same unit (e.g., "ns/op")
- the resulting geometric mean is normalized to C2 JIT

## x86_64

No. | JIT            | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|----------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE JIT | x86_64      | 0.65                      | 301              | ns/op
2   | C2 JIT         | x86_64      | 1                         | 301              | ns/op
3   | GraalVM CE JIT | x86_64      | 1.06                      | 301              | ns/op

**Note:** The first in the row is the fastest JIT, and the last in the row is the slowest JIT

## arm64

No. | JIT            | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|----------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE JIT | x86_64      | 0.76                      | 301              | ns/op
2   | C2 JIT         | x86_64      | 1                         | 301              | ns/op
3   | GraalVM CE JIT | x86_64      | 1.31                      | 301              | ns/op

**Note:** The first in the row is the fastest JIT, and the last in the row is the slowest JIT

To summarize, on both architectures the normalized geometric mean is consistent:

1. GraalVM EE JIT is the fastest
2. C2 JIT is in the middle
3. GraalVM CE JIT is the slowest

# API

This set of benchmarks is dedicated to larger programs using high-level Java APIs (e.g., stream, lambda, fork-join, etc.). It is created to complement the existing JIT benchmarks with another class of benchmarks.

## API Benchmarks

The miscellaneous benchmarks are measured in [average time per operation](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/annotations/Mode.java#L52), which is the score reported by the JMH.

## Benchmark

... TODO ...

## API Geometric Mean

This section describes the normalized GM for the entire miscellaneous benchmarks category, having in total 723 benchmarks.
This is purely informative to have a high-level understanding of the overall benchmark scores.

The process of generating the normalized geometric mean is:
- all benchmark scores are converted to the same unit (e.g., "ns/op")
- the resulting geometric mean is normalized to OpenJDK

## x86_64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean   | Nr.of.Benchmarks | Unit
----|------------------|-------------|-----------------------------|------------------|--------
1   | GraalVM EE       | x86_64      | 0.82                        | 723              | ns/op        
2   | OpenJDK          | x86_64      | 1                           | 723              | ns/op          
3   | GraalVM CE       | x86_64      | 1.02                        | 723              | ns/op           

**Note:** The first in the row is the fastest, and the last in the row is the slowest

## arm64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|------------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE       | arm64       | 0.86                      | 723              | ns/op
2   | GraalVM CE       | arm64       | 0.99                      | 723              | ns/op
3   | OpenJDK          | arm64       | 1                         | 723              | ns/op

**Note:** The first in the row is the fastest, and the last in the row is the slowest

To summarize, on both architectures the normalized geometric mean for GraalVM EE is the fastest.
OpenJDK and GraalVM CE are very close and interchangeable.

# Miscellaneous

This set of benchmarks is dedicated to larger programs using high-level Java APIs (e.g., stream, lambda, fork-join, etc.). It is created to complement the existing JIT benchmarks with another class of benchmarks.

## Miscellaneous Benchmarks

The miscellaneous benchmarks are measured in [average time per operation](https://github.com/openjdk/jmh/blob/master/jmh-core/src/main/java/org/openjdk/jmh/annotations/Mode.java#L52), which is the score reported by the JMH.

## Benchmark

... TODO ...

## Miscellaneous Geometric Mean

This section describes the normalized GM for the entire miscellaneous benchmarks category, having in total 80 benchmarks.
This is purely informative to have a high-level understanding of the overall benchmark scores.

The process of generating the normalized geometric mean is:
- all benchmark scores are converted to the same unit (e.g., "ns/op")
- the resulting geometric mean is normalized to OpenJDK

## x86_64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|------------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE       | x86_64      | 0.87                      | 80               | ns/op
2   | OpenJDK          | x86_64      | 1                         | 80               | ns/op
3   | GraalVM CE       | x86_64      | 1.1                       | 80               | ns/op

**Note:** The first in the row is the fastest, and the last in the row is the slowest

## arm64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|------------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE       | arm64       | 0.91                      | 80               | ns/op
2   | OpenJDK          | arm64       | 1                         | 80               | ns/op
3   | GraalVM CE       | arm64       | 1.13                      | 80               | ns/op

**Note:** The first in the row is the fastest, and the last in the row is the slowest

To summarize, on both architectures the normalized geometric mean is consistent:

1. GraalVM EE is the fastest
2. OpenJDK is in the middle
3. GraalVM CE is the slowest

## Overall Geometric Mean

This section describes the normalized GM for the entire benchmarks categories, having in total 1104 benchmarks.
This is purely informative to have a high-level understanding of the overall benchmark scores.

The process of generating the normalized geometric mean is:
- all benchmark scores are converted to the same unit (e.g., "ns/op")
- the resulting geometric mean is normalized to OpenJDK

## x86_64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|------------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE       | x86_64      | 0.77                      | 1104             | ns/op
2   | OpenJDK          | x86_64      | 1                         | 1104             | ns/op
3   | GraalVM CE       | x86_64      | 1.03                      | 1104             | ns/op

**Note:** The first in the row is the fastest, and the last in the row is the slowest

## arm64

No. | JVM distribution | Arcitecture | Normalized Geometric Mean | Nr.of.Benchmarks | Unit
----|------------------|-------------|---------------------------|------------------|--------
1   | GraalVM EE       | arm64       | 0.83                      | 1104             | ns/op
2   | OpenJDK          | arm64       | 1                         | 1104             | ns/op
3   | GraalVM CE       | arm64       | 1.08                      | 1104             | ns/op

**Note:** The first in the row is the fastest, and the last in the row is the slowest

To summarize, on both architectures the normalized geometric mean is consistent:

1. GraalVM EE is the fastest
2. OpenJDK is in the middle
3. GraalVM CE is the slowest

# Final Thoughts

In this article we compared three different JVM distributions (OpenJDK, GraalVM CE and GraalVM EE) on both x86_64 and arm64.
We used a set of JMH benchmarks to assess the performance of the JIT compilers performing a non-exhaustive set of optimizations.

... TODO ...

In case you want to contribute to this project, feel free to reach out or open a pull request on
[GitHub](https://github.com/ionutbalosin/jvm-performance-benchmarks/).

# References
- [OpenJDK sources](https://github.com/openjdk/jdk)
- [GraalVM sources](https://github.com/oracle/graal)
- [JHM sources](https://github.com/openjdk/jmh)
- [async-profiler](https://github.com/async-profiler/async-profiler)
- [How to not lie with statistics: the correct way to summarize benchmark results](https://dl.acm.org/doi/pdf/10.1145/5666.5673) - Philip J Fleming, John J Wallace