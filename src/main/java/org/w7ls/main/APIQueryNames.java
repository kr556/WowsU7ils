package org.w7ls.main;

import com.fasterxml.jackson.databind.JsonNode;
import org.w7ls.main.io.JsonMappers;

import java.lang.reflect.Field;

public final class APIQueryNames {
    static {
        JsonNode meta = JsonMappers.getJsonByResource("parm/meta.json", null);
        if (meta == null)
            throw new NullPointerException("not found meta file.");

        try {
            Field applicationIdField = APIQueryNames.class.getField("applicationId");

            applicationIdField.setAccessible(true);

            applicationIdField.set(null, meta.get("application_id").asText());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private APIQueryNames() {}

    public static final String applicationId = null;

    public enum Region {
        ASIA,
        EU,
        NA,
        ;

        public static String keyName() {
            return "region";
        }

        public String toKey() {
            return name().toLowerCase().replace("-", "_");
        }
    }

    public enum Language {
        RU, EN,
        PL, DE,
        FR, ES,
        ZH_CN, ZH_TW,
        TR, CS,
        TH, JA,
        PT_BR,
        ES_MX,
        ;

        public static String keyName() {
            return "language";
        }

        public String toValue() {
            return name().toLowerCase().replace("-", "_");
        }
    }
}
