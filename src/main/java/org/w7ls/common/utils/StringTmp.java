package org.w7ls.common.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class StringTmp {
    public static String str(@NotNull Object...objects) {
        return _str(objects);
    }

    public static String strSep(String sep, @NotNull Object...objects) {
        StringBuilder s13 = new StringBuilder();
        for (Object o : objects) {
            s13.append(o);
            s13.append(sep);
        }

        return s13.toString().replaceAll("/$", "");
    }

    public static String csvRow(Object...objects) {
        return Arrays.toString(objects)
                .replaceAll("^\\[", "")
                .replaceAll("]$", "\n");
    }

    private static String _str(@NotNull Object...objects) {
        StringBuilder s13 = new StringBuilder();
        for (Object o : objects) {
            s13.append(o);
        }

        return s13.toString();
    }
}
