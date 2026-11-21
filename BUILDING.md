# Building

This project targets Java 21, Minecraft 1.21.1, NeoForge 21.1.252, and Stargate
Journey 0.6.55-alpha for development builds. The addon requires Stargate
Journey 0.6.55-alpha or newer at runtime.

## Requirements

- A Java 21 JDK
- Internet access for Gradle and Forge dependencies
- A Stargate Journey 1.21.1 JAR for 0.6.55-alpha

## Stargate Journey dependency

The build intentionally does not redistribute Stargate Journey. Provide its
JAR using either of these methods:

1. Set the `SGJOURNEY_JAR` environment variable to the absolute path of the JAR.
2. Create a local `libs/` directory and place the JAR there as
   `Stargate.Journey-1.21.1-0.6.55-alpha.jar`.

The local `libs/` directory is ignored by Git.

## Build command

Windows:

```powershell
.\gradlew.bat clean build
```

Linux or macOS:

```bash
./gradlew clean build
```

The reobfuscated mod JAR is written to `build/libs/`.

To create both the JAR and matching source archive:

```bash
./gradlew release
```
