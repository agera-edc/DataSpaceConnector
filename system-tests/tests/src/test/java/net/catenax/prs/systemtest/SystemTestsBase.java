package net.catenax.prs.systemtest;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;
import io.gatling.javaapi.core.Simulation;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;

public class SystemTestsBase {

    protected RequestSpecification getRequestSpecification() {
        var specificationBuilder = new RequestSpecBuilder();
        return specificationBuilder.build();
    }

    protected void runGatling(Class<? extends Simulation> simulation) {
        var props = new GatlingPropertiesBuilder();
        props.simulationClass(simulation.getCanonicalName());
        props.resultsDirectory("target/gatling");
        Gatling.fromMap(props.build());
    }

}
