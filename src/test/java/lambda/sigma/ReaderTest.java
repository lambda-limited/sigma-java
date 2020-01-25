/*
	Lambda Limited
 */
package lambda.sigma;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author James Thorpe
 * 
 * 
 */
public class ReaderTest {

    public ReaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of close method, of class Reader.
     */
    @Test
    public void testConstants() throws Exception {
        System.out.println("Constant Tests");
        assertEquals(true, read("&t"));
        assertEquals(false, read("&f"));
        assertNull(read("&n"));
        readErr("&x", "invalid constant");
    }

    @Test
    public void testNumbers() throws Exception {
        System.out.println("Number Tests");
        assertEquals(BigDecimal.ZERO, read("0"));
        assertEquals(BigDecimal.valueOf(123), read("123"));
        assertEquals(BigDecimal.valueOf(1.2), read("1.2"));
        assertEquals(new BigDecimal("-1.23e-97"), read("-1.23e-97"));
        assertEquals(new BigDecimal("+1.23E+97"), read("+1.23E+97"));
    }

    @Test
    public void testBytes() throws Exception {      
        System.out.println("byte[] tests");
        byte[] bytes = new byte[512];
        for(int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte)(i & 0xFF);
        }
        Object result = writeread(bytes);
        assertTrue(result instanceof byte[]);
        assertTrue(Arrays.equals((byte[])result, bytes));
    }
    
    @Test
    public void testBase64() throws Exception {
        System.out.println("Base 64 tests");
        Object o = read("*VGhlIHF1aWNrIGJyb3duIGZveA");
        assertTrue(o instanceof byte[]);
        assertEquals("The quick brown fox", new String((byte[])o));
        o = read("*");
        assertTrue(o instanceof byte[]);
        assertTrue(((byte[])o).length == 0);
    }    
    
    @Test
    public void testDates() throws Exception {
        System.out.println("Date Tests");
        assertEquals(LocalDate.parse("2019-01-01"), read("@2019-01-01"));
        assertEquals(LocalTime.parse("12:31:47.7654"), read("@12:31:47.7654"));
        assertEquals(LocalTime.parse("12:00:00.123456789"), read("@12:00:00.123456789"));
        readErr("@12:00:00.1234567891", "invalid fraction");
        assertEquals(OffsetTime.parse("12:31:47+11:00"), read("@12:31:47+11:00"));
        assertEquals(OffsetTime.parse("12:31:47-11:00"), read("@12:31:47-11:00"));
        assertEquals(OffsetTime.parse("12:31:47Z"), read("@12:31:47Z"));
        assertEquals(LocalDateTime.parse("2019-01-01T12:31:47.7654"), read("@2019-01-01T12:31:47.7654"));
        assertEquals(OffsetDateTime.parse("2019-01-01T12:31:47.7654-01:00"), read("@2019-01-01T12:31:47.7654-01:00"));
        assertEquals(OffsetDateTime.parse("2019-01-01T12:31:47Z"), read("@2019-01-01T12:31:47Z"));
        assertEquals(ZonedDateTime.parse("2019-01-01T12:31:47.7654+11:00[Australia/Hobart]"), read("@2019-01-01T12:31:47.7654+11:00[Australia/Hobart]"));
        readErr("@2019-01-01T12:31:47.7654+11:00[Australia/Bogansville]", "invalid time zone");
    }

    @Test
    public void testStrings() throws Exception {
        System.out.println("String Tests");
        assertEquals("", read("\"\""));
        assertEquals("test", read("\"test\""));
        assertEquals("a\"b", read("\"a\\\"b\""));
        assertEquals("\\\"", read("\"\\\\\\\"\""));
        assertEquals("\r\n\t", read("\"\\r\\n\\t\""));
        assertEquals("\u0000", read("\"\\u0000\""));
        assertEquals("\u0001", read("\"\\u0001\""));        
        assertEquals("\u0ABC", read("\"\\u0ABC\""));       
        assertEquals("\u0abc", read("\"\\u0abc\""));         
        testUtf8CodePoints(0x7f, 0x7FF);
        testUtf8CodePoints(0x800, 0x9FF);
        testUtf8CodePoints(0xE000, 0xE0FF);
        testUtf8CodePoints(0x10000, 0x100FF);
    }

    @Test
    public void testList() throws Exception {
        System.out.println("List Tests");
        assertEquals("[]", readwrite("[]"));
        assertEquals("[]", readwrite(" [ ] "));
        assertEquals("[1,2,3,\"test\"]", readwrite("[1, 2 ,3, \"test\"]"));
    }

    @Test
    public void testMap() throws Exception {
        System.out.println("Map Tests");
        assertEquals("{}", readwrite("{}"));
        assertEquals("{}", readwrite(" { } "));
        assertEquals("{\"a\"=6}", readwrite("{\"a\"= 6}"));
        assertEquals("{\"a\"=6,\"b\"=&f,\"c\"=@2019-01-01}", readwrite("{\"a\"=6, \"b\"=&f , \"c\"=@2019-01-01}"));
        assertEquals("{9=9}", readwrite("{9=9}"));
    }

    @Test
    public void testObjects() throws Exception {
        System.out.println("Object Tests");
        Types.unregisterAll();
        readErr("model{}", "type 'model' is not registered");
        Types.register(TestModel.class, "model");
        assertEquals("model{b=54,bl=&f,bytes=|6|abcdef,i=55,l=56,ld=@2019-04-21,ldt=@2019-09-22T11:12:13.123456789,"
                + "list=[\"xyz\"],localTime=@08:09:10,map={\"abc\"=22},offsetTime=@07:08:09+11:00,s=23,str=\"test\",tree={4=16,5=25},"
                + "zonedDateTime=@2020-01-02T01:02:03.456+11:00[Australia/Hobart]}",
                readwrite("model { "
                        + "  b=54,"
                        + "  bl=&f,"
                        + "  i=55,"
                        + "  l=56,"
                        + "  ld=@2019-04-21,"
                        + "  ldt=@2019-09-22T11:12:13.123456789,"
                        + "  list=["
                        + "    \"xyz\""
                        + "  ],"
                        + "  localTime=@08:09:10,"
                        + "  map={"
                        + "    \"abc\"=22"
                        + "  },"
                        + "  offsetTime=@07:08:09+11:00,"
                        + "  s=23,"
                        + "  str=\"test\","
                        + "  tree={"
                        + "   5=25,"
                        + "   4=16"
                        + "  },"
                        + "  ZonedDateTime=@2020-01-02T01:02:03.456+11:00[Australia/Hobart]}"));
    }

    @Test
    public void testErrors() throws Exception {
        System.out.println("Error Tests");
        Types.register(TestModel.class, "model");
        assertNull(read(""));
    }

    private Object read(String s) throws IOException {
        Reader r = new Reader(s);
        return r.read();
    }

    private void readErr(String s, String msgFragment) throws IOException {
        Reader r = new Reader(s);
        try {
            r.read();
            fail();
        } catch (RuntimeException ex) {
            System.out.printf("expected error message containing '%s' but got '%s'\n", msgFragment, ex.getMessage());
            assertTrue(ex.getMessage().contains(msgFragment));
        }
    }

    // read the data and then write it back out again
    // this requires the writer tests to have passed
    private String readwrite(String s) throws IOException {
        Object value = read(s);
        return write(value);
    }
    
    // write the data and then read it back in again.
    // this requires the writer tests to have passed    
    private Object writeread(Object value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new Writer(out);
        w.write(value);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Reader r = new Reader(in);
        return r.read();
    }

    private String write(Object value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer w = new Writer(out);
        w.write(value);
        String result = out.toString(StandardCharsets.UTF_8);
        return result;
    }

    private void testUtf8CodePoints(int start, int end) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; ++i) {
            sb.appendCodePoint(i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(0x80);
        Writer w = new Writer(out);
        w.write(sb.toString());
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        Reader r = new Reader(in);
        Object result = r.read();
        assertTrue(result.equals(sb.toString()));
    }

}
