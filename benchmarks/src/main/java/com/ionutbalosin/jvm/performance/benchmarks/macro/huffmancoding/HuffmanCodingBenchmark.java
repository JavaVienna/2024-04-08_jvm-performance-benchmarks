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
package com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding;

import static com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding.HuffmanCoding.charFrequency;
import static com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding.HuffmanCoding.codingTree;
import static com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding.HuffmanDecoder.decodeData;
import static com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding.HuffmanEncoder.encodeData;
import static com.ionutbalosin.jvm.performance.benchmarks.macro.huffmancoding.HuffmanEncoder.encodingMap;

import java.io.IOException;
import java.util.Map;
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
 * Huffman encoding is an algorithm devised by David A. Huffman of MIT in 1952 for compressing text data to make a file
 * occupy a smaller number of bytes. This relatively simple compression algorithm is powerful enough that variations of it
 * are still used today in computer networks, fax machines, modems, HDTV, and other areas.
 *
 * The steps involved in Huffman encoding a given text source file into a destination compressed file are:
 * - count frequencies: examine a source file's contents and count the number of occurrences of each character
 * - build encoding tree: build a binary tree with a particular structure, where each node represents a character
 * and its count of occurrences in the file. A priority queue is used to help build the tree along the way.
 * - build encoding map: traverse the binary tree to discover the binary encodings of each character
 * - encode data: re-examine the source file's contents, and for each character, output the encoded binary version of
 * that character to the destination file.
 *
 * Note: the benchmark adds one more final step: it decodes the previously encoded data (and does a basic sanity check)
 *
 * References:
 * - https://web.stanford.edu/class/archive/cs/cs106b/cs106b.1176/assn/huffman.html
 * - https://www.techiedelight.com/huffman-coding
 * - https://www.w3.org/2001/06/utf-8-test/UTF-8-demo.html
 * - https://raw.githubusercontent.com/teropa/nlp/master/resources/corpora/gutenberg/shakespeare-caesar.txt
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5)
@State(Scope.Benchmark)
public class HuffmanCodingBenchmark {

  // $ java -jar */*/benchmarks.jar ".*HuffmanCodingBenchmark.*"

  private final int MAX_UTF_16_VALUE = 0xFFFF;
  private final Random random = new Random(16384);

  private Map<Character, Integer> charFrequency;
  private HuffmanNode huffmanTree;
  private Map<Character, String> huffmanCodes;
  private char[] data, dataEncoded, dataDecoded;

  @Param({"262144"})
  private int dataSize;

  @Setup()
  public void setup() throws IOException {
    // initialize data
    data = charArray(dataSize, MAX_UTF_16_VALUE);
    charFrequency = charFrequency(data);
    huffmanTree = codingTree(charFrequency);
    huffmanCodes = encodingMap(huffmanTree);

    // encode/decode data
    dataEncoded = encodeData(data, huffmanCodes);
    dataDecoded = decodeData(dataEncoded, huffmanTree);

    // make sure the results are equivalent before any further benchmarking
    sanityCheck(data, huffmanCodes);
    sanityCheck(data, dataDecoded);
  }

  @Benchmark
  public char[] encode() {
    return encodeData(data, huffmanCodes);
  }

  @Benchmark
  public char[] decode() {
    return decodeData(dataEncoded, huffmanTree);
  }

  private char[] charArray(int length, int maxValue) {
    final char[] charArray = new char[length];

    for (int i = 0; i < length; i++) {
      final int codeUnit = random.nextInt(1, maxValue + 0x1);
      if (Character.isValidCodePoint(codeUnit)) {
        charArray[i] = (char) codeUnit;
      } else {
        i--;
      }
    }

    return charArray;
  }

  /**
   * Sanity check for the results to avoid wrong benchmarks comparisons
   *
   * @param input - source char array to encode
   * @param output - output char array after decoding
   */
  private void sanityCheck(char[] input, char[] output) {
    if (input.length != output.length) {
      throw new AssertionError("Arrays have different length.");
    }

    for (int i = 0; i < input.length; i++) {
      if (input[i] != output[i]) {
        throw new AssertionError("Array values are different.");
      }
    }
  }

  /**
   * Check if all characters present in the data array have valid encodings
   *
   * @param input - source char array to encode
   * @param encodingMap - encoding map containing the huffman codes
   */
  private void sanityCheck(char[] input, Map<Character, String> encodingMap) {
    for (char ch : input) {
      final String encoding = encodingMap.get(ch);
      if (encoding == null || encoding.isEmpty()) {
        throw new AssertionError("Invalid or missing encoding for character: " + ch);
      }
    }
  }
}
