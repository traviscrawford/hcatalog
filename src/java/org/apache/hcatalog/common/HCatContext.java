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
 * HCatContext provides global access to configuration data. It uses a reference to the
 * job configuration so that all the settings can be passed to the backend.
 */
public class HCatContext {

  private static final HCatContext hCatContext = new HCatContext();

  private Configuration conf = null;

  private HCatContext() {
  }

  public static synchronized HCatContext setupHCatContext(Configuration conf) {
    if (hCatContext.conf == null) {
      if (conf == null) {
        throw new RuntimeException("Trying to set HCatContext with a null job conf.");
      } else {
        hCatContext.conf = conf;
      }
    } else {
      if (hCatContext.conf != conf) {
        // pass on the properties that are not in the new conf
        for (Map.Entry<String, String> entry : hCatContext.conf) {
          if (conf.get(entry.getKey()) == null) {
            conf.set(entry.getKey(), entry.getValue());
          }
        }
        hCatContext.conf = conf;
      } // else the same job conf
    }
    return hCatContext;
  }

  public static synchronized HCatContext getInstance() {
    if (hCatContext.conf == null) {
      throw new RuntimeException("HCatContext has not been set up yet.");
    }
    return hCatContext;
  }

  public Configuration getConf() {
    return conf;
  }
}
