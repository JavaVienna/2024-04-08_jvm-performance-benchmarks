/*
 * JVM Performance Benchmarks
 *
 * Copyright (C) 2019 - 2023 Ionut Balosin
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ionutbalosin.jvm.performance.benchmarks.api.string;

import static com.ionutbalosin.jvm.performance.benchmarks.api.string.utils.StringUtils.COMMON_ENGLISH_CHARS_TARGET;
import static com.ionutbalosin.jvm.performance.benchmarks.api.string.utils.StringUtils.generateCharArray;

import com.ionutbalosin.jvm.performance.benchmarks.api.string.utils.StringUtils.Coder;
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
 * This benchmark evaluates the performance of various string replacement operations, measuring the
 * efficiency of character-based and string-based replacements. It assesses the effectiveness of
 * replacing individual characters or substrings within strings, employing different methods such as
 * 'replace', 'replaceAll', and 'replaceFirst'.
 *
 * The generated strings and characters are encoding-specific for Latin-1 and UTF-16.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5)
@State(Scope.Benchmark)
public class StringReplaceBenchmark {

  // java -jar benchmarks/target/benchmarks.jar ".*StringReplaceBenchmark.*"

  private static final String REGEX = "([a-zA-Z0-9]){2,}";

  private static char[] sourceChArray;
  private static String sourceStr;
  private static int offsetIdx;
  private static String replacementStr;
  private static char replacementCh;

  @Param private static Coder coder = Coder.LATIN1;

  @Param({"1024"})
  private static int length = 1024;

  @Setup
  public static void setup() {
    offsetIdx = 0;

    // Generate encoding-specific sources
    sourceChArray = generateCharArray(length, coder, COMMON_ENGLISH_CHARS_TARGET);
    sourceStr = new String(sourceChArray);

    // Generate encoding-specific targets
    replacementStr = new String(generateCharArray(2, coder)); // a (short) String of 2 chars
    replacementCh = generateCharArray(1, coder)[0];
  }

  @Benchmark
  public static String replace_char() {
    final char target = sourceChArray[nextPosition()];
    return sourceStr.replace(target, replacementCh);
  }

  @Benchmark
  public static String replace_string() {
    final String target = String.valueOf(sourceChArray[nextPosition()]);
    return sourceStr.replace(target, replacementStr);
  }

  @Benchmark
  public static String replace_all_regexp() {
    return sourceStr.replaceAll(REGEX, replacementStr);
  }

  @Benchmark
  public static String replace_first_regexp() {
    return sourceStr.replaceFirst(REGEX, replacementStr);
  }

  public static void main(String args[]) {
    setup();
    System.out.println(sourceStr);
    System.out.println(replace_char());
    System.out.println(replace_string());
    System.out.println(replace_all_regexp());
    System.out.println(replace_first_regexp());
  }

  private static int nextPosition() {
    if (++offsetIdx >= length) {
      offsetIdx = 0;
    }

    return offsetIdx;
  }
}
