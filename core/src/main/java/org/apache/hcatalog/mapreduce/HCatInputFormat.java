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

package org.apache.hcatalog.mapreduce;

import java.io.IOException;
import java.util.Properties;

import com.google.common.base.Preconditions;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

import javax.annotation.Nullable;

/**
 * The InputFormat to use to read data from HCatalog.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class HCatInputFormat extends HCatBaseInputFormat {

    /**
     * @deprecated As of release 0.5, replaced by
     * {@link #setInput(Configuration, String, String, String, Properties)}
     * Will be removed in a future release.
     */
    @Deprecated
    public static void setInput(Job job, InputJobInfo inputJobInfo) throws IOException {
        setInput(job.getConfiguration(),
            inputJobInfo.getDatabaseName(),
            inputJobInfo.getTableName(),
            inputJobInfo.getFilter(),
            inputJobInfo.getProperties());
    }

    /**
     * @deprecated As of release 0.5, replaced by
     * {@link #setInput(Configuration, String, String, String, Properties)}.
     * Will be removed in a future release.
     */
    @Deprecated
    public static void setInput(Configuration conf, InputJobInfo inputJobInfo)
        throws IOException {
        try {
            InitializeInput.setInput(conf, inputJobInfo);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * Set inputs to use for the job. This queries the metastore with the given input
     * specification and serializes matching partitions into the job conf for use by MR tasks.
     *
     * @param conf the job configuration
     * @param dbName database name
     * @param tableName table name
     * @param filter filter specification
     * @param properties properties for the input specification
     * @throws IOException on all errors
     */
    public static void setInput(Configuration conf, String dbName, String tableName,
                                @Nullable String filter, @Nullable Properties properties)
        throws IOException {

        Preconditions.checkNotNull(conf, "Required argument conf is null");
        Preconditions.checkNotNull(dbName, "Required argument dbName is null");
        Preconditions.checkNotNull(tableName, "Required argument tableName is null");

        try {
            InitializeInput.setInput(conf, InputJobInfo.create(dbName, tableName, filter, properties));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
