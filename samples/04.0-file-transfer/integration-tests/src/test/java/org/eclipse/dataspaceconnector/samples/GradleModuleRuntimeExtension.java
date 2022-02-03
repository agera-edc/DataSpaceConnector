package org.eclipse.dataspaceconnector.samples;

import org.eclipse.dataspaceconnector.core.system.runtime.BaseRuntime;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class GradleModuleRuntimeExtension implements BeforeAllCallback, AfterAllCallback {
    final String moduleName;
    final Map<String, String> properties;
    private Thread runtimeThread;

    public GradleModuleRuntimeExtension(String moduleName, Map<String, String> properties) {
        this.moduleName = moduleName;
        this.properties = Map.copyOf(properties);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {

        var root = new File("../../..").getCanonicalFile();
        Process exec = Runtime.getRuntime().exec(root + "/gradlew -q " + moduleName + ":printClasspath");
        InputStream inputStream = exec.getInputStream();
        var st = new String(inputStream.readAllBytes());
        assertThat(exec.waitFor()).isEqualTo(0);

        var classPathEntries = Arrays.stream(st.split(":|\\s"))
                .filter(s -> !s.isBlank())
                .flatMap(p -> resolveClassPathEntry(root, p))
                .toArray(URL[]::new);

        var classLoader = URLClassLoader.newInstance(classPathEntries,
                ClassLoader.getSystemClassLoader());

        var mainClassName = BaseRuntime.class.getCanonicalName();
        var mainClass = classLoader.loadClass(mainClassName);
        var mainMethod = mainClass.getMethod("main", String[].class);

        var savedProperties = (Properties) System.getProperties().clone();
        properties.forEach(System::setProperty);
        var latch = new CountDownLatch(1);
        runtimeThread = new Thread(() ->
        {
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                mainMethod.invoke(null, new Object[]{new String[0]});
                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        runtimeThread.start();

        assertThat(latch.await(10, SECONDS)).isTrue();

        System.setProperties(savedProperties);
    }

    private static Stream<URL> resolveClassPathEntry(File root, String classPathEntry) {
        try {
            File f = new File(classPathEntry).getCanonicalFile();

            // If class path entry is not a JAR unter the root (a sub-project), do not transform it
            boolean isUnderRoot = f.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator);
            if (!classPathEntry.toLowerCase(Locale.ROOT).endsWith(".jar") || !f.isFile() || !isUnderRoot) {
                return Stream.of(toURL(classPathEntry));
            }

            // Replace JAR entry with the resolved classes and resources folder
            var buildDir = f.getParentFile().getParent();
            return Stream.of(
                    toURL(buildDir + "/classes/java/main/"),
                    toURL(buildDir + "/resources/main/")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static URL toURL(String s) throws MalformedURLException {
        return new URL("file:" + s);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        runtimeThread.join();
    }
}
