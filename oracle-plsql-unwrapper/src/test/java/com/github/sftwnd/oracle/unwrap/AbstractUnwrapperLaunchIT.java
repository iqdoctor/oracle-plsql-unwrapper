// SPDX-License-Identifier: BSD-3-Clause

package com.github.sftwnd.oracle.unwrap;

import com.github.sftwnd.oracle.unwrap.spi.Samples;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The built unwrapper in a JVM of its own: each way to start it finds the formats through their services. The
 * unwrapper of a subclass differs in the formats it has.
 */
public abstract class AbstractUnwrapperLaunchIT {

    private static final String MODULE = "com.github.sftwnd.oracle.unwrap";
    private static final String MAIN = MODULE + ".cli.UnwrapperCli";
    private static final Path DISTRIBUTION = Paths.get(System.getProperty("distribution.directory"));
    private static final Path MODULE_JARS = Paths.get(System.getProperty("modules.directory"));
    private static final String JAVA = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
    private static final boolean MODULES = !System.getProperty("java.specification.version").startsWith("1.");
    private static final boolean WINDOWS = System.getProperty("os.name").startsWith("Windows");

    private static String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            for (int count = in.read(buffer); count >= 0; count = in.read(buffer)) {
                bytes.write(buffer, 0, count);
            }
        }
        String output = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output;
    }

    /** Name of a sample that this distribution restores. */
    protected abstract String sample();

    /** Name of the format of the distribution, for {@link #formatIsAModuleThatProvidesTheService()}. */
    protected abstract String format();

    /** Checks that the help tells the formats of this distribution. */
    protected abstract void assertHelp(String help);

    private void assertRestores(Path directory, String... command) throws IOException, InterruptedException {
        Path input = directory.resolve("in.sql");
        Path output = directory.resolve("out.sql");
        Files.write(input, Samples.read(sample() + ".wrapped").getBytes(StandardCharsets.ISO_8859_1));
        List<String> arguments = new ArrayList<>(Arrays.asList(command));
        arguments.add(input.toString());
        arguments.add(output.toString());

        run(arguments.toArray(new String[0]));

        assertArrayEquals(Samples.expected(sample() + ".unwrapped", sample() + ".wrapped")
                .getBytes(StandardCharsets.ISO_8859_1), Files.readAllBytes(output));
    }

    @Test
    void classPathFindsTheProviderInMetaInfServices(@TempDir Path directory) throws Exception {
        String classPath = MODULE_JARS + File.separator + "*";

        assertHelp(run(JAVA, "-cp", classPath, MAIN, "--help"));
        assertRestores(directory, JAVA, "-cp", classPath, MAIN);
    }

    @Test
    void executableJarNamesItsDependencies() throws Exception {
        assertHelp(run(JAVA, "-jar", MODULE_JARS.resolve(System.getProperty("unwrapper.jar")).toString(), "--help"));
    }

    @Test
    void executableJarOfTheDistributionHoldsEverything(@TempDir Path directory) throws Exception {
        String jar = DISTRIBUTION.resolve(System.getProperty("unwrapper.jar")).toString();

        assertHelp(run(JAVA, "-jar", jar, "--help"));
        assertRestores(directory, JAVA, "-jar", jar);
    }

    /** The executable of the profile native, if it is built: the same as the executable jar, without a JVM. */
    @Test
    void nativeExecutableHoldsEverything(@TempDir Path directory) throws Exception {
        String executable = System.getProperty("native.executable", "");
        assumeFalse(executable.isEmpty(), "No native executable: the profile native is off");
        String command = WINDOWS ? executable + ".exe" : executable;

        assertHelp(run(command, "--help"));
        assertRestores(directory, command);
    }

    /** The manifest of a part would make the jar look like that part: a bundle, a module, a jar with neighbours. */
    @Test
    void executableJarIsNoBundleAndNoModule() throws Exception {
        try (JarFile jar = new JarFile(System.getProperty("executable.jar"))) {
            Attributes attributes = jar.getManifest().getMainAttributes();

            assertEquals(MAIN, attributes.getValue(Attributes.Name.MAIN_CLASS));
            for (String header : Arrays.asList("Class-Path", "Multi-Release", "Automatic-Module-Name",
                    "Bundle-SymbolicName", "Export-Package", "Import-Package")) {
                assertNull(attributes.getValue(header), header);
            }
            assertNull(jar.getEntry("META-INF/versions/9/module-info.class"));
        }
    }

    @Test
    void modulePathFindsTheProviderInItsDescriptor(@TempDir Path directory) throws Exception {
        assumeTrue(MODULES, "JDK 8 has no modules");
        String module = MODULE + "/" + MAIN;

        assertHelp(run(JAVA, "--module-path", MODULE_JARS.toString(), "--module", module, "--help"));
        assertRestores(directory, JAVA, "--module-path", MODULE_JARS.toString(), "--module", module);
    }

    @Test
    void formatIsAModuleThatProvidesTheService() throws Exception {
        assumeTrue(MODULES, "JDK 8 has no modules");

        String name = format().toLowerCase(Locale.ROOT);
        String description = run(JAVA, "--module-path", MODULE_JARS.toString(), "--describe-module", MODULE + "." + name);

        assertTrue(description.contains("provides " + MODULE + ".spi.FormatUnwrapper with " + MODULE + "." + name
                + "." + format() + "FormatUnwrapper"), description);
    }

    @Test
    void launcherStartsTheUnwrapper(@TempDir Path directory) throws Exception {
        assumeFalse(WINDOWS, "The launcher of Windows is a command file");
        String launcher = DISTRIBUTION.resolve("bin").resolve("unwrapper").toString();

        assertHelp(run(launcher, "--help"));
        assertRestores(directory, launcher);
    }
}
