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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.MRConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;


/** 
 * TestMapOutputType checks whether the Map task handles type mismatch
 * between mapper output and the type specified in
 * JobConf.MapOutputKeyType and JobConf.MapOutputValueType.
 */
public class TestMapOutputType {
  private static final File TEST_DIR = new File(
      System.getProperty("test.build.data",
          System.getProperty("java.io.tmpdir")), "TestMapOutputType-mapred");
  JobConf conf = new JobConf(TestMapOutputType.class);
  JobClient jc;
  /** 
   * TextGen is a Mapper that generates a Text key-value pair. The
   * type specified in conf will be anything but.
   */
   
  static class TextGen
    implements Mapper<WritableComparable, Writable, Text, Text> {
    
    public void configure(JobConf job) {
    }
    
    public void map(WritableComparable key, Writable val,
                    OutputCollector<Text, Text> out,
                    Reporter reporter) throws IOException {
      out.collect(new Text("Hello"), new Text("World"));
    }
    
    public void close() {
    }
  }
  
  /** A do-nothing reducer class. We won't get this far, really.
   *
   */
  static class TextReduce
    implements Reducer<Text, Text, Text, Text> {
    
    public void configure(JobConf job) {
    }

    public void reduce(Text key,
                       Iterator<Text> values,
                       OutputCollector<Text, Text> out,
                       Reporter reporter) throws IOException {
      out.collect(new Text("Test"), new Text("Me"));
    }

    public void close() {
    }
  }

  @BeforeEach
  public void configure() throws Exception {
    Path testdir = new Path(TEST_DIR.getAbsolutePath());
    Path inDir = new Path(testdir, "in");
    Path outDir = new Path(testdir, "out");
    FileSystem fs = FileSystem.get(conf);
    fs.delete(testdir, true);
    conf.setInt(JobContext.IO_SORT_MB, 1);
    conf.setInputFormat(SequenceFileInputFormat.class);
    FileInputFormat.setInputPaths(conf, inDir);
    FileOutputFormat.setOutputPath(conf, outDir);
    conf.setMapperClass(TextGen.class);
    conf.setReducerClass(TextReduce.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class); 
    
    conf.set(MRConfig.FRAMEWORK_NAME, MRConfig.LOCAL_FRAMEWORK_NAME);   
 
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    if (!fs.mkdirs(testdir)) {
      throw new IOException("Mkdirs failed to create " + testdir.toString());
    }
    if (!fs.mkdirs(inDir)) {
      throw new IOException("Mkdirs failed to create " + inDir.toString());
    }
    Path inFile = new Path(inDir, "part0");
    SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, inFile,
                                                           Text.class, Text.class);
    writer.append(new Text("rec: 1"), new Text("Hello"));
    writer.close();
    
    jc = new JobClient(conf);
  }

  @AfterEach
  public void cleanup() {
    FileUtil.fullyDelete(TEST_DIR);
  }

  @Test
  public void testKeyMismatch() throws Exception {
    //  Set bad MapOutputKeyClass and MapOutputValueClass
    conf.setMapOutputKeyClass(IntWritable.class);
    conf.setMapOutputValueClass(IntWritable.class);
    
    RunningJob r_job = jc.submitJob(conf);
    while (!r_job.isComplete()) {
      Thread.sleep(1000);
    }
    
    if (r_job.isSuccessful()) {
      fail("Oops! The job was supposed to break due to an exception");
    }
  }

  @Test
  public void testValueMismatch() throws Exception {
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(IntWritable.class);
    
    RunningJob r_job = jc.submitJob(conf);
    while (!r_job.isComplete()) {
      Thread.sleep(1000);
    }
    
    if (r_job.isSuccessful()) {
      fail("Oops! The job was supposed to break due to an exception");
    }
  }

  @Test
  public void testNoMismatch() throws Exception{
    //  Set good MapOutputKeyClass and MapOutputValueClass
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(Text.class);
     
    RunningJob r_job = jc.submitJob(conf);
    while (!r_job.isComplete()) {
      Thread.sleep(1000);
    }
     
    if (!r_job.isSuccessful()) {
      fail("Oops! The job broke due to an unexpected error");
    }
  }
}
