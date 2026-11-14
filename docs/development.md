# Development

## Target and identity

Minecraft 1.21.1, NeoForge 21.1.265, Java 21, Gradle 9.2.1, and ModDevGradle 2.0.148.

Identity, artifact coordinates, author, description, and code license are configured in `gradle.properties`. The current coordinates are `com.kyivsec.oreui:oreui-lib:0.1.0`, with mod ID `oreui_lib`. The entrypoint ID must match the property; `verifyArtifact` checks this.

The code is MIT licensed, copyright KyivSec. See `../THIRD-PARTY-NOTICES.md` for separate design attribution. Credits are also visible in the mod metadata and packaged in the JAR.

## Commands

Use Java 21 on PATH, or set JAVA_HOME to a Java 21 JDK.

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

`build` produces the library and sources JARs in `build/libs`. `check` includes `verifyArtifact`, which checks expanded metadata, expected identity, required-dependency count, packaged notices, Java 21 bytecode, and entrypoint construction in an isolated classloader without Minecraft or NeoForge classes. This is a bootstrap isolation check, not a complete dedicated-server gameplay test.

JUnit is used only by the test source set and is not bundled with the mod or added to normal game runs. Run `gradlew.bat test` for the automated checks.

In a world, enter `/oreui test` to open the full-window interactable example. No cheats, operator privileges, server command, or server-side installation are required. See `test-screen.md` for controls and the initial public widget API.

The entrypoint is explicitly client-only. Keep future Minecraft client references behind client-side entrypoints and in client API packages. Do not add server-side services or require this library on the server. The metadata allows client/server mod-list differences.

Client runs use `run`; dedicated-server runs use `run-server`. Review and comply with Mojang's EULA before running Minecraft. This project's build scripts do not write EULA acceptance files.

## Resources and consumers

Resources live under `src/main/resources/assets/oreui_lib`. `OreNativeIcons` exposes 59 static glyphs; compatibility APIs redirect to current artwork or the Package fallback. Loading/progress use separate 15×240 strips. See `native-icons.md`, `loading-animations.md` and `widgets.md` for rendering and usage APIs.

No font binaries or definitions are bundled. See `fonts.md` for selecting application-supplied font resources and maintaining editable-text metrics. `verifyArtifact` rejects bundled library font resources and a global Minecraft font override.

Consumers can use a built JAR as a local Gradle file dependency:

```groovy
dependencies {
    implementation files('libs/oreui-lib-0.1.0.jar')
}
```

Consumers using these widgets also declare `oreui_lib` as a required client-side dependency in their own NeoForge metadata. No publishing repository is configured and no consumer project is modified by this project.

Player previews optionally copy Curios 9.5.1–9.x equipment. The build resolves Curios from its group-scoped official Maven repository as `compileOnly`, without bundling it or requiring it for normal runs. `gradlew.bat runClient -PwithCurios` adds it to the development runtime for manual review. See `3d-previews.md` for snapshot behavior and compatibility limits.

## API ownership

`OreScreen` shares scrolling, clipping, focus and input cancellation between the gallery and popups. `OreColumn` and padded `OreSection` compose native `LinearLayout`. `OreEditBox` and `OreTextField` style Minecraft's editor without changing its editing model. Native item, block-state and isolated player previews reuse Minecraft's renderers. See `widgets.md`, `test-screen.md` and `3d-previews.md` for integration and lifecycle requirements.
