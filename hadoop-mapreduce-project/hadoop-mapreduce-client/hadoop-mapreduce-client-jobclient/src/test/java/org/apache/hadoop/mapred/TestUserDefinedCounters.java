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

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.IdentityMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUserDefinedCounters {
  private static String TEST_ROOT_DIR =
    new File(System.getProperty("test.build.data", "/tmp")).toURI()
    .toString().replace(' ', '+')
    + "/" + TestUserDefinedCounters.class.getName();

  private final Path INPUT_DIR = new Path(TEST_ROOT_DIR + "/input");
  private final Path OUTPUT_DIR = new Path(TEST_ROOT_DIR + "/out");
  private final Path INPUT_FILE = new Path(INPUT_DIR , "inp");

  enum EnumCounter { MAP_RECORDS }
  
  static class CountingMapper<K, V> extends IdentityMapper<K, V> {

    public void map(K key, V value,
        OutputCollector<K, V> output, Reporter reporter)
        throws IOException {
      output.collect(key, value);
      reporter.incrCounter(EnumCounter.MAP_RECORDS, 1);
      reporter.incrCounter("StringCounter", "MapRecords", 1);
    }

  }
  
  private void cleanAndCreateInput(FileSystem fs) throws IOException {
    fs.delete(INPUT_DIR, true);
    fs.delete(OUTPUT_DIR, true);

    OutputStream os = fs.create(INPUT_FILE);

    Writer wr = new OutputStreamWriter(os);
    wr.write("hello1\n");
    wr.write("hello2\n");
    wr.write("hello3\n");
    wr.write("hello4\n");
    wr.close();
  }

  @Test
  public void testMapReduceJob() throws Exception {

    JobConf conf = new JobConf(TestUserDefinedCounters.class);
    conf.setJobName("UserDefinedCounters");
    
    FileSystem fs = FileSystem.get(conf);
    cleanAndCreateInput(fs);
    
    conf.setInputFormat(TextInputFormat.class);

    conf.setMapOutputKeyClass(LongWritable.class);
    conf.setMapOutputValueClass(Text.class);

    conf.setOutputFormat(TextOutputFormat.class);
    conf.setOutputKeyClass(LongWritable.class);
    conf.setOutputValueClass(Text.class);

    conf.setMapperClass(CountingMapper.class);
    conf.setReducerClass(IdentityReducer.class);

    FileInputFormat.setInputPaths(conf, INPUT_DIR);

    FileOutputFormat.setOutputPath(conf, OUTPUT_DIR);

    RunningJob runningJob = JobClient.runJob(conf);

    Path[] outputFiles = FileUtil.stat2Paths(
        fs.listStatus(OUTPUT_DIR, 
                      new Utils.OutputFileUtils.OutputFilesFilter()));
    if (outputFiles.length > 0) {
      InputStream is = fs.open(outputFiles[0]);
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String line = reader.readLine();
      int counter = 0;
      while (line != null) {
        counter++;
        assertTrue(line.contains("hello"));
        line = reader.readLine();
      }
      reader.close();
      assertEquals(4, counter);
    }
    verifyCounters(runningJob, 4);
  }
  
  public static void verifyCounters(RunningJob runningJob, int expected)
  throws IOException {
    assertEquals(expected,
        runningJob.getCounters().getCounter(EnumCounter.MAP_RECORDS));
    assertEquals(expected,
        runningJob.getCounters().getGroup("StringCounter")
        .getCounter("MapRecords"));
  }

}
