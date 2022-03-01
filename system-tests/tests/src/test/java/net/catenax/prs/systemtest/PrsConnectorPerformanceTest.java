package net.catenax.prs.systemtest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.gatling.javaapi.http.HttpDsl.http;
import static java.lang.String.format;

/**
 * This class is responsible for running a performance test on connectors integrated with PRS.
 */
@Tag("SystemTests")
public class PrsConnectorPerformanceTest extends SystemTestsBase {

    /*
     */

    @Test
    public void test() {
        runGatling(PerformanceTestsRunner.class);
    }

}