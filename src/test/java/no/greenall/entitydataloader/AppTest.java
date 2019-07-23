package no.greenall.entitydataloader;

import no.greenall.entitydataloader.util.TestResources;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class AppTest {

    private static final String HELP_RESPONSE_TXT = "help_response.txt";
    private static final String UNRECOGNIZED_SERIALIZATION_TXT = "unrecognized_serialization.txt";
    private static final String MISSING_REQUIRED_OPTION_INPUT_FILE_TXT = "missing_required_option_input_file.txt";

    @Rule
    public TestResources testResources = TestResources.getDefaultTestResource();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testApp() throws IOException {
        String helpResponse = getTextFromFile(HELP_RESPONSE_TXT);

        String[] emptyArgs = {};
        App.main(emptyArgs);
        assertThat(testResources.getStandardOutString(), is(equalTo(helpResponse)));
    }

    @Test
    public void testUnrecognizedSerialization() throws IOException {
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage(getTextFromFile(UNRECOGNIZED_SERIALIZATION_TXT));

        String[] args = {"-u=http://example.org", "-k=123", "-i=file.txt", "-s=trix"};
        App.main(args);
    }

    @Test
    public void testNoInputFile() throws IOException {
        String helpResponse = getTextFromFile(MISSING_REQUIRED_OPTION_INPUT_FILE_TXT);

        String[] args = {"-s=trix"};
        App.main(args);
        assertThat(testResources.getStandardErrorString(), is(equalTo(helpResponse)));

    }

    private String getTextFromFile(String filepath) throws IOException {
        URL file = Optional.ofNullable(this.getClass().getClassLoader().getResource(filepath))
                .orElseThrow(FileNotFoundException::new);
        return IOUtils.toString(file, StandardCharsets.UTF_8);
    }
}