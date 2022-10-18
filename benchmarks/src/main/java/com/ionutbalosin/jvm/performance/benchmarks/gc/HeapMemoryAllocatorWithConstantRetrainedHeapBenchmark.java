/**
 *  JVM Performance Benchmarks
 *
 *  Copyright (C) 2019 - 2022 Ionut Balosin
 *  Website: www.ionutbalosin.com
 *  Twitter: @ionutbalosin
 *
 *  Co-author: Florin Blanaru
 *  Twitter: @gigiblender
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.ionutbalosin.jvm.performance.benchmarks.gc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jol.info.GraphLayout;

/*
 * This benchmark initially allocates (during setup) chunks of chained objects, until it fills up
 * a certain percent of Heap (e.g., 25%, 50%, 75%) and keeps strong references to them from an array.
 * Such a chain looks like Object 1 -> Object 2 -> … -> Object 32 where an object consist of pointer to the next object
 * and an array of longs.
 * Note: the chaining might have an impact on the GC roots traversal (for example during the “parallel” marking phase),
 * since the degree of pointer indirection (i.e., reference processing) is not negligible, while traversing the object graph dependencies.
 *
 * Then, in the benchmark test() method, similar object chains are allocated, and they replace, one by one (i.e., incrementally),
 * the ones from the initial array so that the former ones become eligible for Garbage Collector.
 *
 * Note: During the lifecycle of the benchmark the amount of retained memory by strong references is trying to be constant!
 *
 * Note: some objects within the chain are potentially considered big, so they would normally follow the slow path allocation,
 * residing directly in the Tenured Generation (in case of generational collectors), increasing the likelihood of full GCs.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 2)
@State(Scope.Benchmark)
public class HeapMemoryAllocatorWithConstantRetrainedHeapBenchmark {

  @Param private PercentageOfRetainedHeap percentageOfRetainedHeap;

  private final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
  private final int CHAIN_REFERENCE_DEPTH = 32;
  private final int ARRAY_LENGTH_INCREMENT = 4;
  private final int ARRAY_LENGTH_MULTIPLIER = 1;

  private int retainedArraySize;
  private ObjectChain[] retainedArray;
  private AtomicInteger index;

  @Setup()
  public void setup() {
    long objectSizeInBytes = GraphLayout.parseInstance(createChainedObjects()).totalSize();
    long maxNumberOfObjects = MAX_MEMORY / objectSizeInBytes;
    index = new AtomicInteger(0);

    switch (percentageOfRetainedHeap) {
      case P_25:
        retainedArraySize = (int) (maxNumberOfObjects * 0.25);
        break;
      case P_50:
        retainedArraySize = (int) (maxNumberOfObjects * 0.50);
        break;
      case P_75:
        retainedArraySize = (int) (maxNumberOfObjects * 0.75);
        break;
      default:
        throw new UnsupportedOperationException(
            "Unsupported percentage of allocated instances " + percentageOfRetainedHeap);
    }

    retainedArray = new ObjectChain[retainedArraySize];
    for (int i = 0; i < retainedArraySize; i++) {
      retainedArray[i] = createChainedObjects();
    }
  }

  // JMH Opts: -prof gc

  @Benchmark
  @Fork(jvmArgsAppend = {"-XX:+UseSerialGC", "-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch"})
  public void serialGC() {
    allocate();
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-XX:+UseParallelGC", "-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch"})
  public void parallelGC() {
    allocate();
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-XX:+UseG1GC", "-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch"})
  public void g1GC() {
    allocate();
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-XX:+UseShenandoahGC", "-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch"})
  public void shenandoahGC() {
    allocate();
  }

  @Benchmark
  @Fork(jvmArgsAppend = {"-XX:+UseZGC", "-Xms4g", "-Xmx4g", "-XX:+AlwaysPreTouch"})
  public void zGC() {
    allocate();
  }

  @CompilerControl(CompilerControl.Mode.DONT_INLINE)
  private void allocate() {
    ObjectChain chainHead = createChainedObjects();
    retainedArray[index.getAndIncrement() % retainedArraySize] = chainHead;
  }

  private ObjectChain createChainedObjects() {
    int arrayLength = 0;
    ObjectChain head = new ObjectChain(arrayLength);

    ObjectChain cursor = head;
    for (int i = 0; i < CHAIN_REFERENCE_DEPTH; i++) {
      arrayLength *= ARRAY_LENGTH_MULTIPLIER;
      arrayLength += ARRAY_LENGTH_INCREMENT;

      ObjectChain nextObj = new ObjectChain(arrayLength);
      cursor.refObj = nextObj;
      cursor = nextObj;
    }

    return head;
  }

  public enum PercentageOfRetainedHeap {
    P_25,
    P_50,
    P_75
  }

  //   ObjectChain internals:
  //         OFFSET  SIZE               TYPE DESCRIPTION
  //            0      4                    (object header)
  //            4      4                    (object header)
  //            8      4                    (object header)
  //            12     4   java.lang.Object ObjectChain.refObj
  //            16     4             long[] ObjectChain.longArray
  //            20     4                    (loss due to the next object alignment)
  //    Instance size: 24 bytes
  //    Space losses: 0 bytes internal + 4 bytes external = 4 bytes total

  public class ObjectChain {
    Object refObj;
    long array[];

    ObjectChain(int arrayLength) {
      if (arrayLength > 0) this.array = new long[arrayLength];
    }
  }
}
