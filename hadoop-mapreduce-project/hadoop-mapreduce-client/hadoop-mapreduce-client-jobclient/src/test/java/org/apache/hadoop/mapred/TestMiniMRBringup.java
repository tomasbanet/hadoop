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

package org.apache.hadoop.mapred;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.v2.MiniMRYarnCluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Unit-test to test bringup and shutdown of Mini Map-Reduce Cluster.
 */
public class TestMiniMRBringup {

  @Test
  public void testBringUp() throws IOException {
    MiniMRCluster mr = null;
    try {
      mr = new MiniMRCluster(1, "local", 1);
    } finally {
      if (mr != null) { mr.shutdown(); }
    }
  }

  @Test
  public void testMiniMRYarnClusterWithoutJHS() throws IOException {
    MiniMRYarnCluster mr = null;
    try {
      final Configuration conf = new Configuration();
      conf.setBoolean(MiniMRYarnCluster.MR_HISTORY_MINICLUSTER_ENABLED, false);
      mr = new MiniMRYarnCluster("testMiniMRYarnClusterWithoutJHS");
      mr.init(conf);
      mr.start();
      assertEquals(null, mr.getHistoryServer());
    } finally {
      if (mr != null) {
        mr.stop();
      }
    }
  }
}
