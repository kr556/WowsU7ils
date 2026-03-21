package org.w7ls.main.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class JsonMappersTest {
    @Test
    void getJsonByResource() throws IOException {
        JsonNode meta = JsonMappers.getJsonByResource("meta.json");
        System.out.println(meta);
    }
}