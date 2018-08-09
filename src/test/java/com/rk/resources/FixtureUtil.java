package com.rk.resources;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class FixtureUtil {
    private static final String FIXTURES_DIR = "fixtures";

    /**
     * Returns stream of string resources from fixtures directory. Result is plain (non-recursive)
     * @param directory to search for fixtures
     * @return stream with string content of files
     */
    public static Stream<String> jsonStringsFromFixtureDirectory(String directory) {
        try {
            Path targetDir = Paths.get(Resources.getResource(String.format("%s/%s", FIXTURES_DIR, directory)).toURI());
            return Files.list(targetDir).map(path -> {
                try {
                    return new String(Files.readAllBytes(path));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
