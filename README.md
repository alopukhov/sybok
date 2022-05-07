[![GitHub](https://img.shields.io/github/license/alopukhov/sybok)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.alopukhov.sybok/sybok-engine)](https://mvnrepository.com/artifact/io.github.alopukhov.sybok/sybok-engine)

# Sybok

Sybok is a test engine for well-known JUnit platform.  
The main goal is running groovy test scripts on junit platform without compilation using
[JUnit Console](https://junit.org/junit5/docs/snapshot/user-guide/#running-tests-console-launcher). 

# Getting started

Just add Sybok to your runtime classpath and you are ready to go.
Library is available at maven central.

Grab it With gradle:
```groovy
runtimeOnly "io.github.alopukhov.sybok:sybok-engine:${sybokVersion}"
```

Or with maven:
```xml
<dependency>
  <groupId>io.github.alopukhov.sybok</groupId>
  <artifactId>sybok-engine</artifactId>
  <version>${sybokVersion}</version>
  <scope>runtime</scope>
</dependency>
``` 

# Configuration options
* `sybok.script-roots` comma separated list of paths to script roots folders.
Those folders must exist. Specifying some directory and its ancestor is prohibited.
For example following config considered illegal: `./specs/,./specs/a/b` 
* `sybok.delegate-engine-ids` comma separated list of engines ids to use.
Empty list (default) will use all engines found on classpath except sybok itself.

When using JUnit Console you may specify them via `--config` option, e.g.
`--config sybok.script-roots=./specs/`

# Scripts directory structure
Sybok does not enforce any particular folder structure.
However, there are some basic restriction:
* Package declarations must match directory structure 
* Only single non-nested class may be declared in a file 
* Declared classname must match filename
* Declaring same class under multiple roots is prohibited 

# Selecting tests
Following test selectors are supported:
* `-d, --select-directory` traverses specified directories to find tests.
Directories not under script roots will be silently ignored.
* `-p, --select-package` traverse files in all script roots belonging to specified package.
Empty (root) package will be ignored - use directory selectors instead.
* `-f, --select-file` select specific file for test discovery.
Files not under script roots will be silently ignored.
* `-c, --select-class` looks for specified class inside test roots.

First two options also takes into account package and classname include/exclude filters.

File and class selectors ignores those filters.

---
:grey_exclamation: Default JUnit Console include classname pattern is `^(Test.*|.+[.$]Test.*|.*Tests?)$`.
Override it or name your test classes according to it.

---

# Example
See [sybok-example](https://github.com/alopukhov/sybok-example) repo for example project.