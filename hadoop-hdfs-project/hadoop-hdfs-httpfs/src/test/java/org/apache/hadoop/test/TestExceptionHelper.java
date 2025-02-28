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
package org.apache.hadoop.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class TestExceptionHelper implements TestExecutionExceptionHandler {

  @Test
  public void dummy() {
  }

  @Override
  public void handleTestExecutionException(ExtensionContext context,
      Throwable cause) throws Throwable {
    Method testMethod = context.getRequiredTestMethod();
    TestException testExceptionAnnotation = testMethod.getAnnotation(TestException.class);
    try {
      if (testExceptionAnnotation != null) {
        Class<? extends Throwable> klass = testExceptionAnnotation.exception();
        fail("Expected Exception: " + klass.getSimpleName());
      }
    } catch (Throwable ex) {
      if (testExceptionAnnotation != null) {
        Class<? extends Throwable> klass = testExceptionAnnotation.exception();
        if (klass.isInstance(cause)) {
          String regExp = testExceptionAnnotation.msgRegExp();
          Pattern pattern = Pattern.compile(regExp);
          if (!pattern.matcher(cause.getMessage()).find()) {
            fail("Expected Exception Message pattern: " + regExp +
                " got message: " + ex.getMessage());
          }
        } else {
          fail("Expected Exception: " + klass.getSimpleName() + " got: " +
              ex.getClass().getSimpleName());
        }
      } else {
        throw ex;
      }
    }
  }
}
