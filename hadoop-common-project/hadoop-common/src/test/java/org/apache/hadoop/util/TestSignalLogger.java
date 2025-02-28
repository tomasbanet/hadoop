/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.util;

import org.apache.commons.lang3.SystemUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class TestSignalLogger {
  public static final Logger LOG =
      LoggerFactory.getLogger(TestSignalLogger.class);
  
  @Test
  @Timeout(value = 60)
  public void testInstall() throws Exception {
    assumeTrue(SystemUtils.IS_OS_UNIX);
    SignalLogger.INSTANCE.register(LOG);
    try {
      SignalLogger.INSTANCE.register(LOG);
      fail("expected IllegalStateException from double registration");
    } catch (IllegalStateException e) {
      // fall through
    }
  }
}
