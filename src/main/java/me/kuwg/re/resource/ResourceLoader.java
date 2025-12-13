package me.kuwg.re.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ResourceLoader {
    public static String loadResourceAsString(String resourcePath) {
        try (InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static Path getResourcePath(String resourcePath) {
        URL resourceUrl = ResourceLoader.class.getResource(resourcePath);
        if (resourceUrl == null) {
            return null;
        }
        try {
            return Path.of(resourceUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
