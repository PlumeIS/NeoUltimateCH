package cn.plumc.ultimatech.utils;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpHelper {

    private static final String userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/115.0.1901.183";

    private static final int TIMEOUT = 5000;

    public String sendGet(String uri, @Nullable Map<String, String> cookies) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(uri);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", userAgent);

            if (Objects.nonNull(cookies)) {
                String cookieHeader = cookies.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("; "));
                connection.setRequestProperty("Cookie", cookieHeader);
            }

            connection.connect();

            return readResponse(connection);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        )) {
            StringBuilder builder = new StringBuilder();
            reader.lines().forEach(line -> builder.append(line).append("\n"));
            return builder.toString();
        }
    }
}