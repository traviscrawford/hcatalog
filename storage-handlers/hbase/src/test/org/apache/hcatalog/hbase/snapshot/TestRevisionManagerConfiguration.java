package org.apache.hcatalog.hbase.snapshot;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

public class TestRevisionManagerConfiguration {

    @Test
    public void testDefault() {
        Configuration conf = RevisionManagerConfiguration.create();
        Assert.assertEquals("org.apache.hcatalog.hbase.snapshot.ZKBasedRevisionManager",
            conf.get(RevisionManagerFactory.REVISION_MGR_IMPL_CLASS));
    }
}
