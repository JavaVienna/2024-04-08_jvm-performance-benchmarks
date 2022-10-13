package com.ionutbalosin.jvm.performance.benchmarks.compiler;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/*
 * (c) 2019 Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin
 *
 * For the full copyright and license information, please view the LICENSE file that was distributed with this source code.
 */
/*
 * Resources:
 * - see https://github.com/Microsoft/DirectXShaderCompiler/blob/master/docs/Vectorizers.rst
 * - see https://llvm.org/docs/Vectorizers.html#reductions
 */

/*
 * Tests different vectorization patterns using an array of ints. All loops have stride 1 and the loop counter is of type int or long.
 */

//  Pattern:
//
//    int[] A;
//
//    // sum_of_all_array_elements
//    sum += A[i];
//
//    // sum_of_all_array_elements_long_stride
//    sum += A[l];
//
//    // sum_of_all_array_elements_by_adding_a_const
//    sum += A[i] + CONST;
//
//    // sum_of_all_even_array_elements
//    if ( (A[i] & 0x1) == 0 ) {
//        sum += A[i];
//    }
//
//    // sum_of_all_array_elements_matching_a_predicate
//    if (P[i]) {
//        sum += A[i];
//    }
//
//    // sum_of_all_array_elements_by_shifting_and_masking
//    sum += (A[i] >> SHIFT) & MASK;
//
//    // multiply_each_array_element_by_const
//    A[i] = A[i] * CONST;
//
//    // add_const_to_each_array_element
//    A[i] = A[i] + CONST;
//
//    // shl_each_array_element_by_const
//    A[i] = A[i] << CONST;
//
//    // mod_each_array_element_by_const
//    A[i] = A[i] % CONST;
//
//    // saves_induction_variable_to_each_array_element
//    A[i] = i;
//
//    // increment_arrays_elements_backward_iterator (i=n-1...0)
//    A[i] = i;
//
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5)
@State(Scope.Thread)
public class VectorizationPatternsSingleIntArrayBenchmark {

  private final int CONST = 5;
  private final int SHIFT = 3;
  private final int MASK = 0x7f; // 127
  boolean[] P;

  @Param({"262144"})
  private int size;

  private int[] A;

  // java -jar benchmarks/target/benchmarks.jar ".*VectorizationPatternsSingleIntArrayBenchmark.*"

  @Setup
  public void setup() {
    final Random random = new Random(16384);
    A = new int[size];
    P = new boolean[size];
    for (int i = 0; i < size; i++) {
      A[i] = i + random.nextInt(32);
      P[i] = (i % 2 == 0);
    }
  }

  @Benchmark
  public int sum_of_all_array_elements() {
    int sum = 0;
    for (int i = 0; i < size; i++) {
      sum += A[i];
    }
    return sum;
  }

  @Benchmark
  public int sum_of_all_array_elements_long_stride() {
    int sum = 0;
    for (long i = 0; i < size; i++) {
      sum += A[(int) i];
    }
    return sum;
  }

  // https://github.com/Microsoft/DirectXShaderCompiler/blob/master/docs/Vectorizers.rst#reductions
  @Benchmark
  public int sum_of_all_array_elements_by_adding_a_const() {
    int sum = 0;
    for (int i = 0; i < size; i++) {
      sum += A[i] + CONST;
    }
    return sum;
  }

  @Benchmark
  public int sum_of_all_even_array_elements() {
    int sum = 0;
    for (int i = 0; i < size; i++) {
      if ((A[i] & 0x1) == 0) {
        sum += A[i];
      }
    }
    return sum;
  }

  @Benchmark
  public int sum_of_all_array_elements_matching_a_predicate() {
    int sum = 0;
    for (int i = 0; i < size; i++) {
      if (P[i]) { // if conversion ("flatten" the IF statement in the code and generate a single
        // stream of instructions)
        sum += A[i];
      }
    }
    return sum;
  }

  @Benchmark
  public int sum_of_all_array_elements_by_shifting_and_masking() {
    int sum = 0;
    for (int i = 0; i < size; i++) {
      sum += (A[i] >> SHIFT) & MASK;
    }
    return sum;
  }

  @Benchmark
  public int[] multiply_each_array_element_by_const() {
    for (int i = 0; i < size; i++) {
      A[i] = A[i] * CONST;
    }
    return A;
  }

  @Benchmark
  public int[] add_const_to_each_array_element() {
    for (int i = 0; i < size; i++) {
      A[i] = A[i] + CONST;
    }
    return A;
  }

  @Benchmark
  public int[] shl_each_array_element_by_const() {
    for (int i = 0; i < size; i++) {
      A[i] = A[i] << CONST;
    }
    return A;
  }

  @Benchmark
  public int[] mod_each_array_element_by_const() {
    for (int i = 0; i < size; i++) {
      A[i] = A[i] % CONST;
    }
    return A;
  }

  @Benchmark
  public int[] saves_induction_variable_to_each_array_element() {
    for (int i = 0; i < size; i++) {
      A[i] = i;
    }
    return A;
  }

  @Benchmark
  public int[] increment_arrays_elements_backward_iterator() {
    for (int i = size - 1; i >= 0; --i) {
      A[i] += 1;
    }
    return A;
  }
}
