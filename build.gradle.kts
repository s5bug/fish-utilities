plugins {
    id("java")
    id("application")
}

group = "tf.bug"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    mavenCentral()
}

dependencies {
    implementation("com.discord4j:discord4j-core:3.3.0-SNAPSHOT")

    implementation(libs.lucene.core)
    implementation(libs.lucene.analysis.common)
    implementation(libs.lucene.queryparser)

    implementation("app.xivgear:xivapi-java:0.1.12")

    implementation("org.jsoup:jsoup:1.21.2")

    compileOnly("org.jetbrains:annotations:26.0.2")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
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