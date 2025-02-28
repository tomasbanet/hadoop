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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.hadoop.test.GenericTestUtils;
import org.apache.hadoop.test.GenericTestUtils.LogCapturer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestReflectionUtils {

  private static Class toConstruct[] = { String.class, TestReflectionUtils.class, HashMap.class };
  private Throwable failure = null;

  @BeforeEach
  public void setUp() {
    ReflectionUtils.clearCache();
  }

  @Test
  public void testCache() throws Exception {
    assertEquals(0, cacheSize());
    doTestCache();
    assertEquals(toConstruct.length, cacheSize());
    ReflectionUtils.clearCache();
    assertEquals(0, cacheSize());
  }
    
    
  @SuppressWarnings("unchecked")
  private void doTestCache() {
    for (int i=0; i<toConstruct.length; i++) {
      Class cl = toConstruct[i];
      Object x = ReflectionUtils.newInstance(cl, null);
      Object y = ReflectionUtils.newInstance(cl, null);
      assertEquals(cl, x.getClass());
      assertEquals(cl, y.getClass());
    }
  }

  @Test
  public void testThreadSafe() throws Exception {
    Thread[] th = new Thread[32];
    for (int i=0; i<th.length; i++) {
      th[i] = new Thread() {
          @Override
          public void run() {
            try {
              doTestCache();
            } catch (Throwable t) {
              failure = t;
            }
          }
        };
      th[i].start();
    }
    for (int i=0; i<th.length; i++) {
      th[i].join();
    }
    if (failure != null) {
      failure.printStackTrace();
      fail(failure.getMessage());
    }
  }
    
  private int cacheSize() throws Exception {
    return ReflectionUtils.getCacheSize();
  }

  @Test
  public void testCantCreate() {
    try {
      ReflectionUtils.newInstance(NoDefaultCtor.class, null);
      fail("invalid call should fail");
    } catch (RuntimeException rte) {
      assertEquals(NoSuchMethodException.class, rte.getCause().getClass());
    }
  }
    
  @SuppressWarnings("unchecked")
  @Test
  public void testCacheDoesntLeak() throws Exception {
    int iterations=9999; // very fast, but a bit less reliable - bigger numbers force GC
    for (int i=0; i<iterations; i++) {
      URLClassLoader loader = new URLClassLoader(new URL[0], getClass().getClassLoader());
      Class cl = Class.forName("org.apache.hadoop.util.TestReflectionUtils$LoadedInChild", false, loader);
      Object o = ReflectionUtils.newInstance(cl, null);
      assertEquals(cl, o.getClass());
    }
    System.gc();
    assertTrue(cacheSize()<iterations, cacheSize()+" too big");
  }
  
  @Test
  public void testGetDeclaredFieldsIncludingInherited() {
    Parent child = new Parent() {
      private int childField;
      @SuppressWarnings("unused")
      public int getChildField() { return childField; }
    };
    
    List<Field> fields = ReflectionUtils.getDeclaredFieldsIncludingInherited(
        child.getClass());
    boolean containsParentField = false;
    boolean containsChildField = false;
    for (Field field : fields) {
      if (field.getName().equals("parentField")) {
        containsParentField = true;
      } else if (field.getName().equals("childField")) {
        containsChildField = true;
      }
    }
    
    List<Method> methods = ReflectionUtils.getDeclaredMethodsIncludingInherited(
        child.getClass());
    boolean containsParentMethod = false;
    boolean containsChildMethod = false;
    for (Method method : methods) {
      if (method.getName().equals("getParentField")) {
        containsParentMethod = true;
      } else if (method.getName().equals("getChildField")) {
        containsChildMethod = true;
      }
    }
    
    assertTrue(containsParentField, "Missing parent field");
    assertTrue(containsChildField, "Missing child field");
    assertTrue(containsParentMethod, "Missing parent method");
    assertTrue(containsChildMethod, "Missing child method");
  }

  @Test
  public void testLogThreadInfo() throws Exception {
    Logger logger = LoggerFactory.getLogger(TestReflectionUtils.class);
    LogCapturer logCapturer = LogCapturer.captureLogs(logger);

    final String title = "title";
    ReflectionUtils.logThreadInfo(logger, title, 0L);

    Assertions.assertThat(logCapturer.getOutput())
        .contains("Process Thread Dump: " + title);
  }

  @Test
  public void testNewInstanceForNonDefaultConstructor() {
    Object x = ReflectionUtils.newInstance(
        NoDefaultCtor.class, null, new Class[] {int.class}, 1);
    assertTrue(x instanceof NoDefaultCtor);
  }

  @Test
  public void testNewInstanceForNonDefaultConstructorWithException() {
    try {
      ReflectionUtils.newInstance(
          NoDefaultCtor.class, null, new Class[]{int.class}, 1, 2);
      fail("Should have failed before this point");
    } catch (IllegalArgumentException e) {
      GenericTestUtils.assertExceptionContains(
          "1 parameters are required but 2 arguments are provided", e);
    }

    try {
      ReflectionUtils.newInstance(
          NoDefaultCtor.class, null, new Class[]{int.class});
      fail("Should have failed before this point");
    } catch (IllegalArgumentException e) {
      GenericTestUtils.assertExceptionContains(
          "1 parameters are required but 0 arguments are provided", e);
    }
  }

  // Used for testGetDeclaredFieldsIncludingInherited
  private class Parent {
    private int parentField;
    @SuppressWarnings("unused")
    public int getParentField() { return parentField; }
  }
    
  private static class LoadedInChild {
  }
    
  public static class NoDefaultCtor {
    public NoDefaultCtor(int x) {}
  }
}
