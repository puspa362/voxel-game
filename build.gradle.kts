plugins {
    application
}

val lwjglVersion = "3.4.1"
val targetJavaVersion = JavaVersion.current().majorVersion.toInt()
val appMainClass = "com.example.voxelgame.VoxelGame"

fun resolveLwjglNatives(): String {
    val operatingSystemName = System.getProperty("os.name").lowercase()
    val architecture = System.getProperty("os.arch").lowercase()
    val isWindows = operatingSystemName.contains("windows")
    val isLinux = operatingSystemName.contains("linux")
    val isMacOs = operatingSystemName.contains("mac") || operatingSystemName.contains("darwin")

    return when {
        isWindows -> if ("64" in architecture || architecture.startsWith("arm")) {
            "natives-windows"
        } else {
            "natives-windows-x86"
        }

        isLinux -> when {
            architecture.startsWith("aarch64") || architecture.startsWith("arm64") -> "natives-linux-arm64"
            architecture.startsWith("arm") -> "natives-linux-arm32"
            architecture.startsWith("ppc64le") -> "natives-linux-ppc64le"
            architecture.startsWith("riscv64") -> "natives-linux-riscv64"
            "64" in architecture -> "natives-linux"
            else -> "natives-linux-x86"
        }

        isMacOs -> if (architecture.startsWith("aarch64") || architecture.startsWith("arm64")) {
            "natives-macos-arm64"
        } else {
            "natives-macos"
        }

        else -> throw GradleException("Unsupported operating system: $operatingSystemName")
    }
}

val lwjglNatives = resolveLwjglNatives()

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

application {
    mainClass.set(appMainClass)
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.joml:joml:1.10.8")
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.named<JavaExec>("run") {
    workingDir = project.layout.projectDirectory.asFile
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true

    manifest {
        attributes(
            "Main-Class" to appMainClass,
            "Enable-Native-Access" to "ALL-UNNAMED",
            "Implementation-Title" to rootProject.name,
            "Implementation-Version" to "1.0.0"
        )
    }

    from({
        configurations.runtimeClasspath.get()
            .filter { it.exists() }
            .map { dependency ->
                if (dependency.isDirectory) dependency else zipTree(dependency)
            }
    })
}

tasks.register<Zip>("sourceZip") {
    group = "distribution"
    description = "Packages source, assets, documentation, and Gradle files for a release archive."
    archiveBaseName.set(rootProject.name)
    archiveVersion.set("1.0.0")
    archiveClassifier.set("source")

    from(project.layout.projectDirectory) {
        include("src/**")
        include("docs/**")
        include("scripts/**")
        include("texture_sources/**")
        include("gradle/**")
        include("build.gradle.kts")
        include("settings.gradle.kts")
        include("gradle.properties")
        include("gradlew.bat")
        include("README.md")
        include("release-notes/**")
        exclude("**/*.tmp")
        exclude("**/*.lock")
    }
}

tasks.assemble {
    dependsOn(tasks.named("sourceZip"))
}
