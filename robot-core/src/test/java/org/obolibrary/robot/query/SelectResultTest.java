package org.obolibrary.robot.query;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class  SelectResultTest {

    private SelectResult result;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setup() {
        InputStream resultsContents = this.getClass().getResourceAsStream("/sparql.out");
        ResultSet resultSet = ResultSetFactory.fromTSV(resultsContents);
        result = new SelectResult(resultSet);
    }

    @Test
    public void testWrite() throws IOException {
        File testOutput = temp.newFile("testout");
        result.writeResults(testOutput, Optional.of(ResultSetLang.SPARQLResultSetTSV));
        ArrayList<String> lines = Lists.newArrayList(IOUtils.lineIterator(new FileInputStream(testOutput), Charset.defaultCharset()));

        ArrayList<String> expectedLines = Lists.newArrayList(IOUtils.lineIterator(this.getClass().getResourceAsStream("/sparql.out"), Charset.defaultCharset()));

        assertEquals(expectedLines.size(), lines.size());
        for(int i=0; i<lines.size(); i++) {
            String l = lines.get(i);
            String expected = expectedLines.get(i);
            assertEquals(expected, l);
        }
    }

}
