package org.apache.hcatalog.pig;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.apache.hcatalog.data.schema.HCatFieldSchema;
import org.apache.hcatalog.data.schema.HCatSchema;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceSchema.ResourceFieldSchema;
import org.apache.pig.data.DataType;
import org.junit.Test;

public class TestPigHCatUtil {

  @Test
  public void testGetBagSubSchema() throws Exception {

    HCatSchema hCatSchema = new HCatSchema(Lists.newArrayList(
        new HCatFieldSchema("asdf", HCatFieldSchema.Type.STRING, "asdf comment")));
    HCatFieldSchema hCatFieldSchema =
        new HCatFieldSchema("llama", HCatFieldSchema.Type.ARRAY, hCatSchema, "heyyy llama llama");
    ResourceSchema actual = PigHCatUtil.getBagSubSchema(hCatFieldSchema);

    ResourceFieldSchema[] bagSubFieldSchemas = new ResourceFieldSchema[1];
    bagSubFieldSchemas[0] = new ResourceFieldSchema().setName("innertuple")
        .setDescription("The tuple in the bag")
        .setType(DataType.TUPLE);

    ResourceFieldSchema[] innerTupleFieldSchemas = new ResourceFieldSchema[1];
    innerTupleFieldSchemas[0] = new ResourceFieldSchema().setName("innerfield").setType(DataType.CHARARRAY);

    bagSubFieldSchemas[0].setSchema(new ResourceSchema().setFields(innerTupleFieldSchemas));
    ResourceSchema expected = new ResourceSchema().setFields(bagSubFieldSchemas);

    Assert.assertEquals(expected.toString(), actual.toString());
  }
}
