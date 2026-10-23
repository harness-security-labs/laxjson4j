package ai.traceable.laxjson4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.node.JsonNode;
import ai.traceable.laxjson4j.parser.JsonParser;
import ai.traceable.laxjson4j.pool.ParserPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JsonPoolTest {

  private Json json;

  @BeforeEach
  void setUp() {
    json = new Json();
    json.acquire();
  }

  @Test
  void acquireReturnsNonNullWithValidParser() {
    ParserPool pool = json.acquire();
    assertNotNull(pool);
    assertNotNull(pool.parser());

    JsonNode node = pool.parser().parse("{\"a\": 1}");
    assertEquals(1, node.get("a").asInt());
    json.release(pool);
  }

  @Test
  void releaseAndReacquireReturnsSameInstance() {
    ParserPool first = json.acquire();
    json.release(first);

    ParserPool second = json.acquire();
    assertSame(first, second);
    json.release(second);
  }

  @Test
  void doubleAcquireWithoutReleaseReturnsDifferentInstances() {
    ParserPool first = json.acquire();
    ParserPool second = json.acquire();
    assertNotSame(first, second);

    json.release(first);
    json.release(second);
  }

  @Test
  void recycledParserWorksCorrectly() {
    ParserPool pool = json.acquire();
    JsonParser parser = pool.parser();

    JsonNode r1 = parser.parse("{\"x\": 10}");
    assertEquals(10, r1.get("x").asInt());

    json.release(pool);

    pool = json.acquire();
    parser = pool.parser();

    JsonNode r2 = parser.parse("[1, 2, 3]");
    assertEquals(3, r2.size());
    assertEquals(2, r2.get(1).asInt());

    json.release(pool);
  }

  @Test
  void oversizedTextBufferIsTrimmedOnRelease() {
    ParserPool pool = json.acquire();
    JsonParser parser = pool.parser();

    StringBuilder sb = new StringBuilder("{\"key\": \"");
    for (int i = 0; i < 100_000; i++) sb.append("\\n");
    sb.append("\"}");
    parser.parse(sb.toString());

    json.release(pool);

    ParserPool recycled = json.acquire();
    assertNotSame(parser, recycled.parser());
    json.release(recycled);
  }

  @Test
  void normalTextBufferIsRetainedOnRelease() {
    ParserPool pool = json.acquire();
    JsonParser parser = pool.parser();

    parser.parse("{\"key\": \"small value\"}");
    json.release(pool);

    ParserPool recycled = json.acquire();
    assertSame(parser, recycled.parser());
    json.release(recycled);
  }
}
