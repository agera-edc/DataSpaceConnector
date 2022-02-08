/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */
package org.eclipse.dataspaceconnector.junit.launcher;

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A JUnit extension for running an embedded EDC runtime as part of a test fixture.
 * The runtime obtains a classpath determined by the Gradle build.
 * <p>
 * This extension attaches a EDC runtime to the {@link BeforeTestExecutionCallback} and {@link AfterTestExecutionCallback} lifecycle hooks. Parameter injection of runtime services is supported.
 */
public class EdcRuntimeExtension extends EdcExtension {
    final String moduleName;
    final Map<String, String> properties;
    private Thread runtimeThread;
    private static final String GRADLE_WRAPPER = "gradlew";

    public EdcRuntimeExtension(String moduleName, Map<String, String> properties) {
        this.moduleName = moduleName;
        this.properties = Map.copyOf(properties);
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {

        // Find the project root directory, moving up the directory tree
        var root = findRoot(new File(".").getCanonicalFile());
        if (root == null) {
            throw new EdcException("Could not find " + GRADLE_WRAPPER + " in parent directories.");
        }

        // Run a Gradle custom task to determine the runtime classpath of the module to run
        String command = new File(root, GRADLE_WRAPPER).getCanonicalPath() + " -q " + moduleName + ":printClasspath";
        Process exec = Runtime.getRuntime().exec(command);
        InputStream inputStream = exec.getInputStream();
        var classpathString = new String(inputStream.readAllBytes());
        if (exec.waitFor() != 0) {
            throw new EdcException("Failed to run gradle command: " + command);
        }

        // Replace subproject JAR entries with subproject build directories in classpath.
        // This ensures modified classes are picked up without needing to rebuild dependent JARs.
        var classPathEntries = Arrays.stream(classpathString.split(":|\\s"))
                .filter(s -> !s.isBlank())
                .flatMap(p -> resolveClassPathEntry(root, p))
                .toArray(URL[]::new);

        // Create a ClassLoader that only has the target module class path, and is not
        // parented with the current ClassLoader.
        var classLoader = URLClassLoader.newInstance(classPathEntries,
                ClassLoader.getSystemClassLoader());

        // Temporarily inject system properties.
        var savedProperties = (Properties) System.getProperties().clone();
        properties.forEach(System::setProperty);

        var latch = new CountDownLatch(1);

        runtimeThread = new Thread(() ->
        {
            try {

                // Make the ClassLoader available to the ServiceLoader.
                // This ensures the target module's extensions are discovered and loaded at runtime boot.
                Thread.currentThread().setContextClassLoader(classLoader);

                // Boot EDC runtime.
                super.beforeTestExecution(extensionContext);

                latch.countDown();
            } catch (Exception e) {
                throw new EdcException(e);
            }
        });

        // Start thread and wait for EDC to start up.
        runtimeThread.start();

        if (!latch.await(10, SECONDS)) {
            throw new EdcException("Failed to start EDC runtime");
        }

        // Restore system properties.
        System.setProperties(savedProperties);
    }


    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        runtimeThread.join();
        super.afterTestExecution(context);
    }

    /**
     * Utility method to locate the Gradle project root.
     *
     * @param path directory in which to start ascending search for the Gradle root.
     * @return The Gradle project root directly, or <code>null</code> if not found.
     */
    private File findRoot(File path) {
        File gradlew = new File(path, GRADLE_WRAPPER);
        if (gradlew.exists()) {
            return path;
        }
        var parent = path.getParentFile();
        if (parent != null) {
            return findRoot(parent);
        }
        return null;
    }

    /**
     * Replace Gradle subproject JAR entries with subproject build directories in classpath.
     * This ensures modified classes are picked up without needing to rebuild dependent JARs.
     *
     * @param root           project root directory.
     * @param classPathEntry class path entry to resolve.
     * @return resolved class path entries for the input argument.
     */
    private static Stream<URL> resolveClassPathEntry(File root, String classPathEntry) {
        try {
            File f = new File(classPathEntry).getCanonicalFile();

            // If class path entry is not a JAR under the root (i.e. a sub-project), do not transform it
            boolean isUnderRoot = f.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator);
            if (!classPathEntry.toLowerCase(Locale.ROOT).endsWith(".jar") || !isUnderRoot) {
                return Stream.of(new File(classPathEntry).toURI().toURL());
            }

            // Replace JAR entry with the resolved classes and resources folder
            var buildDir = f.getParentFile().getParentFile();
            return Stream.of(
                    new File(buildDir, "/classes/java/main").toURI().toURL(),
                    new File(buildDir, "/resources/main/").toURI().toURL()
            );
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

}
