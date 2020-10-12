[![Build Status](https://travis-ci.org/ryarnyah/dblock-maven-plugin.svg?branch=master)](https://travis-ci.org/ryarnyah/dblock-maven-plugin) 

DBLock Version: [0.4.0](https://github.com/ryarnyah/dblock/releases/tag/0.4.0)

# DBLock Backwards Compatibility Check Maven Plugin

The <code>dblock-maven-plugin</code> plugin is a Maven plugin to run a backwards compatibility 
check SQL databases. The plugin can be integrated after database schema update.
After first execution, then plugin generate a `db.lock` file and check againts it
for the nexts.

It is also possible to force any breaking changes and reset the current state
by deleting or altering the db.lock file. It will then reinitialize the next time the
plugin is run.

#### Maintaining Backwards Compatibility
In order to maintain backwards compatibility for your databases, a few
rules must be followed when making updates. Please refer here to avoid breaking changes:
https://github.com/ryarnyah/dblock/blob/master/README.md

## Usage

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.ryarnyah</groupId>
            <artifactId>dblock-maven-plugin</artifactId>
            <version>0.1.0</version>
            <configuration>
                <database-type>POSTGRES</database-type>
                <database-schema-regex>public</database-schema-regex>
                <database-conn-info>host=localhost port=5432 user=postgres dbname=test sslmode=disable password=postgres</database-conn-info>
                <database-lock-file>.dblock.lock</database-lock-file>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>backwards-compatibility-check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Configuration

* `<database-type>` (default to `POSTGRES`) - must be a value of `POSTGRES`, `MYSQL`, `MSSQL`.
* `<database-schema-regex>` (default to `.*`) - regex to match schema(s) to check.
* `<database-conn-info>` (empty) - Database connetion info (See https://github.com/ryarnyah/dblock/blob/master/README.md for format)
* `<database-lock-file>` (default to `db.lock`) - File path to lock file.
