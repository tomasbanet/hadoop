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
package org.apache.hadoop.fs.viewfs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.FsConstants;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.ContractTestUtils;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The FileStatus is being serialized in MR as jobs are submitted.
 * Since viewfs has overlayed ViewFsFileStatus, we ran into
 * serialization problems. THis test is test the fix.
 */
public class TestViewfsFileStatus {

  private static final File TEST_DIR = GenericTestUtils.getTestDir(
      TestViewfsFileStatus.class.getSimpleName());

  @BeforeEach
  public void setUp() {
    FileUtil.fullyDelete(TEST_DIR);
    assertTrue(TEST_DIR.mkdirs());
  }

  @AfterEach
  public void tearDown() throws IOException {
    FileUtil.fullyDelete(TEST_DIR);
  }

  @Test
  public void testFileStatusSerialziation()
      throws IOException, URISyntaxException {
    String testfilename = "testFileStatusSerialziation";
    TEST_DIR.mkdirs();
    File infile = new File(TEST_DIR, testfilename);
    final byte[] content = "dingos".getBytes();

    try (FileOutputStream fos =  new FileOutputStream(infile)) {
      fos.write(content);
    }
    assertEquals((long)content.length, infile.length());

    Configuration conf = new Configuration();
    ConfigUtil.addLink(conf, "/foo/bar/baz", TEST_DIR.toURI());
    try (FileSystem vfs = FileSystem.get(FsConstants.VIEWFS_URI, conf)) {
      assertEquals(ViewFileSystem.class, vfs.getClass());
      Path path = new Path("/foo/bar/baz", testfilename);
      FileStatus stat = vfs.getFileStatus(path);
      assertEquals(content.length, stat.getLen());
      ContractTestUtils.assertNotErasureCoded(vfs, path);
      assertTrue(stat.toString().contains("isErasureCoded=false"),
          path + " should have erasure coding unset in " +
          "FileStatus#toString(): " + stat);

      // check serialization/deserialization
      DataOutputBuffer dob = new DataOutputBuffer();
      stat.write(dob);
      DataInputBuffer dib = new DataInputBuffer();
      dib.reset(dob.getData(), 0, dob.getLength());
      FileStatus deSer = new FileStatus();
      deSer.readFields(dib);
      assertEquals(content.length, deSer.getLen());
      assertFalse(deSer.isErasureCoded());
    }
  }

  /**
   * Tests the ACL returned from getFileStatus for directories and files.
   * @throws IOException
   */
  @Test
  public void testListStatusACL() throws IOException {
    String testfilename = "testFileACL";
    String childDirectoryName = "testDirectoryACL";
    TEST_DIR.mkdirs();
    File infile = new File(TEST_DIR, testfilename);
    final byte[] content = "dingos".getBytes();

    try (FileOutputStream fos =  new FileOutputStream(infile)) {
      fos.write(content);
    }
    assertEquals(content.length, infile.length());
    File childDir = new File(TEST_DIR, childDirectoryName);
    childDir.mkdirs();

    Configuration conf = new Configuration();
    ConfigUtil.addLink(conf, "/file", infile.toURI());
    ConfigUtil.addLink(conf, "/dir", childDir.toURI());
    conf.setBoolean(Constants.CONFIG_VIEWFS_MOUNT_LINKS_AS_SYMLINKS, false);
    try (FileSystem vfs = FileSystem.get(FsConstants.VIEWFS_URI, conf)) {
      assertEquals(ViewFileSystem.class, vfs.getClass());
      FileStatus[] statuses = vfs.listStatus(new Path("/"));

      FileSystem localFs = FileSystem.getLocal(conf);
      FileStatus fileStat = localFs.getFileStatus(new Path(infile.getPath()));
      FileStatus dirStat = localFs.getFileStatus(new Path(childDir.getPath()));

      for (FileStatus status : statuses) {
        if (status.getPath().getName().equals("file")) {
          assertEquals(fileStat.getPermission(), status.getPermission());
        } else {
          assertEquals(dirStat.getPermission(), status.getPermission());
        }
      }

      localFs.setPermission(new Path(infile.getPath()),
          FsPermission.valueOf("-rwxr--r--"));
      localFs.setPermission(new Path(childDir.getPath()),
          FsPermission.valueOf("-r--rwxr--"));

      statuses = vfs.listStatus(new Path("/"));
      for (FileStatus status : statuses) {
        if (status.getPath().getName().equals("file")) {
          assertEquals(FsPermission.valueOf("-rwxr--r--"),
              status.getPermission());
          assertFalse(status.isDirectory());
        } else {
          assertEquals(FsPermission.valueOf("-r--rwxr--"),
              status.getPermission());
          assertTrue(status.isDirectory());
        }
      }
    }
  }

  // Tests that ViewFileSystem.getFileChecksum calls res.targetFileSystem
  // .getFileChecksum with res.remainingPath and not with f
  @Test
  public void testGetFileChecksum() throws IOException {
    final Path path = new Path("/tmp/someFile");
    FileSystem mockFS = Mockito.mock(FileSystem.class);
    InodeTree.ResolveResult<FileSystem> res =
        new InodeTree.ResolveResult<FileSystem>(null, mockFS, null,
            new Path("someFile"), true);
    @SuppressWarnings("unchecked")
    InodeTree<FileSystem> fsState = Mockito.mock(InodeTree.class);
    Mockito.when(fsState.resolve(path.toString(), true)).thenReturn(res);
    ViewFileSystem vfs = Mockito.mock(ViewFileSystem.class);
    vfs.fsState = fsState;

    Mockito.when(vfs.getFileChecksum(path)).thenCallRealMethod();
    Mockito.when(vfs.getUriPath(path)).thenCallRealMethod();
    vfs.getFileChecksum(path);

    Mockito.verify(mockFS).getFileChecksum(new Path("someFile"));
  }

  @AfterAll
  public static void cleanup() throws IOException {
    FileUtil.fullyDelete(TEST_DIR);
  }

}
