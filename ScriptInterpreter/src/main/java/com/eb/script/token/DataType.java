package com.eb.script.token;

import com.eb.script.json.Json;
import com.eb.script.arrays.ArrayDef;
import com.eb.script.image.EbsImage;
import com.eb.script.image.EbsVectorImage;
import com.eb.script.image.EbsCanvas;
import com.eb.util.Util;
import java.util.Date;

/**
 *
 * @author Earl Bosch
 */
public enum DataType {
    BYTE(Byte.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    FLOAT(Float.class),
    DOUBLE(Double.class),
    STRING(String.class),
    DATE(Date.class),
    BOOL(Boolean.class),
    JSON(Object.class),
    ARRAY(Object[].class),
    QUEUE(java.util.Queue.class),
    RECORD(java.util.Map.class),
    MAP(java.util.Map.class),
    BITMAP(Byte.class),
    INTMAP(Integer.class),
    BINARY(byte[].class),
    IMAGE(EbsImage.class),
    VECTOR_IMAGE(EbsVectorImage.class),
    CANVAS(EbsCanvas.class),
    ANY(Comparable.class);

    public final Class dataClass;
    public final DataType type;

    private DataType(Class dataClass) {
        this.dataClass = dataClass;
        this.type = this;
    }

    public Object[] getArray(int length) {
        if (null != type) {
            switch (type) {
                case BYTE -> {
                    return new Byte[length];
                }
                case ARRAY -> {
                    return new Object[length];
                }
                case QUEUE -> {
                    // Queues don't use this method directly (they use ArrayDeque internally)
                    // but kept for consistency with other DataTypes
                    return new Object[length];
                }
                case INTEGER -> {
                    return new Integer[length];
                }
                case LONG -> {
                    return new Long[length];
                }
                case FLOAT -> {
                    return new Float[length];
                }
                case DOUBLE -> {
                    return new Double[length];
                }
                case STRING -> {
                    return new String[length];
                }
                case DATE -> {
                    return new Date[length];
                }
                case BOOL -> {
                    return new Boolean[length];
                }
                case JSON -> {
                    return new Object[length];
                }
                case RECORD -> {
                    return new Object[length];
                }
                case MAP -> {
                    return new Object[length];
                }
                case BITMAP -> {
                    return new Byte[length];
                }
                case INTMAP -> {
                    return new Integer[length];
                }
                case BINARY -> {
                    return new byte[length][];
                }
                case IMAGE -> {
                    return new EbsImage[length];
                }
                case VECTOR_IMAGE -> {
                    return new EbsVectorImage[length];
                }
                case CANVAS -> {
                    return new EbsCanvas[length];
                }
                case ANY -> {
                    return new Comparable[length];
                }
            }
        }
        return null;
    }

    public boolean isDataType(Object value) {
        if (value == null) {
            return true; // allow nulls for any type
        }
        if (type == JSON) {
            // Accept any valid JSON node: Map, List, String, Number, Boolean
            return (value instanceof java.util.Map)
                || (value instanceof java.util.List)
                || (value instanceof String)
                || (value instanceof Number)
                || (value instanceof Boolean);
        }
        if (type == ARRAY) {
            // Accept interpreter arrays, Java arrays, and Lists as "array-like"
            return (value instanceof ArrayDef)
                || (value instanceof java.util.List)
                || (value.getClass().isArray());
        }
        if (type == QUEUE) {
            // Accept QueueDef objects as queues
            return value instanceof com.eb.script.arrays.QueueDef;
        }
        if (type == RECORD) {
            // Accept Map objects as records
            return value instanceof java.util.Map;
        }
        if (type == MAP) {
            // Accept Map objects as maps (key-value stores)
            return value instanceof java.util.Map;
        }
        if (type == IMAGE) {
            // Accept EbsImage objects as images
            return value instanceof EbsImage;
        }
        if (type == VECTOR_IMAGE) {
            // Accept EbsVectorImage objects as vector images
            return value instanceof EbsVectorImage;
        }
        if (type == CANVAS) {
            // Accept EbsCanvas objects as canvases
            return value instanceof EbsCanvas;
        }
        if (type == BITMAP) {
            // Accept Byte objects as bitmaps
            return value instanceof Byte || value instanceof Number;
        }
        if (type == INTMAP) {
            // Accept Integer objects as intmaps
            return value instanceof Integer || value instanceof Number;
        }
        if (type == BINARY) {
            // Accept byte arrays as binary
            return value instanceof byte[];
        }
        // For other types, allow subclasses (e.g., HashMap instanceof Map)
        return dataClass.isInstance(value);
    }

    public final Object convertValue(Object value) {
        if (value == null) {
            // Return default values for primitive types when null
            switch (type) {
                case BYTE -> { return (byte) 0; }
                case INTEGER -> { return 0; }
                case LONG -> { return 0L; }
                case FLOAT -> { return 0.0f; }
                case DOUBLE -> { return 0.0; }
                case BOOL -> { return false; }
                case STRING -> { return ""; }
                default -> { return null; }
            }
        }
        switch (type) {
            case STRING -> {
                switch (value) {
                    case Long v -> value = String.valueOf(v);
                    case Float v -> value = String.valueOf(v);
                    case Double v -> value = String.valueOf(v);
                    case Boolean v -> value = booleanToString(v);
                    default -> {
                        value = Util.stringify(value);
                    }
                }
            }
            case BYTE -> {
                value = convertToByte(value);
            }
            case INTEGER -> {
                switch (value) {
                    case Byte v -> value = convertFromByte(v);
                    case Long v -> value = (int) (long) v;
                    case Float v -> value = (int) (float) v;
                    case Double v -> value = (int) (double) v;
                    case String v -> value = Integer.valueOf(v);
                    default -> {
                    }
                }
            }
            case LONG -> {
                switch (value) {
                    case Byte v -> value = (long) convertFromByte(v);
                    case Integer v -> value = (Long) (long) v;
                    case Float v -> value = (Long) (long) (float) v;
                    case Double v -> value = (Long) (long) (double) v;
                    case String v -> value = Long.valueOf(v);
                    default -> {
                    }
                }
            }
            case FLOAT -> {
                switch (value) {
                    case Byte v -> value = (float) convertFromByte(v);
                    case Double v -> value = (Float) (float) (double) v;
                    case Integer v -> value = (Float) (float) v;
                    case Long v -> value = (Float) (float) (long) v;
                    case String v -> value = Float.valueOf(v);
                    default -> {
                    }
                }
            }
            case DOUBLE -> {
                switch (value) {
                    case Byte v -> value = (double) convertFromByte(v);
                    case Float v -> value = (Double) (double) (float) v;
                    case Integer v -> value = (Double) (double) v;
                    case Long v -> value = (Double) (double) v;
                    case String v -> value = Double.valueOf(v);
                    default -> {
                    }
                }
            }
            case BOOL -> {
                switch (value) {
                    case String v -> value = stringToBoolean(v);
                    default -> {
                    }
                }
            }
            case JSON -> {
                // If we get a String, parse it as JSON into Map/List/Number/Boolean/String/null
                if (value instanceof String s) {
                    value = Json.parse("{" + s + "}"); // see tiny parser below
                }
                // If it's already Map/List/Number/Boolean/String, accept as-is
            }
            case ARRAY -> {
                // Accept ArrayDef/List/arrays as-is.
                // If given a JSON array string (e.g., "[1,2,3]"), try to parse it.
                if (value instanceof String s) {
                    String ts = s.trim();
                    if (ts.startsWith("[") && ts.endsWith("]")) {
                        try {
                            value = Json.parse(ts);
                        } catch (RuntimeException ignore) {
                            // keep original
                        }
                    }
                }
            }
            case RECORD, MAP -> {
                // Accept Map objects as records/maps
                // If given a JSON object string, parse it
                if (value instanceof String s) {
                    String ts = s.trim();
                    if (ts.startsWith("{") && ts.endsWith("}")) {
                        try {
                            value = Json.parse(ts);
                        } catch (RuntimeException ignore) {
                            // keep original
                        }
                    }
                }
            }
            case BITMAP -> {
                // Convert to byte for bitmap type
                value = convertToByte(value);
            }
            case INTMAP -> {
                // Convert to int for intmap type
                value = convertToInt(value);
            }
            case BINARY -> {
                // Binary accepts byte[] as-is, no conversion needed
                if (!(value instanceof byte[])) {
                    throw new RuntimeException("Cannot convert " + value.getClass().getSimpleName() + " to binary (byte[])");
                }
            }
            default -> {
            }
        }
        return value;
    }

    private String booleanToString(Boolean b) {
        if (b) {
            return "Y";
        } else if (!b) {
            return "N";
        }
        return null;
    }

    private Boolean stringToBoolean(String s) {
        if (s.equalsIgnoreCase("true")
            || s.equalsIgnoreCase("Y")) {
            return true;
        } else if (s.equalsIgnoreCase("false")
            || s.equalsIgnoreCase("N")) {
            return false;
        }
        return null;
    }

    private static byte convertToByte(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Byte b) {
            return b;
        }
        if (v instanceof Number n) {
            int i = n.intValue();
            return (byte) (i & 0xFF);
        }
        if (v instanceof String s) {
            int i = Integer.parseInt(s.trim());
            if (i < -128 || i > 127) {
                throw new RuntimeException("Byte overflow: " + i + " not in [-128..127]");
            }
            return (byte) i;
        }
        throw new RuntimeException("Cannot convert " + v.getClass().getSimpleName() + " to byte");
    }

    private static int convertToInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Integer i) {
            return i;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        if (v instanceof String s) {
            return Integer.parseInt(s.trim());
        }
        throw new RuntimeException("Cannot convert " + v.getClass().getSimpleName() + " to int");
    }

    private static int convertFromByte(Byte v) {
        if (v == null) {
            return 0;
        } else {
            return (v & 0xFF);
        }
    }
}