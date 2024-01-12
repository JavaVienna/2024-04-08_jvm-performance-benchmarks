/*
 * JVM Performance Benchmarks
 *
 * Copyright (C) 2019 - 2024 Ionut Balosin
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
package com.ionutbalosin.jvm.performance.benchmarks.api.diskio;

import com.ionutbalosin.jvm.performance.benchmarks.api.diskio.util.DataObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/*
 * Measures the time it takes to serialize a custom object using an ObjectInputStream.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(value = 5)
@State(Scope.Benchmark)
public class ObjectInputStreamBenchmark {

  // $ java -jar */*/benchmarks.jar ".*ObjectInputStreamBenchmark.*"

  private final int OBJECTS = 16_384;

  private File file;
  private ObjectInputStream ois;
  private int objectsRead;

  @Setup(Level.Trial)
  public void beforeTrial() throws IOException {
    final DataObject dataObject = new DataObject();

    file = File.createTempFile("ObjectInputStream", ".tmp");
    try (ObjectOutputStream fos = new ObjectOutputStream(new FileOutputStream(file))) {
      for (int i = 0; i < OBJECTS; i++) {
        fos.writeObject(dataObject);
      }
    }
  }

  @TearDown(Level.Trial)
  public void afterTrial() {
    file.delete();
  }

  @Setup(Level.Iteration)
  public void beforeIteration() throws IOException {
    objectsRead = 0;
    ois = new ObjectInputStream(new FileInputStream(file));
  }

  @TearDown(Level.Iteration)
  public void afterIteration() throws IOException {
    ois.close();
  }

  @Benchmark
  public DataObject read() throws IOException, ClassNotFoundException {
    if (objectsRead + 1 >= OBJECTS) {
      afterIteration();
      beforeIteration();
    }

    final DataObject dataObject = (DataObject) ois.readObject();
    objectsRead++;

    return dataObject;
  }
}
