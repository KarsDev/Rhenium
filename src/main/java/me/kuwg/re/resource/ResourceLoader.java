package me.kuwg.re.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ResourceLoader {
    public static String loadResourceAsString(String resourcePath) {
        try (InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                System.err.println("Resource not found: " + resourcePath);
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static Path extractResourceToTempFile(String resourcePath) {
        try (InputStream in =
                     ResourceLoader.class.getResourceAsStream(resourcePath)) {

            if (in == null) {
                return null;
            }

            String suffix =
                    resourcePath.substring(resourcePath.lastIndexOf('.'));

            Path temp = Files.createTempFile("rhenium-native-", suffix);

            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);

            temp.toFile().deleteOnExit();

            return temp;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
