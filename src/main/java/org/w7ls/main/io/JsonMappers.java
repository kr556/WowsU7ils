package org.w7ls.main.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w7ls.utils.StringTmp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class JsonMappers {
    private JsonMappers() {}

    public static JsonNode getJsonByResource(String fullTree) throws IOException {
        try (InputStream s = JsonMappers.class.getResourceAsStream("/" + fullTree)) {
            BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(s)));
            return new ObjectMapper().readTree(br);
        }
    }

    @Nullable
    public static JsonNode getJsonByResource(String fullTree, @Nullable Function<IOException, JsonNode> catchAction) {
        try {
            return getJsonByResource(fullTree);
        } catch (IOException e) {
            return Objects.requireNonNullElse(catchAction, null).apply(e);
        }
    }

    public static @Nullable JsonNode getJsonRes(@NotNull URL url) throws IOException {
        return new ObjectMapper().readTree(url);
    }

    public static @Nullable JsonNode getJsonRes(@NotNull String url, @NotNull RequestQueries parms) throws IOException {
        return new ObjectMapper().readTree(getURL(url, parms));
    }

    public static @Nullable JsonNode getJsonRes(@NotNull String url, @NotNull RequestQueries parms, Function<IOException, JsonNode> catchAction) {
        try {
            return new ObjectMapper().readTree(getURL(url, parms));
        } catch (IOException e) {
            return catchAction.apply(e);
        }
    }

    public static URL getURL(String url, RequestQueries parms) throws IOException {
        List<String> ks = parms.getKeys();
        List<String> vs = parms.getValues();

        StringBuilder _url = new StringBuilder(url);
        for (int i = 0; i < ks.size(); i++) {
            if (i == 0) {
                _url.append(StringTmp.str("/?", ks.get(i), "=", vs.get(i)));
                continue;
            }
            _url.append(StringTmp.str("&", ks.get(i), "=", vs.get(i)));
        }
        return new URL(_url.toString());
    }
}
