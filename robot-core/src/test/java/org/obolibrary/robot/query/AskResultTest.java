package org.obolibrary.robot.query;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Created by edouglass on 9/15/17.
 *
 * Tests output For Ask Result
 */
public class AskResultTest {

    private AskResult result;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() {
        result = new AskResult(true);
    }

    @Test
    public void testWrite() throws IOException {
        File testOut = temp.newFile("testOut");
        result.writeResults(testOut, Optional.empty());

        String written = Files.toString(testOut, Charset.defaultCharset());
        assertEquals("true", written);
    }
}
