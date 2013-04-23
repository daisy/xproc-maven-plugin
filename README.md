[xproc-maven-plugin][]
======================
Maven plugin for running [XProc][] pipelines and [XProcSpec][] tests.

Goals
-----

### xproc:xproc
Run an XProc pipeline.

- `pipeline` Path to the pipeline.

### xproc:xprocspec
Run a series of XProcSpec tests.

- `xprocspecDirectory` Directory containing the XProcSpec tests. Default `${basedir}/src/test/xprocspec`
- `reportsDirectory` Directory that will contain the generated reports. Default `${project.build.dir}/xprocspec-reports`

Usage
-----
```xml
<build>
  <plugins>
    ...
    <plugin>
      <groupId>org.daisy.pipeline.maven</groupId>
      <artifactId>xproc-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <executions>
        <execution>
          <phase>test</phase>
          <goals>
            <goal>xprocspec</goal>
          </goals>
        </execution>
      </executions>
      <dependencies>
        <dependency>
          <groupId>org.daisy.libs</groupId>
          <artifactId>com.xmlcalabash</artifactId>
          <version>1.0.10-SNAPSHOT</version>
        </dependency>
      </dependencies>
    </plugin>
    ...
  <plugins>
</build>
```

For rendering the XProcSpec reports in HTML:

```xml
    <plugin>
      <groupId>org.daisy.pipeline.maven</groupId>
      <artifactId>xproc-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <configuration>
        <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
      </configuration>
      ...
<reporting>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-report-plugin</artifactId>
      <version>2.14.1</version>
    </plugin>
  </plugins>
</reporting>
```

License
-------
Copyright © 2013 [Bert Frees][bert]


[xproc-maven-plugin]: http://github.com/bertfrees/xproc-maven-plugin
[xproc]: http://xproc.org/
[xprocspec]: https://github.com/josteinaj/xprocspec
[bert]: http://github.com/bertfrees
