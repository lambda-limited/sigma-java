package lambda.sigma;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;

/**
 * @author James Thorpe
 * <p>
 * Reader for Lambda sigma serialisation format.
 * <p>
 * For pedagogical reasons the parser is implemented as a simple top-down parser
 * which closely mirrors the ABNF grammar for the format. It is more than 'fast
 * enough'(tm) for high volume use and the loss of simplicity caused by
 * optimisation is not worth it in my opinion.
 * <p>
 * The ABNF Grammar is as per https://tools.ietf.org/html/rfc5234
 * <p>
 * <pre>
 * value            =  number /
 *                     string /
 *                     temporal /
 *                     list /
 *                     map /
 *                     object /
 *                     constant /
 *                     binary
 *
 * number           =  [sign] int [frac] [exp]
 * int              =  [sign] 1*DIGIT
 * frac             =  "." 1*DIGIT
 * exp              =  ("e" / "E") [sign] DIGIT1-9 *DIGIT
 * sign             =  "-" / "+"
 *
 * string           =  DQUOTE
 * char             =  unescaped /
 *                     escape (
 *	                  DQUOTE /        ; quotation mark  U+0022
 *	                  "\"  /          ; reverse solidus U+005C
 *	                  "n"  /          ; line feed       U+000A
 *	                  "r"  /          ; carriage return U+000D
 *	                  "t"  /          ; tab             U+0009
 *	                  "u" 4HEXDIG )   ; codepoint       U+XXXX
 * unescaped        =  %x20-21 / %x23-5B / %x5D-10FFFF
 * escape           =  %x5C                 ; \
 *
 * temporal         =  "@" (zoned-datetime
 *                          offset-datetime /
 *                          offset-time /
 *                          local-datetime /
 *                          local-date /
 *                          local-time)
 * zoned-datetime   =  date "T" time offset "[" timezone "]"
 * offset-datetime  =  date "T" time offset
 * offset-date      =  date offset
 * offset-time      =  time offset
 * local-datetime   =  date "T" time
 * date             =  year "-" month "-" day
 * time             =  hour ":" min ":" sec nanos
 * offset           =  (sign hour ":" min) / Z
 * year             =  4DIGIT
 * month            =  2DIGIT
 * date             =  2DIGIT
 * hour             =  2DIGIT
 * min              =  2DIGIT
 * sec              =  2DIGIT
 * nanos            =  ["." 1*9DIGIT]
 *
 * list             =  "[" [WS / list-elements] "]"
 * list-elements    =  list-element *("," list-element)
 * list-element     =  WS value WS
 *
 * map              =  "{" [WS / map-elements] "}"
 * map-elements     =  map-element *("," map-element)
 * map-element      =  WS value WS ":" WS value WS
 *
 * object           =  identifier "{" [WS / object-fields] "}"
 * object-fields    =  object-field *("," object-field)
 * object-field     =  WS identifier WS ":" WS value WS
 * identifier       =  ALPHA *ALPHANUM
 *
 * constant         = "&" (null / true / false)
 * null             = "n"
 * true             = "t"
 * false            = "f"
 *
 * binary           = bytes / base64
 * bytes            = "|" length "|" *BYTE
 * length           = 1*DIGIT
 * base64           = "*" BASE64
 *
 * ALPHA            =  %x41-5A / %x61-7A          ; A-F, or a-f
 * ALPHANUM         =  ALPHA / DIGIT
 * BASE64           =  ALPHA / "+" / "/" / "="
 * BYTE             =  %x00-FF
 * DIGIT            =  %x30-39
 * DIGIT1-9         =  %x31-39
 * HEXDIG           =  DIGIT / %x41-46 / %x61-66  ; 0-9, A-F, or a-f
 * WS               =  *(CR LF HTAB SP)
 * </pre>
 */
public class Reader {

    /*
        The length of the utf8 octet sequence 
        based on the first octet in the sequence.  A length of zero
        indicates an illegal encoding.
     */
    static final byte[] UTF8_LENGTH = {
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    /*
        Adjustment values used to 'remove' the tag bits in each utf8
        sequence.  These are constant for any sequence of a given length
        and can be removed with a single subtraction.
     */
    static final int[] UTF8_TAG = {
        0x00000000,
        0x00000000,
        0x00003080,
        0x000E2080,
        0x03C82080
    };

    private int c;
    private InputStream in;
    private int pos = 0;

    public Reader(InputStream in) {
        this.in = in;
    }

    public Reader(String input) {
        this(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    }

    public <T> T read(Class<T> klass) throws IOException {
        return klass.cast(read());
    }

    public Object read() throws IOException {
        readChar();
        skipWs();
        if (c == -1) {
            return null;
        }
        Object value = readValue();
        skipWs();
        if (c != -1) {
            throw error("end of input expected");
        }
        return value;
    }

    private boolean consume(int ch) throws IOException {
        if (ch == c) {
            readChar();
            return true;
        }
        return false;
    }

    private int consumeDigit() throws IOException {
        int ch = c;
        if (isDigit()) {
            readChar();
            return ch - '0';
        }
        throw error("digit expected but '%c' found", ch);
    }

    private void consumeOrError(int ch) throws IOException {
        if (ch == c) {
            readChar();
        } else {
            throw error("'%c' expected but '%c' found", ch, c);
        }
    }

    private SigmaException error(String msg, Object... params) {
        String m = String.format("Reader Error [%d]: %s", pos, msg);
        return new SigmaException(String.format(m, params));
    }

    private SigmaException error(Throwable cause, String msg, Object... params) {
        String m = String.format("Reader Error [%d]: %s", pos, msg);
        return new SigmaException(String.format(m, params), cause);
    }

    private boolean isDigit() {
        return c >= '0' && c <= '9';
    }

    /* 
    separator characters that can occur after values in lists and maps
    and after identifiers in objects
     */
    private boolean isSeparator(int ch) {
        return ch == '=' || ch == ',' || ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == -1;
    }

    private boolean isWs(int ch) {
        return ch == 0x20 || ch == 0x09 || ch == 0x0a || ch == 0x0d;
    }

    private byte[] readBase64() throws IOException {
        readChar();
        String bytes = readToken();
        return Base64.getDecoder().decode(bytes);
    }

    /*
    Read next character from reader keeping track of the position within the
    file for the purposes of error reporting.
     */
    private void readByte() throws IOException {
        c = in.read();
        ++pos;
    }

    private byte[] readBytes() throws IOException {
        readChar();
        int len = 0;
        while (c != '|') {
            len = (len * 10) + (c - '0');
            readChar();
        }
        byte[] b = new byte[len];
        int totalRead = 0;
        while (true) {
            int bytesRead = in.read(b, totalRead, len - totalRead);
            if (bytesRead == -1) {
                throw error("unexpected end of input in binary data");
            }
            if ((totalRead += bytesRead) == len) {
                break;
            }
        }
        readChar();
        return b;
    }

    /*
    Read and decode UTF-8 encoded character from input stream
     */
    private void readChar() throws IOException {
        readByte();
        if (c == -1) {
            return;
        }
        int utf8 = c;
        int len = UTF8_LENGTH[c];
        for (int i = 1; i < len; ++i) {
            readByte();
            utf8 = (utf8 << 6) + c;
        }
        utf8 -= UTF8_TAG[len];
        c = utf8;
    }

    private Temporal readDate() throws IOException {
        int state = 0;
        boolean hasDate = false;
        boolean hasTime = false;
        boolean hasOffset = false;
        boolean hasZone = false;
        int year = 0;
        int month = 1;
        int day = 1;
        int hour = 0;
        int minute = 0;
        int second = 0;
        int fraction = 0;
        boolean offsetUtc = false;
        boolean offsetNeg = false;
        int offsetHour = 0;
        int offsetMin = 0;
        String zone = null;

        readChar(); // skip @
        while (true) {
            switch (state) {
                case 0:  // yyyy or hh
                    year = consumeDigit();
                    year = year * 10 + consumeDigit();
                    if (consume(':')) {
                        hour = year;
                        hasTime = true;
                        state = 1;
                        continue;
                    }
                    hasDate = true;
                    year = year * 10 + consumeDigit();
                    year = year * 10 + consumeDigit();
                    consumeOrError('-');
                    month = consumeDigit();
                    month = month * 10 + consumeDigit();
                    consumeOrError('-');
                    day = consumeDigit();
                    day = day * 10 + consumeDigit();
                    state = 1;
                    if (consume('T')) {
                        hasTime = true;
                        hour = consumeDigit();
                        hour = hour * 10 + consumeDigit();
                        consumeOrError(':');
                        state = 1;
                        continue;
                    }
                    state = 9;
                    break;
                case 1: //  mm:ss.sssssss (hh already read in state 0)
                    minute = consumeDigit();
                    minute = minute * 10 + consumeDigit();
                    consumeOrError(':');
                    second = consumeDigit();
                    second = second * 10 + consumeDigit();
                    // fractional second
                    if (consume('.')) {
                        int multiplier = 100000000;
                        fraction = consumeDigit() * multiplier;
                        while (isDigit()) {
                            multiplier /= 10;
                            if (multiplier == 0) {
                                throw error("invalid fraction of a second");
                            }
                            fraction += consumeDigit() * multiplier;
                        }
                    }
                    // offset
                    if (c == '-' || c == '+' || c == 'Z') {
                        state = 2;
                        continue;
                    }
                    state = 9;
                    break;
                case 2: // offset and zone
                    hasOffset = true;
                    if (!consume('Z')) {
                        offsetNeg = (c == '-');
                        readChar();
                        offsetHour = consumeDigit();
                        offsetHour = offsetHour * 10 + consumeDigit();
                        consumeOrError(':');
                        offsetMin = consumeDigit();
                        offsetMin = offsetMin * 10 + consumeDigit();
                    } else {
                        offsetUtc = true;
                    }
                    if (c == '[') {
                        hasZone = true;
                        readChar();
                        zone = readToken();
                        consumeOrError(']');
                    }
                    state = 9;
                    break;
                case 9: // construct date from parsed parts
                    boolean valid = (month >= 1 && month <= 12)
                            && (day >= 1 && day <= Month.of(month).length(Year.isLeap(year)))
                            && (hour >= 0 && hour <= 23)
                            && (minute >= 0 && minute <= 59)
                            && (second >= 0 && second <= 59)
                            && ((offsetHour * 100 - offsetMin) >= -1200)
                            && ((offsetHour * 100 + offsetMin) <= 1400)
                            && (offsetMin >= 0 && offsetMin <= 59);
                    if (!valid) {
                        throw error("invalid time or date");
                    }
                    if (offsetNeg) {
                        offsetHour = -offsetHour;
                        offsetMin = -offsetMin;
                    }
                    if (hasZone) { // ZonedDateTime
                        try {
                            ZoneId zoneId = ZoneId.of(zone);
                            return ZonedDateTime.of(year, month, day, hour, minute, second, fraction, zoneId);
                        } catch (Exception ex) {
                            throw error("invalid time zone");
                        }
                    }
                    if (hasOffset && hasDate && hasTime) {
                        ZoneOffset offset = offsetUtc ? ZoneOffset.UTC : ZoneOffset.ofHoursMinutes(offsetHour, offsetMin);
                        return OffsetDateTime.of(year, month, day, hour, minute, second, fraction, offset);
                    }
                    if (hasOffset && hasTime) {
                        ZoneOffset offset = offsetUtc ? ZoneOffset.UTC : ZoneOffset.ofHoursMinutes(offsetHour, offsetMin);
                        return OffsetTime.of(hour, minute, second, fraction, offset);
                    }
                    if (hasDate && hasTime) {
                        return LocalDateTime.of(year, month, day, hour, minute, second, fraction);
                    }
                    if (hasDate) {
                        return LocalDate.of(year, month, day);
                    }
                    return LocalTime.of(hour, minute, second, fraction);
            }
        }
    }

    private int readHexDigit() throws IOException {
        int ch = c;
        if (ch >= '0' && ch <= '9') {
            readChar();
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'F') {
            readChar();
            return ch + 10 - 'A';
        }
        if (ch >= 'a' && ch <= 'f') {
            readChar();
            return ch + 10 - 'a';
        }
        throw error("invalid hex digit '%c'", ch);
    }

    private List<Object> readList() throws IOException {
        readChar(); // skip '['
        List<Object> list = new ArrayList<>();
        skipWs();
        if (!consume(']')) {
            readListElements(list);
            consumeOrError(']');
        }
        return list;
    }

    private void readListElement(List<Object> list) throws IOException {
        skipWs();
        Object value = readValue();
        skipWs();
        list.add(value);
    }

    private void readListElements(List<Object> list) throws IOException {
        readListElement(list);
        while (consume(',')) {
            readListElement(list);
        }
    }

    private HashMap<Object, Object> readMap() throws IOException {
        readChar(); // skip '{'
        HashMap<Object, Object> map = new HashMap<>();
        skipWs();
        if (!consume('}')) {
            readMapElements(map);
            consumeOrError('}');
        }
        return map;
    }

    private void readMapElement(HashMap<Object, Object> map) throws IOException {
        skipWs();
        Object key = readValue();
        skipWs();
        consumeOrError('=');
        skipWs();
        Object value = readValue();
        skipWs();
        map.put(key, value);
    }

    private void readMapElements(HashMap<Object, Object> map) throws IOException {
        readMapElement(map);
        while (consume(',')) {
            readMapElement(map);
        }
    }

    /*
    readNumber() since the standard Java BigDecimal has the same format
    as the lambda number we do not have to parse
     */
    private Number readNumber() throws IOException {
        try {
            return new BigDecimal(readToken());
        } catch (NumberFormatException ex) {
            throw error("invalid number");
        }
    }

    private Object readObject() throws IOException {
        String identifier = readToken();
        skipWs();
        consumeOrError('{');
        Class<?> klass = Types.getType(identifier);
        if (klass == null) {
            throw error("type '%s' is not registered", identifier);
        }
        try {
            Object object = klass.getConstructor().newInstance();
            readObjectFields(object);
            consumeOrError('}');
            return object;
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw error(ex, "unable to create instance of type '%s'", identifier);
        }
    }

    private void readObjectField(Object object, PropertyDescriptor[] props) throws IOException {
        skipWs();
        String identifier = readToken();
        skipWs();
        consumeOrError('=');
        skipWs();
        Object value = readValue();
        skipWs();

        for (int i = 0; i < props.length; ++i) {
            if (props[i].getName().equalsIgnoreCase(identifier)) {
                try {
                    Class<?> k = props[i].getPropertyType();
                    value = Types.coerceType(value, k);
                    props[i].getWriteMethod().invoke(object, value);
                    return;
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    throw error("unable to set property %s.%s", object.getClass().getName(), identifier);
                }
            }
        }
        throw error("unable to find property %s.%s", object.getClass().getName(), identifier);
    }

    private void readObjectFields(Object object) throws IOException {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            readObjectField(object, props);
            while (consume(',')) {
                readObjectField(object, props);
            }
        } catch (IntrospectionException ex) {
            throw error(ex.getMessage());
        }
    }

    private String readString() throws IOException {
        readChar(); // skip '"'
        StringBuilder sb = new StringBuilder();
        while (!consume('"')) {
            if (consume('\\')) {
                switch (c) {
                    case '\\':
                    case '"':
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'u':
                        readChar();
                        int ch = (readHexDigit() << 12)
                                + (readHexDigit() << 8)
                                + (readHexDigit() << 4)
                                + (readHexDigit());
                        sb.appendCodePoint(ch);
                        continue; // while()
                }
            }
            sb.appendCodePoint(c);
            readChar();
        }

        return sb.toString();
    }

    // simple state machine to parse datetime variations
    private Temporal readTemporal() throws IOException {
        readChar(); // skip '@'
        try {
            String token = readToken();
            if (token.charAt(2) == ':') {  // writer guarantees token size > 2
                // if char[2] = ':' we have a time variant
                if (token.indexOf('-', 8) >= 0 || token.indexOf('+', 8) >= 0 || token.indexOf('Z', 8) >= 0) {
                    return OffsetTime.parse(token, DateTimeFormatter.ISO_OFFSET_TIME);
                } else {
                    return LocalTime.parse(token, DateTimeFormatter.ISO_LOCAL_TIME);
                }
            } else if (token.length() == 10) {
                // local dates are always 10 characters in length
                return LocalDate.parse(token, DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                // We have a datetime variant

                // readToken will not have read the IANA timezone because
                // "[" is regarded as a terminal token character.  So if the
                // next input character is "[" we have need to read the
                // timezone and parse as a ZonedDateTime
                if (c == '[') {
                    readChar();  // skip [
                    String timezone = readToken();
                    if (!consume(']')) {
                        throw error("invalid date timezone");
                    }
                    token = token + "[" + timezone + "]";
                    return ZonedDateTime.parse(token, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                }
                // if we have an offset then it is an OffsetDateTime
                if (token.indexOf('-', 10) >= 0 || token.indexOf('+', 10) >= 0 || token.indexOf('Z', 10) >= 0) {
                    return OffsetDateTime.parse(token, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
                // otherwise we have a LocalDateTime
                return LocalDateTime.parse(token, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (DateTimeParseException ex) {
            throw error("invalid date or time");
        }
    }

    private String readToken() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (!isSeparator(c) && !isWs(c)) {
            sb.appendCodePoint(c);
            readChar();
        }
        return sb.toString();
    }

    private Object readValue() throws IOException {
        switch (c) {
            case '"':
                return readString();
            case '[':
                return readList();
            case '{':
                return readMap();
            case '+':
            case '-':
                return readNumber();
            case '@':
                return readDate();
            case '|':
                return readBytes();
            case '*':
                return readBase64();
            case '&':
                readChar();
                if (consume('n')) {
                    return null;
                }
                if (consume('t')) {
                    return true;
                }
                if (consume('f')) {
                    return false;
                }
                throw error("invalid constant");
            default:
                if (c >= '0' && c <= '9') {
                    return readNumber();
                } else {
                    return readObject();
                }
        }
    }

    private void skipWs() throws IOException {
        while (isWs(c)) {
            readChar();
        }
    }

}
