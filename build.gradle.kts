plugins {
    id("java")
}

group = "tf.bug"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.3.0-SNAPSHOT")

    implementation("org.apache.lucene:lucene-core:10.1.0")
    implementation("org.apache.lucene:lucene-analysis-common:10.1.0")
    implementation("org.apache.lucene:lucene-queryparser:10.1.0")

    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}