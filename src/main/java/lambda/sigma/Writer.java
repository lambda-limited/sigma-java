package lambda.sigma;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.temporal.Temporal;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author James Thorpe
 */
public class Writer implements Closeable {

    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final Logger LOG = Logger.getLogger(Writer.class.getName());

    private final OutputStream out;
    private boolean allowBytes;

    public Writer(OutputStream out) {
        this.out = out;
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public void write(Object object) {
        write(object, true);
    }

    public void write(Object object, boolean allowBytes) {
        this.allowBytes = allowBytes;
        writeValue(object);
    }

    private void writeBase64(byte[] b) {
        writeChar('*');
        if (b.length > 0) {
            byte[] bytes = Base64.getEncoder().withoutPadding().encode(b);
            writeByteArray(bytes);
        }
    }

    protected void writeByte(int b) {
        try {
            out.write(b);
        } catch (IOException ex) {
            throw new SigmaException(ex.getMessage(), ex);
        }
    }

    protected void writeByteArray(byte[] bytes) {
        try {
            out.write(bytes);
        } catch (IOException ex) {
            throw new SigmaException(ex.getMessage(), ex);
        }
    }

    private void writeBytes(byte[] b) {
        writeChar('|');
        writeChars(Integer.toString(b.length));
        writeChar('|');
        if (b.length > 0) {
            writeByteArray(b);
        }
    }


    /*
    Encode character as UTF-8 byte sequence.  
     */
    private void writeChar(int c) throws SigmaException {
        if (c <= 0x7F) {
            writeByte(c);
        } else if (c <= 0x7FF) {
            writeByte(0xC0 | (c >> 6));
            writeByte(0x80 | (c & 0x3F));
        } else if (c <= 0xFFFF) {
            writeByte(0xE0 | (c >> 12));
            writeByte(0x80 | ((c >> 6) & 0x3F));
            writeByte(0x80 | (c & 0x3F));
        } else if (c <= 0x10FFFF) {
            writeByte(0xF0 | (c >> 18));
            writeByte(0x80 | ((c >> 12) & 0x3F));
            writeByte(0x80 | ((c >> 6) & 0x3F));
            writeByte(0x80 | (c & 0x3F));
        } else {
            throw new SigmaException("Character out of range for UTF-8 encoding");
        }
    }

    private void writeChars(String s) {
        s.codePoints().forEach(c -> writeChar(c));
    }

    private void writeList(Collection<Object> list) {
        writeChar('[');
        String separator = "";
        for (Object value : list) {
            writeChars(separator);
            writeValue(value);
            separator = ",";
        }
        writeChar(']');
    }

    private void writeMap(Map<Object, Object> map) {
        String separator = "";
        writeChar('{');
        for (Map.Entry<Object, Object> e : map.entrySet()) {
            writeChars(separator);
            writeValue(e.getKey());
            writeChar('=');
            writeValue(e.getValue());
            separator = ",";
        }
        writeChar('}');
    }

    private void writeNumber(Object object) {
        writeChars(object.toString());
    }

    private void writeObject(Object object) {
        try {
            String typeName = Types.getTypeName(object.getClass());
            if (typeName == null) {
                throw new SigmaException("No object type registered for class " + object.getClass().getName());
            }
            String separator = "";
            writeChars(typeName);
            writeChar('{');
            BeanInfo beanInfo = Introspector.getBeanInfo(object.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor prop : propertyDescriptors) {
                // don't write read only properites
                if (prop.getWriteMethod() != null) {
                    writeChars(separator);
                    writeChars(prop.getName());
                    writeChar('=');
                    Method getter = prop.getReadMethod();
                    Object value = getter.invoke(object);
                    writeValue(value);
                    separator = ",";
                }
            }
            writeChar('}');
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new SigmaException(ex.getMessage(), ex);
        }
    }

    private void writeString(String s) {
        writeChar('"');
        s.codePoints().forEach(c -> {
            switch (c) {
                case '"':
                    writeChars("\\\"");
                    break;
                case '\\':
                    writeChars("\\\\");
                    break;
                case '\n':
                    writeChars("\\n");
                    break;
                case '\r':
                    writeChars("\\r");
                    break;
                case '\t':
                    writeChars("\\t");
                    break;
                default:
                    if (c < ' ') {
                        writeChars("\\u00");
                        writeChar(HEX_CHARS[(c >> 4) & 0xF]);
                        writeChar(HEX_CHARS[c & 0xF]);
                    } else {
                        writeChar(c);
                    }
            }
        });
        writeChar('"');
    }

    private void writeTemporal(Object object) {
        writeChar('@');
        writeChars(object.toString());
    }

    private void writeValue(Object value) {
        if (value == null) {
            writeChars("&n");
        } else if (value instanceof Boolean) {
            writeChars(Boolean.class.cast(value) ? "&t" : "&f");
        } else if (value instanceof String) {
            writeString(value.toString());
        } else if (value instanceof Number) {
            writeNumber(value);
        } else if (value instanceof Temporal) {
            writeTemporal(value);
        } else if (value instanceof Map) {
            writeMap(Map.class.cast(value));
        } else if (value instanceof Collection) {
            writeList(Collection.class.cast(value));
        } else if (value instanceof byte[]) {
            if (allowBytes) {
                writeBytes((byte[]) value);
            } else {
                writeBase64((byte[]) value);
            }
        } else {
            writeObject(value);
        }
    }

}
