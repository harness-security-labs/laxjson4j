package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.NullNode;
import ai.traceable.laxjson4j.node.StringNode;
import org.junit.jupiter.api.Test;

class EnumConversionTest {

  enum Color {
    RED,
    GREEN,
    BLUE
  }

  @Test
  void validEnumConstant() {
    assertEquals(Color.RED, NodeConverter.convert(new StringNode("RED"), Color.class));
    assertEquals(Color.GREEN, NodeConverter.convert(new StringNode("GREEN"), Color.class));
    assertEquals(Color.BLUE, NodeConverter.convert(new StringNode("BLUE"), Color.class));
  }

  @Test
  void invalidEnumConstantThrows() {
    JsonConversionException ex =
        assertThrows(
            JsonConversionException.class,
            () -> NodeConverter.convert(new StringNode("INVALID"), Color.class));
    assertTrue(ex.getMessage().contains("INVALID"));
    assertTrue(ex.getMessage().contains("Color"));
  }

  @Test
  void nullNodeToEnum() {
    assertNull(NodeConverter.convert(NullNode.INSTANCE, Color.class));
    assertNull(NodeConverter.convert(null, Color.class));
  }
}
