# Building

This project targets Java 17, Minecraft 1.20.1, Forge 47.4.0, and Stargate
Journey 0.6.55-alpha for development and gameplay validation.

## Requirements

- A Java 17 JDK
- Internet access for Gradle and Forge dependencies
- The Stargate Journey 1.20.1 0.6.55-alpha JAR
- The Immersive Portals 3.0.7 Forge JAR (`IMMERSIVE_PORTALS_JAR` or
  `libs/immersive-portals-3.0.7-all.jar`)

## Stargate Journey dependency

The build intentionally does not redistribute Stargate Journey. Provide its
JAR using either of these methods:

1. Set the `SGJOURNEY_JAR` environment variable to the absolute path of the JAR.
2. Create a local `libs/` directory and place the JAR there as
   `Stargate.Journey-1.20.1-0.6.55-alpha.jar`.

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
