/*
 * JVM Performance Benchmarks
 *
 * Copyright (C) 2019 - 2023 Ionut Balosin
 *
 * Author: Ionut Balosin
 * Website: www.ionutbalosin.com
 * Twitter: @ionutbalosin / Mastodon: ionutbalosin@mastodon.social
 *
 * Co-author: Florin Blanaru
 * Twitter: @gigiblender / Mastodon: gigiblender@mastodon.online
 *
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
package com.ionutbalosin.jvm.performance.benchmarks.macro.fibonacci.iterative;

import java.math.BigInteger;

public class IterativeFormula {

  public static BigInteger fibonacci(int n, BigInteger a, BigInteger b) {
    BigInteger n0 = a;
    BigInteger n1 = b;
    for (int i = 0; i < n; i++) {
      BigInteger temp = n1;
      n1 = n1.add(n0);
      n0 = temp;
    }
    return n0;
  }
}
