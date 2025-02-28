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
package org.apache.hadoop.fs.shell.find;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.apache.hadoop.fs.shell.find.TestHelper.*;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.shell.PathData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
public class TestIname {
  private FileSystem mockFs;
  private Name.Iname name;

  @BeforeEach
  public void resetMock() throws IOException {
    mockFs = MockFileSystem.setup();
  }

  private void setup(String arg) throws IOException {
    name = new Name.Iname();
    addArgument(name, arg);
    name.setOptions(new FindOptions());
    name.prepare();
  }

  // test a matching name (same case)
  @Test
  public void applyMatch() throws IOException {
    setup("name");
    PathData item = new PathData("/directory/path/name", mockFs.getConf());
    assertEquals(Result.PASS, name.apply(item, -1));
  }

  // test a non-matching name
  @Test
  public void applyNotMatch() throws IOException {
    setup("name");
    PathData item = new PathData("/directory/path/notname", mockFs.getConf());
    assertEquals(Result.FAIL, name.apply(item, -1));
  }

  // test a matching name (different case)
  @Test
  public void applyMixedCase() throws IOException {
    setup("name");
    PathData item = new PathData("/directory/path/NaMe", mockFs.getConf());
    assertEquals(Result.PASS, name.apply(item, -1));
  }

  // test a matching glob pattern (same case)
  @Test
  public void applyGlob() throws IOException {
    setup("n*e");
    PathData item = new PathData("/directory/path/name", mockFs.getConf());
    assertEquals(Result.PASS, name.apply(item, -1));
  }

  // test a matching glob pattern (different case)
  @Test
  public void applyGlobMixedCase() throws IOException {
    setup("n*e");
    PathData item = new PathData("/directory/path/NaMe", mockFs.getConf());
    assertEquals(Result.PASS, name.apply(item, -1));
  }

  // test a non-matching glob pattern
  @Test
  public void applyGlobNotMatch() throws IOException {
    setup("n*e");
    PathData item = new PathData("/directory/path/notmatch", mockFs.getConf());
    assertEquals(Result.FAIL, name.apply(item, -1));
  }
}
