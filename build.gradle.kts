plugins {
    `java-library`
    id("io.freefair.lombok") version "8.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
    `maven-publish`
}


java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 11 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

val pluginName = "ThePitPremium"
val version = "3.93"
repositories {
    maven("https://maven.aliyun.com/repository/public/")
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")

    maven("https://repo.crazycrew.us/releases")
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.inventivetalent.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.panda-lang.org/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://redempt.dev")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    mavenLocal()
}

dependencies {
    // Pin the Paper 1.20.6-compatible API; do not accidentally compile against another MM jar.
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"),
        "exclude" to listOf("MythicMobs*.jar"))))
    compileOnly(files("libs/MythicMobs-5.8.2.jar"))
//    compileOnly("io.papermc.paper:paper-api:1.20.6-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
    compileOnly("org.mongodb:mongo-java-driver:3.12.11")
    compileOnly("org.mongojack:mongojack:4.8.1")
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.0")
    compileOnly("redis.clients:jedis:2.9.0")
    compileOnly("org.java-websocket:Java-WebSocket:1.6.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.13")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.0")
    compileOnly("com.github.Redempt:RedLib:6.6.1")
    compileOnly("com.andan:AVipSystem:1.0-SNAPSHOT")
    api(libs.slf4j)
    api(libs.book)
    api(libs.slf4j)
    api(libs.reflectionhelper)
    compileOnly(libs.luckperms)
    compileOnly(libs.narshorn)
    compileOnly(libs.httpclient)
    compileOnly(libs.httpcore)
    compileOnly(libs.decentholograms)
    compileOnly(libs.packetevents)
    compileOnly(libs.papi)
    implementation(libs.anvilgui)
    implementation("de.rapha149.signgui:signgui:2.5.2")
    implementation("de.exlll:configlib-yaml:4.8.1")
//    implementation("dev.rollczi:litecommands-bukkit:3.9.7")
    implementation(platform("com.intellectualsites.bom:bom-newest:1.52")) // Ref: https://github.com/IntellectualSites/bom
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    implementation("org.openjdk.nashorn:nashorn-core:15.6")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.snakeyaml:snakeyaml-engine:2.10")
    testImplementation(libs.packetevents) // Packet data/copy tests; still NOT bundled into the plugin.


}

tasks.shadowJar {
    archiveFileName.set("$pluginName-$version.jar")
    mergeServiceFiles()
    exclude("META-INF/**")

    relocate("co.aikar.commands", "cn.charlotte.pit.utils.acf.commands")
    relocate("co.aikar.locales", "cn.charlotte.pit.utils.acf.locales")
    relocate("de.rapha149.signgui", "com.heypixel.libs.signgui")
    relocate("net.wesjd.anvilgui", "com.heypixel.libs.anvilgui")
}
tasks.assemble {
    dependsOn(tasks.reobfJar)
}
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
