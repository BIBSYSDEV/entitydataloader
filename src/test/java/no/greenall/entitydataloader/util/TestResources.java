package no.greenall.entitydataloader.util;


import org.junit.rules.ExternalResource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.util.Objects.isNull;

public class TestResources extends ExternalResource {

    private static TestResources currentTestResource = null;
    private ByteArrayOutputStream systemOutByteArray;
    private PrintStream systemOut;
    private ByteArrayOutputStream systemErrByteArray;
    private PrintStream systemErr;

    private TestResources() {
        // NO-OP
    }

    public static TestResources getDefaultTestResource() {
        if (isNull(currentTestResource)) {
            currentTestResource = new TestResources();
        }
        return currentTestResource;
    }

    protected void before() {
        systemOutByteArray = new ByteArrayOutputStream();
        systemErrByteArray = new ByteArrayOutputStream();
        systemOut = System.out;
        systemErr = System.err;
        System.setOut(new PrintStream(systemOutByteArray));
        System.setErr(new PrintStream(systemErrByteArray));
    }

    protected void after() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    void resetStandardOutString() {
        systemOutByteArray.reset();
    }

    public String getStandardOutString() {
        return systemOutByteArray.toString();
    }

    public String getStandardErrorString() {
        return systemErrByteArray.toString();
    }
}
