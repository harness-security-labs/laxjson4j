package ai.traceable.laxjson4j.convert;

import static org.junit.jupiter.api.Assertions.*;

import ai.traceable.laxjson4j.exception.JsonConversionException;
import ai.traceable.laxjson4j.node.LongNode;
import ai.traceable.laxjson4j.node.StringNode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateTimeConversionTest {

  private static final long EPOCH_MILLIS = 1709294400000L; // 2024-03-01T12:00:00Z

  @Test
  void dateAndTimestampFromEpochMillis() {
    Date date = NodeConverter.convert(new LongNode(EPOCH_MILLIS), Date.class);
    assertEquals(EPOCH_MILLIS, date.getTime());

    Timestamp ts = NodeConverter.convert(new LongNode(EPOCH_MILLIS), Timestamp.class);
    assertEquals(EPOCH_MILLIS, ts.getTime());
  }

  @Test
  void dateAndTimestampFromIsoString() {
    String iso = "2024-03-01T12:00:00Z";
    Date date = NodeConverter.convert(new StringNode(iso), Date.class);
    assertEquals(EPOCH_MILLIS, date.getTime());

    Timestamp ts = NodeConverter.convert(new StringNode(iso), Timestamp.class);
    assertEquals(EPOCH_MILLIS, ts.getTime());
  }

  @Test
  void localDateAndLocalDateTimeFromIsoString() {
    LocalDate ld = NodeConverter.convert(new StringNode("2024-03-01"), LocalDate.class);
    assertEquals(LocalDate.of(2024, 3, 1), ld);

    LocalDateTime ldt =
        NodeConverter.convert(new StringNode("2024-03-01T12:30:00"), LocalDateTime.class);
    assertEquals(LocalDateTime.of(2024, 3, 1, 12, 30, 0), ldt);
  }

  @Test
  void localDateAndLocalDateTimeFromEpoch() {
    Instant instant = Instant.ofEpochMilli(EPOCH_MILLIS);
    LocalDate expectedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDateTime expectedDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

    assertEquals(expectedDate, NodeConverter.convert(new LongNode(EPOCH_MILLIS), LocalDate.class));
    assertEquals(
        expectedDateTime, NodeConverter.convert(new LongNode(EPOCH_MILLIS), LocalDateTime.class));
  }

  @Test
  void instantConversions() {
    Instant fromEpoch = NodeConverter.convert(new LongNode(EPOCH_MILLIS), Instant.class);
    assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), fromEpoch);

    Instant fromString =
        NodeConverter.convert(new StringNode("2024-03-01T12:00:00Z"), Instant.class);
    assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), fromString);
  }

  @Test
  void invalidDateStringThrows() {
    assertThrows(
        JsonConversionException.class,
        () -> NodeConverter.convert(new StringNode("not-a-date"), LocalDate.class));

    JsonConversionException ex =
        assertThrows(
            JsonConversionException.class,
            () -> NodeConverter.convert(new StringNode("abc"), Instant.class));
    assertTrue(ex.getMessage().contains("unparseable"));
  }
}
