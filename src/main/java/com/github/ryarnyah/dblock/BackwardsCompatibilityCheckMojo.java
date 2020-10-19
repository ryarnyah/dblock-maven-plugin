package com.github.ryarnyah.dblock;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Mojo(
        name = "backwards-compatibility-check",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true)
public class BackwardsCompatibilityCheckMojo
        extends AbstractMojo {

    public static final int RESULT_CODE_SUCCESS = 0;

    public enum DatabaseTypeParameter {
        POSTGRES("postgres"),
        MYSQL("mysql"),
        MSSQL("mssql");

        String code;

        DatabaseTypeParameter(String code) {
            this.code = code;
        }
    }

    @Parameter(property = "database-type", defaultValue = "POSTGRES", required = true)
    protected DatabaseTypeParameter databaseType;

    @Parameter(property = "database-schema-regex", defaultValue = ".*")
    protected String databaseSchemaRegex;

    @Parameter(property = "database-conn-info", required = true)
    protected String databaseConnInfo;

    @Parameter(property = "database-lock-file", defaultValue = "db.lock")
    protected String databaseLockFile;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    private List<String> buildOptions() {
        List<String> options = new ArrayList<>();
        options.add("-database-lock-file");
        options.add(this.databaseLockFile);
        switch (this.databaseType) {
            case POSTGRES:
                options.add("-pg-conn-info");
                options.add(this.databaseConnInfo);
                options.add("-pg-schema-regexp");
                options.add(this.databaseSchemaRegex);
                break;
            case MSSQL:
                options.add("-mssql-conn-info");
                options.add(this.databaseConnInfo);
                options.add("-mssql-schema-regexp");
                options.add(this.databaseSchemaRegex);
                break;
            case MYSQL:
                options.add("-mysql-conn-info");
                options.add(this.databaseConnInfo);
                options.add("-mysql-schema-regexp");
                options.add(this.databaseSchemaRegex);
                break;
        }
        return options;
    }

    /**
     * Execute the plugin.
     *
     * @throws MojoExecutionException thrown when execution of dblock fails.
     * @throws MojoFailureException   thrown when compatibility check fails.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String classifier = project.getProperties().getProperty("os.detected.classifier");
        if (classifier == null) {
            getLog().error("Add os-maven-plugin to your POM. https://github.com/trustin/os-maven-plugin");
            throw new MojoExecutionException("Unable to detect OS type.");
        }

        // Copy dblock executable locally if needed
        Path exeDirPath = Paths.get(project.getBuild().getDirectory(), "dblock-bin");
        try {
            Files.createDirectories(exeDirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to create the dblock binary directory", e);
        }

        String exeExtension = "";
        if (classifier.startsWith("windows")) {
            exeExtension = ".exe";
        }

        Path exePath = exeDirPath.resolve("dblock" + exeExtension);
        if (!Files.exists(exePath)) {
            String dblockResourcePath = classifier + "/dblock" + exeExtension;

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(dblockResourcePath)) {
                if (in == null) {
                    throw new MojoExecutionException(
                            "OS not supported. Unable to find a dblock binary for the classifier " + classifier);
                }

                Files.copy(in, exePath);

                PosixFileAttributeView attributes = Files.getFileAttributeView(exePath, PosixFileAttributeView.class);
                if (attributes != null) {
                    attributes.setPermissions(PosixFilePermissions.fromString("rwxrwxr-x"));
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to write the dblock binary", e);
            }
        }

        String pathEnv = "PATH=" + System.getenv("PATH");

        // Run dblock
        try {
            Process dblockStatusProcess = executedblock(exePath, pathEnv, buildOptions());
            if (dblockStatusProcess.waitFor() != RESULT_CODE_SUCCESS) {
                throw new MojoFailureException("Backwards compatibility check failed!");
            }
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("An error occurred while running dblock", e);
        }
    }

    private Process executedblock(Path exePath, String pathEnv, List<String> options) throws IOException {
        String[] cmdLineParameters = new String[]{
                exePath.toString()
        };
        String[] parameters = Stream.concat(Arrays.stream(cmdLineParameters), options.stream())
                .toArray(String[]::new);
        Process dblockProcess = Runtime.getRuntime()
                .exec(parameters, new String[]{pathEnv});
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(dblockProcess.getErrorStream()));
        String s;
        while ((s = stdInput.readLine()) != null) {
            getLog().info(s);
        }
        return dblockProcess;
    }
}
