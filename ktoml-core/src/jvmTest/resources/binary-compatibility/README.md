# Upstream-compiled configuration consumers

These fixtures exercise calls from consumers compiled against the published
`com.akuleshov7:ktoml-core-jvm:0.7.1` artifact. The binary compatibility tests load their bytecode
with the integrated library's `TomlInputConfig` class, without putting the upstream jar on the test classpath.
Keeping the bytecode prevents recompilation against the integrated source from silently selecting newer APIs.
The small Java 8 class files are stored as Base64 text resources alongside their sources.

The adjacent Java and Kotlin sources produce the `.class.base64` resources. Java calls the original
six-boolean constructor and `copy`; Kotlin calls the synthetic default constructor and `copy$default`.
Kotlin compilation used compiler and stdlib version 2.2.0 with JVM target 1.8. Java compilation used
JDK 21 with `--release 8`.

Upstream jar:

- [Maven Central artifact](https://repo.maven.apache.org/maven2/com/akuleshov7/ktoml-core-jvm/0.7.1/ktoml-core-jvm-0.7.1.jar)
- SHA-256: `c22cb661eefc75e30007a1b08d37c4e93a157f7f3a950a478251632c0af6050d`

To regenerate, run from this directory with `UPSTREAM_JAR` pointing to that jar,
`KOTLIN_STDLIB_JAR` to kotlin-stdlib 2.2.0, and `KOTLIN_COMPILER_CLASSPATH` to the Kotlin 2.2.0
compiler's runtime classpath (compiler-embeddable and its dependencies):

```sh
mkdir -p /tmp/ktoml-upstream-config-fixtures
javac --release 8 -classpath "$UPSTREAM_JAR" -d /tmp/ktoml-upstream-config-fixtures UpstreamJavaConfigConsumer.java
java -classpath "$KOTLIN_COMPILER_CLASSPATH" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -jvm-target 1.8 \
  -classpath "$UPSTREAM_JAR:$KOTLIN_STDLIB_JAR" \
  -d /tmp/ktoml-upstream-config-fixtures UpstreamKotlinConfigConsumer.kt
base64 /tmp/ktoml-upstream-config-fixtures/com/akuleshov7/ktoml/binarycompatibility/fixtures/UpstreamJavaConfigConsumer.class \
  > UpstreamJavaConfigConsumer.class.base64
base64 /tmp/ktoml-upstream-config-fixtures/com/akuleshov7/ktoml/binarycompatibility/fixtures/UpstreamKotlinConfigConsumerKt.class \
  > UpstreamKotlinConfigConsumerKt.class.base64
```

Regenerate only against upstream 0.7.1, preserving the original method descriptors.
