package org.apache.hcatalog.common;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;

/**
 * HCatContext provides global access to configuration data.
 */
public class HCatContext {

  private static HCatContext hCatContext = null;

  private Configuration conf;

  private HCatContext(Configuration conf) {
    this.conf = Preconditions.checkNotNull(conf);
  }

  public static HCatContext get() {
    if (hCatContext != null) {
      return hCatContext;
    }
    throw new RuntimeException("HCatContext has not yet been configured.");
  }

  public static synchronized HCatContext get(Configuration conf) {
    if (hCatContext == null) {
      hCatContext = new HCatContext(conf);
    } else {
      hCatContext.conf = conf;
    }
    return hCatContext;
  }

  public Configuration getConf() {
    return conf;
  }
}
