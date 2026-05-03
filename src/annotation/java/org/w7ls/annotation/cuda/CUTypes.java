package org.w7ls.annotation.cuda;

import org.jetbrains.annotations.Nullable;

public enum CUTypes {
    _byte("byte", Byte.class),
    _short("short", Short.class),
    _int("int", Integer.class),
    _long("long", Long.class),
    _float("float", Float.class),
    _double("double", Double.class),
    _long_long("long long", long[].class),
    // unsigned
    u_byte("unsigned byte", Byte.class),
    u_short("unsigned short", Short.class),
    u_int("unsigned int", Integer.class),
    u_long("unsigned long", Long.class),
    u_long_long("unsigned long long", long[].class),
    // pointer
    p_byte("byte*", int[].class),
    p_short("short*", int[].class),
    p_int("int*", int[].class),
    p_long("long*", long[].class),
    p_float("float*", float[].class),
    p_double("double*", double[].class),
    p_long_long("long long*", long[].class),
    p_u_byte("unsigned byte*", int[].class),
    p_u_short("unsigned short*", int[].class),
    p_u_int("unsigned int*", int[].class),
    p_u_long("unsigned long*", long[].class),
    p_u_long_long("unsigned long long*", long[].class),
    ;

    // c言語での名前
    public final String cname;
    public final Class<?> inJava;

    CUTypes(String n, Class<?> inJava) {
        cname = n;
        this.inJava = inJava;
    }

    public static @Nullable CUTypes toC(Class<?> inJava) {
        for (CUTypes c : CUTypes.values())
            if (c.inJava == inJava)
                return c;
        return null;
    }

    public static @Nullable CUTypes toC(String inJava) {
        for (CUTypes c : CUTypes.values())
            if (c.inJava.toString().equals(inJava))
                return c;
        return null;
    }
}
