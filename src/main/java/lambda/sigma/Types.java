package lambda.sigma;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 *
 * @author James Thorpe
 */
public class Types {

    private static final ConcurrentHashMap<Class<?>, String> TYPE_TO_TYPENAME = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Class<?>> TYPENAME_TO_TYPE = new ConcurrentHashMap<>();

    public static void register(Class<?> type, String typeName) {
        TYPE_TO_TYPENAME.put(type, typeName);
        TYPENAME_TO_TYPE.put(typeName, type);
    }

    public static Class<?> getType(String typeName) {
        return TYPENAME_TO_TYPE.get(typeName);
    }

    public static String getTypeName(Class<?> type) {
        return TYPE_TO_TYPENAME.get(type);
    }

    public static void unregisterAll() {
        TYPE_TO_TYPENAME.clear();
        TYPENAME_TO_TYPE.clear();
    }

   /**
     * coerce the value to be of type k.
     *
     * This is required when reading numeric values since the exact numeric type
     * cannot be determine by the Reader. The reader therefore reads
     * all numbers as a BigDecimal which can be narrowed for assignments.
     * coerceType will not convert floating point number to integers and will
     * not allow integer conversions that lose precision.
     *
     * coerce will also convert container types (maps and lists) provided the
     * containers implement the same interface.
     *
     * For example, map data can be assigned to a HashMap, TreeMap or
     * ConcurrentHashMap since they all inherit the Map interface.
     *
     * @param value
     * @param k
     * @return
     */
    public static Object coerceType(Object value, Class<?> k) throws SigmaException {
        try {
            if (k.isAssignableFrom(value.getClass())) {
                return value;  // assignment compatible already (ie value is a subclass of k)
            }
            if (value instanceof Boolean && boolean.class.isAssignableFrom(k)) {
                return value;
            }
            if (value instanceof BigDecimal) {
                BigDecimal bd = (BigDecimal) value;
                // check conversions in the "most likely" order
                if (BigDecimal.class.isAssignableFrom(k)) {
                    return bd;
                }
                if (int.class.isAssignableFrom(k)) {
                    return bd.intValueExact();
                }
                if (long.class.isAssignableFrom(k)) {
                    return bd.longValueExact();
                }
                if (float.class.isAssignableFrom(k)) {
                    return bd.floatValue();
                }
                if (double.class.isAssignableFrom(k)) {
                    return bd.doubleValue();
                }
                if (byte.class.isAssignableFrom(k)) {
                    return bd.byteValueExact();
                }
                if (short.class.isAssignableFrom(k)) {
                    return bd.shortValueExact();
                }
                if (Integer.class.isAssignableFrom(k)) {
                    return bd.intValueExact();
                }
                if (Long.class.isAssignableFrom(k)) {
                    return bd.longValueExact();
                }
                if (Float.class.isAssignableFrom(k)) {
                    return bd.floatValue();
                }
                if (Double.class.isAssignableFrom(k)) {
                    return bd.doubleValue();
                }
                if (Byte.class.isAssignableFrom(k)) {
                    return bd.byteValueExact();
                }
                if (Short.class.isAssignableFrom(k)) {
                    return bd.shortValueExact();
                }

            } else if (value instanceof HashMap && !k.isAssignableFrom(value.getClass())) {
                if (!Map.class.isAssignableFrom(k)) {
                    throw new SigmaException(String.format("unable to coerce map to type '%s'", k.getName()));
                }
                // convert to assignable map type.  Due to type erasure we cannot
                // be 100% certain that the elements are the right type for the
                // new map.
                return k.getConstructor(Map.class).newInstance(value);
            } else if (value instanceof ArrayList && !k.isAssignableFrom(value.getClass())) {
                if (!Collection.class.isAssignableFrom(k)) {
                    throw new SigmaException(String.format("unable to coerce list type '%s'", k.getName()));
                }
                // convert to assignable list type
                return k.getConstructor(Collection.class).newInstance(value);
            }
            throw new SigmaException(String.format("unable to coerce type '%s' to type '%s'", value.getClass().getName(), k.getName()));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | InstantiationException ex) {
            throw new SigmaException(String.format("unable to coerce type '%s' to type '%s'", value.getClass().getName(), k.getName()), ex);
        }
    }

}
