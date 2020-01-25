package lambda.sigma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author James Thorpe
 */
public class WriterTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    public WriterTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBase64() throws Exception {
        System.out.println("Base64 tests");
        assertEquals("*", write(new byte[]{}, false));
        assertEquals("*VGhlIHF1aWNrIGJyb3duIGZveA", write("The quick brown fox".getBytes(StandardCharsets.US_ASCII), false));
    }

    @Test
    public void testBinary() throws Exception {
        System.out.println("Base64 tests");
        assertEquals("|3|abc", write(new byte[]{0x61, 0x62, 0x63}));
        assertEquals("|0|", write(new byte[]{}));
    }

    @Test
    public void testBoolean() throws Exception {
        System.out.println("Boolean tests");
        assertEquals("&f", write(false));
        assertEquals("&t", write(true));
    }

    @Test
    public void testDates() throws Exception {
        System.out.println("Date tests");
        assertEquals("@2019-08-21", write(LocalDate.of(2019, Month.AUGUST, 21)));
        assertEquals("@10:23:56.123", write(LocalTime.parse("10:23:56.123")));
        assertEquals("@10:23:56.123Z", write(OffsetTime.parse("10:23:56.123Z")));
        assertEquals("@10:23:56.123+11:00", write(OffsetTime.parse("10:23:56.123+11:00")));
        assertEquals("@10:23:56.123-11:00", write(OffsetTime.parse("10:23:56.123-11:00")));
        assertEquals("@2019-08-21T10:11:12.123456789", write(LocalDateTime.parse("2019-08-21T10:11:12.123456789")));
        assertEquals("@2019-08-21T10:11:12+11:30", write(OffsetDateTime.parse("2019-08-21T10:11:12+11:30")));
        assertEquals("@2019-08-21T10:11:12-11:30", write(OffsetDateTime.parse("2019-08-21T10:11:12-11:30")));
        assertEquals("@2019-08-21T10:11:12Z", write(OffsetDateTime.parse("2019-08-21T10:11:12Z")));
        assertEquals("@2019-12-21T10:11:12+11:00[Australia/Hobart]", write(ZonedDateTime.parse("2019-12-21T10:11:12+11:00[Australia/Hobart]")));
    }

    @Test
    public void testList() throws Exception {
        System.out.println("List tests");
        ArrayList<Object> list = new ArrayList<>();
        assertEquals("[]", write(list));
        list.add(123);
        assertEquals("[123]", write(list));
        list.add("test");
        assertEquals("[123,\"test\"]", write(list));
        list.add(new ArrayList<>());
        assertEquals("[123,\"test\",[]]", write(list));
    }

    @Test
    public void testMap() throws Exception {
        System.out.println("Map tests");
        TreeMap<String, Object> map = new TreeMap<>();
        assertEquals("{}", write(map));
        map.put("a", 123);
        assertEquals("{\"a\"=123}", write(map));
        map.put("b", "test");
        assertEquals("{\"a\"=123,\"b\"=\"test\"}", write(map));
        map.put("c d", false);
        assertEquals("{\"a\"=123,\"b\"=\"test\",\"c d\"=&f}", write(map));
        map.put("e=f", null);
        assertEquals("{\"a\"=123,\"b\"=\"test\",\"c d\"=&f,\"e=f\"=&n}", write(map));
        HashMap<Object, Integer> hmap = new HashMap<>();
        hmap.put(1, 2);
        hmap.put("3", 4);
        assertEquals("{1=2,\"3\"=4}", write(hmap));
    }

    @Test
    public void testNull() throws Exception {
        System.out.println("null test");
        assertEquals("&n", write(null));
    }

    @Test
    public void testNumbers() throws Exception {
        System.out.println("Number tests");
        assertEquals("0", write(0));
        assertEquals("3", write((byte) 3));
        assertEquals("4", write((short) 4));
        assertEquals("123456789123", write(123456789123L));
        assertEquals("1.234", write(1.234f));
        assertEquals("1.234", write(1.234));
        assertEquals("1.23468273648723676E+5867", write(new BigDecimal("1.23468273648723676e5867")));
        assertEquals("1234682736487236765867", write(new BigInteger("1234682736487236765867")));
    }

    @Test
    public void testObject() throws Exception {
        System.out.println("Object tests");
        Types.unregisterAll();
        TestModel m = new TestModel();
        writeErr(m, "No object type registered for class");
        Types.register(TestModel.class, "model");
        String result = write(m);
        assertEquals("model{b=53,bl=&t,bytes=|6|abcdef,i=54,l=55,ld=@2019-03-21,ldt=@2019-08-22T10:11:12.123456789,"
                + "list=[\"abc\"],localTime=@10:11:12,map={\"xyx\"=99,9=9},offsetTime=@10:11:12+11:20,"
                + "s=56,str=\"string\",tree={2=4,3=9},zonedDateTime=@2020-01-01T00:00+11:00[Australia/Hobart]}",
                result);
    }

    @Test
    public void testString() throws Exception {
        System.out.println("String tests");
        assertEquals("\"\"", write(""));
        assertEquals("\"test\"", write("test"));
        assertEquals("\"a\\\"b\"", write("a\"b"));
        assertEquals("\"\\\\\\\"\"", write("\\\""));
        assertEquals("\"\\r\\n\\t\"", write("\r\n\t"));
        assertEquals("\"\\u0000\"", write("\u0000"));
        assertEquals("\"\\u0001\"", write("\u0001"));
        assertEquals("\"a\"", write("\u0061"));
        //test UTF-8 encodings
        testUtf8CodePoints(0x7f, 0x7FF);
        testUtf8CodePoints(0x800, 0x9FF);
        testUtf8CodePoints(0xE000, 0xE0FF);
        testUtf8CodePoints(0x10000, 0x100FF);
    }

    private void testUtf8CodePoints(int start, int end) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; ++i) {
            sb.appendCodePoint(i);
        }
        String test = "\"" + sb.toString() + "\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream(0x80);
        Writer w = new Writer(out);
        w.write(sb.toString());
        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(test.equals(result));
    }

    private String write(Object value, boolean allowBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer instance = new Writer(out);
        instance.write(value, allowBytes);
        String result = out.toString(StandardCharsets.UTF_8);
        return result;
    }

    private String write(Object value) throws IOException {
        return write(value, true);
    }

    private void writeErr(Object value, String msgFragment) throws IOException {
        try {
            write(value);
            fail();
        } catch (RuntimeException ex) {
            assertTrue(ex.getMessage().contains(msgFragment));
        }
    }

}
