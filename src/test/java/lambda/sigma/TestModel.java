package lambda.sigma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Simple test model with all the data types supported by Sigma.
 */
public class TestModel {
    private byte b = 53;
    private boolean bl = true;
    private byte[] bytes = "abcdef".getBytes();
    private int i = 54;
    private long l = 55;
    private LocalDate ld = LocalDate.of(2019, Month.MARCH, 21);
    private LocalDateTime ldt = LocalDateTime.parse("2019-08-22T10:11:12.123456789");
    private List<Object> list = new ArrayList();
    private LocalTime localTime = LocalTime.of(10, 11, 12);
    private HashMap<Object, Object> map = new HashMap<>();
    private OffsetTime offsetTime = OffsetTime.of(10, 11, 12, 0, ZoneOffset.ofHoursMinutes(11, 20));
    private short s = 56;
    private String str = "string";
    private TreeMap<Comparable, Integer> tree = new TreeMap<>();
    private ZonedDateTime zonedDateTime = ZonedDateTime.parse("2020-01-01T00:00:00+11:00[Australia/Hobart]");

    public TestModel() {
        list.add("abc");
        map.put("xyx", 99);
        map.put(9, 9);
        tree.put(3, 9);
        tree.put(2, 4);
    }

    public byte getB() {
        return b;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getI() {
        return i;
    }

    public long getL() {
        return l;
    }

    public LocalDate getLd() {
        return ld;
    }

    public LocalDateTime getLdt() {
        return ldt;
    }

    public List<Object> getList() {
        return list;
    }
    public LocalTime getLocalTime() {
        return localTime;
    }

    public HashMap<Object, Object> getMap() {
        return map;
    }
    public OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public short getS() {
        return s;
    }

    public String getStr() {
        return str;
    }

    public TreeMap<Comparable, Integer> getTree() {
        return tree;
    }
    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public boolean isBl() {
        return bl;
    }

    public void setB(byte b) {
        this.b = b;
    }

    public void setBl(boolean bl) {
        this.bl = bl;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void setI(int i) {
        this.i = i;
    }

    public void setL(long l) {
        this.l = l;
    }

    public void setLd(LocalDate ld) {
        this.ld = ld;
    }

    public void setLdt(LocalDateTime ldt) {
        this.ldt = ldt;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }
    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    public void setMap(HashMap<Object, Object> map) {
        this.map = map;
    }
    public void setOffsetTime(OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

    public void setS(short s) {
        this.s = s;
    }

    public void setStr(String str) {
        this.str = str;
    }

    // to test collection coercion.
    public void setTree(TreeMap<Comparable, Integer> tree) {
        this.tree = tree;
    }
    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }
}
