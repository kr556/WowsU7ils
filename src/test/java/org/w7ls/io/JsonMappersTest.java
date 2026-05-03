package org.w7ls.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.w7ls.common.io.JsonMappers;

import java.io.IOException;

class JsonMappersTest {
    @Test
    void getJsonByResource() throws IOException {
        JsonNode meta = JsonMappers.getJsonByResource("meta.json");
        System.out.println(meta);
    }
}