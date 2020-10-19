package com.github.ryarnyah.dblock;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class BasicMojoIntegrationTest extends AbstractMojoTestCase {

    private static final String testDir = "/src/test/resources/unit/";
    private BackwardsCompatibilityCheckMojo myMojo;

    protected void setUp()
            throws Exception {
        // required
        super.setUp();
        setupMojo();
    }

    protected void tearDown()
            throws Exception {
        // required
        super.tearDown();
        File lockFile = getTestFile(testDir + ".dblock.lock");
        lockFile.delete();
    }

    /**
     * Tests that dblock is properly initialized.
     * @throws Exception if any.
     */
    @Test
    public void testDblockInit()
            throws Exception {
        myMojo.execute();
    }

    /**
     * Tests that dblock is properly initialized.
     * @throws Exception if any.
     */
    @Test
    public void testUpdateSchema()
            throws Exception {
        // Init
        myMojo.execute();
        // Init schema
        try(Connection connection = getConnection("test")) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new liquibase.Liquibase("liquibase/changelog.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }
        // update lock file
        myMojo.execute();
        try(Connection connection = getConnection("test")) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new liquibase.Liquibase("liquibase/drop-column.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }
        // update lock file
        try {
            myMojo.execute();
        } catch (MojoFailureException e) {
            // Pass
        }
    }

    private Connection getConnection(String database) throws SQLException {
        String url = "jdbc:postgresql://localhost/" + database;
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "postgres");
        return DriverManager.getConnection(url, props);
    }

    /**
     * Setup backwards compatibility check mojo.
     */
    private void setupMojo()
            throws Exception {
        File pom = getTestFile(testDir + "project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());
        myMojo = (BackwardsCompatibilityCheckMojo) lookupMojo("backwards-compatibility-check", pom);
        assertNotNull(myMojo);

        Model m = new Model();
        String classifier = System.getProperty("os.name").toLowerCase();
        if ((classifier.contains("mac"))) {
            classifier = "osx-x86_64";
        } else if (classifier.contains("nux")) {
            classifier = "linux-x86_64";
        } else if (classifier.contains("windows")) {
            classifier = "windows-x86_64";
        }

        m.addProperty("os.detected.classifier", classifier);
        Build b = new Build();
        b.setDirectory(System.getProperty("user.dir") + testDir);
        m.setBuild(b);

        myMojo.databaseLockFile = System.getProperty("user.dir") + testDir + ".dblock.lock";

        myMojo.project = new MavenProject(m);
    }
}
