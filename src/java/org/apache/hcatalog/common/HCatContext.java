/*
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

package org.apache.hcatalog.common;

import org.apache.hadoop.conf.Configuration;

import java.util.Map;

/**
 * HCatContext provides global access to configuration data.
 */
public class HCatContext {

  private static final HCatContext hCatContext = new HCatContext();

  private Configuration conf = new Configuration();

  private HCatContext() {
  }

  public static HCatContext getInstance() {
    return hCatContext;
  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
  }

  /**
   * Merge the given configuration into the HCatContext conf, overwriting any existing keys.
   */
  public void mergeConf(Configuration conf) {
    for (Map.Entry<String, String> entry : conf) {
      this.conf.set(entry.getKey(), entry.getValue());
    }
  }
}
