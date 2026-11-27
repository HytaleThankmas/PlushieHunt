plugins {
    id("gg.ginco.hygradle")
}

version = "1.0.0"

hytale {
    group = "com.melodic"
    name = "PlushieHunt"
    description = "Tracks hidden plushies each player has found through trigger volumes"
    mainClass = "com.melodic.plushiehunt.PlushieHuntPlugin"
    author("MelodicAlbuild")

    bundleDependencies = true
    includesAssetPack = true
}

tasks.named<Jar>("jar") {
    archiveBaseName = "PlushieHunt"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    compileOnly(project(":plugins:thankmas-vault"))
    compileOnly(files("../../libs/Wardrobe-0.3.5+0.7.2.jar"))
}
