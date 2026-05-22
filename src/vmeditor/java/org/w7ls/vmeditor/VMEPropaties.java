package org.w7ls.vmeditor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.Properties;

public class VMEPropaties {
    public static final Langage vmeLanguage;

    public enum Langage {
        ja,
        en, // 未実装
        cn  // 未実装
        ;

        private static Langage getInstance(String str) {
            return switch (str) {
                case "ja" -> ja;
                case "en" -> en;
                case "cn" -> cn;
                default -> throw new IllegalStateException("Unexpected value: " + str);
            };
        }
    }

    static {
        try (Reader reader = new FileReader("setting.properties")) {
            Properties properties = new Properties();
            properties.load(reader);
            vmeLanguage = Langage.getInstance(Objects.requireNonNullElse(
                    properties.getProperty("vme_language"),
                    "ja"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
