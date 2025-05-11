plugins {
    id("java")
    id("application")
}

group = "tf.bug"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.3.0-SNAPSHOT")

    val luceneVersion = "10.2.0"
    implementation("org.apache.lucene:lucene-core:$luceneVersion")
    implementation("org.apache.lucene:lucene-analysis-common:$luceneVersion")
    implementation("org.apache.lucene:lucene-queryparser:$luceneVersion")

    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "tf.bug.fishutils.Main"
    }
}

application {
    applicationDefaultJvmArgs = listOf("-DentityExpansionLimit=2560000", "-Djdk.xml.totalEntitySizeLimit=512000000")
    mainClass = "tf.bug.fishutils.Main"
}

tasks.test {
    useJUnitPlatform()
}