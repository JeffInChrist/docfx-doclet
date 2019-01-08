package com.microsoft.doclet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.microsoft.util.FileUtilTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class DocletRunnerTest {

    private final String PARAMS_DIR = "src/test/resources/test-doclet-params.txt";
    private final String EXPECTED_GENERATED_FILES_DIR = "src/test/resources/expected-generated-files";
    private final String OUTPUT_DIR = "target/test-out";

    @Before
    public void cleanup() throws IOException {
        FileUtilTest.deleteDirectory(OUTPUT_DIR);
    }

    @Test
    public void testFilesGeneration() throws IOException {
        DocletRunner.main(new String[]{PARAMS_DIR});

        List<Path> expectedFilePaths = Files.list(Path.of(EXPECTED_GENERATED_FILES_DIR)).collect(Collectors.toList());
        List<Path> generatedFilePaths = Files.list(Path.of(OUTPUT_DIR)).collect(Collectors.toList());
        assertThat("Wrong files count", generatedFilePaths.size(), is(expectedFilePaths.size()));

        for (Path expectedFilePath : expectedFilePaths) {
            Path generatedFilePath = Path.of(OUTPUT_DIR, expectedFilePath.getFileName().toString());

            String generatedFileContent = Files.readString(generatedFilePath);
            String expectedFileContent = Files.readString(expectedFilePath);

            String[] generatedFileLines = generatedFileContent.split("\n");
            String[] expectedFileLines = expectedFileContent.split("\n");

            assertThat("Unexpected amount of lines in file " + generatedFilePath, generatedFileLines.length,
                is(expectedFileLines.length));

            for (int i = 0; i < generatedFileLines.length; i++) {
                assertThat("Wrong file content for file " + generatedFilePath,
                    generatedFileLines[i], is(expectedFileLines[i]));
            }
        }
    }
}
