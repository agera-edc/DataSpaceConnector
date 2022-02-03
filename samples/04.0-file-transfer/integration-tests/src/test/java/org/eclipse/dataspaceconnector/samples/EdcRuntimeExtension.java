package org.eclipse.dataspaceconnector.samples;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class EdcRuntimeExtension implements BeforeAllCallback, AfterAllCallback {
    final Map<String, String> properties;
    private Thread otherConnector;

    public EdcRuntimeExtension(Map<String, String> properties) {
        this.properties = Map.copyOf(properties);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        var rootProject = Paths.get(System.getProperty("user.dir")).getParent().toAbsolutePath();

        var saved = (Properties) System.getProperties().clone();
        properties.forEach((k, v) -> System.setProperty(k, v));
        var latch = new CountDownLatch(1);
        otherConnector = new Thread(() ->
        {
            try {
                var file = new File(rootProject + "/consumer/build/libs/consumer.jar");
                assertThat(file).canRead();
                var jar = new JarInputStream(new FileInputStream(file));
                var manifest = jar.getManifest();
                var mainClassName = manifest.getMainAttributes().getValue("Main-Class");

                var classLoader = URLClassLoader.newInstance(new URL[]{file.toURI().toURL()},
                        ClassLoader.getSystemClassLoader());
                Thread.currentThread().setContextClassLoader(classLoader);

                var mainClass = classLoader.loadClass(mainClassName);
                var mainMethod = mainClass.getMethod("main", String[].class);
                mainMethod.invoke(null, new Object[]{new String[0]});

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        otherConnector.start();
        latch.await(10, SECONDS);
        System.setProperties(saved);
        System.out.println("PROPS:" + System.getProperties());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        
    }
}
