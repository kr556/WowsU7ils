package org.w7ls.common;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;

public final class W7lsProperties {
    private W7lsProperties() {}

    public final static String cuSrcDir;
    public final static String ptxDir;
    public final static String cuFileExtension;
    public final static String ptxFileExtension;

    static {
        try (Reader reader = new FileReader("setting.properties")) {
            Properties properties = new Properties();
            properties.load(reader);
            cuSrcDir = properties.getProperty("cu_src_dir");
            ptxDir = properties.getProperty("ptx_dir");
            cuFileExtension = properties.getProperty("cu_file_extension");
            ptxFileExtension = properties.getProperty("ptx_file_extension");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (cuSrcDir == null ||
            ptxDir == null)
            throw new RuntimeException("not found properties.");
    }
}
