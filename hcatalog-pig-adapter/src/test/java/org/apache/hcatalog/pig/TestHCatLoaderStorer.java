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

package org.apache.hcatalog.pig;

import org.apache.hadoop.fs.FileUtil;
import org.apache.hcatalog.HcatTestUtils;
import org.apache.hcatalog.mapreduce.HCatBaseTest;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

/**
 * Test that require both HCatLoader and HCatStorer. For read or write only functionality,
 * please consider @{link TestHCatLoader} or @{link TestHCatStorer}.
 */
public class TestHCatLoaderStorer extends HCatBaseTest {

  /**
   * Ensure Pig can read tinyint/smallint columns.
   */
  @Test
  public void testSmallTinyInt() throws Exception {

    String tblName = "test_small_tiny_int";
    File dataDir = new File(TEST_DATA_DIR + "/testSmallTinyIntData");
    File dataFile = new File(dataDir, "testSmallTinyInt.tsv");

    FileUtil.fullyDelete(dataDir); // Might not exist
    Assert.assertTrue(dataDir.mkdir());

    // TODO(travis): Put values outside range make sure its null. Should we throw an exception?
    String[] lines = new String[]{"256\t0", "32767\t127"};
    HcatTestUtils.createTestDataFile(dataFile.getAbsolutePath(), lines);

    // Create a table with smallint/tinyint columns, load data, and query from Hive.
    Assert.assertEquals(0, driver.run("drop table if exists " + tblName).getResponseCode());
    Assert.assertEquals(0, driver.run("create external table " + tblName +
        " (my_small_int smallint, my_tiny_int tinyint)" +
        " row format delimited fields terminated by '\t' stored as textfile").getResponseCode());
    Assert.assertEquals(0, driver.run("load data local inpath '" +
        dataDir.getAbsolutePath() + "' into table " + tblName).getResponseCode());
    Assert.assertEquals(0, driver.run("select * from " + tblName).getResponseCode());

    PigServer server = new PigServer(ExecType.LOCAL);
    server.registerQuery(
        "data = load '" + tblName + "' using org.apache.hcatalog.pig.HCatLoader();");

    // Ensure Pig schema is correct.
    Schema schema = server.dumpSchema("data");
    Assert.assertEquals(2, schema.getFields().size());
    Assert.assertEquals("my_small_int", schema.getField(0).alias);
    Assert.assertEquals(DataType.INTEGER, schema.getField(0).type);
    Assert.assertEquals("my_tiny_int", schema.getField(1).alias);
    Assert.assertEquals(DataType.INTEGER, schema.getField(1).type);

    // Ensure Pig can read data correctly.
    Iterator<Tuple> it = server.openIterator("data");
    Tuple t = it.next();
    Assert.assertEquals(256, t.get(0));
    Assert.assertEquals(0, t.get(1));
    t = it.next();
    Assert.assertEquals(32767, t.get(0));
    Assert.assertEquals(127, t.get(1));
    Assert.assertFalse(it.hasNext());
  }
}
