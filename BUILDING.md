# Building

This project targets Java 17, Minecraft 1.20.1, Forge 47.4.0, and Stargate
Journey 0.6.50 for development builds. The addon requires Stargate Journey
0.6.49 or newer at runtime; 0.6.50 is the current recommended version.

## Requirements

- A Java 17 JDK
- Internet access for Gradle and Forge dependencies
- A Stargate Journey 1.20.1 development or universal JAR for 0.6.50

## Stargate Journey dependency

The build intentionally does not redistribute Stargate Journey. Provide its
JAR using either of these methods:

1. Set the `SGJOURNEY_JAR` environment variable to the absolute path of the JAR.
2. Create a local `libs/` directory and place the JAR there as
   `Stargate.Journey-1.20.1-0.6.50.jar`.

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
