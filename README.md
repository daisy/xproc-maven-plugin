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
- `reportDirectory` Directory that will contain the generated reports. Default `${project.build.dir}/xprocspec-reports`

Usage
-----
```xml
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
```

License
-------
Copyright © 2013 [Bert Frees][bert]


[xproc-maven-plugin]: http://github.com/bertfrees/xproc-maven-plugin
[xproc]: http://xproc.org/
[xprocspec]: https://github.com/josteinaj/xprocspec
[bert]: http://github.com/bertfrees