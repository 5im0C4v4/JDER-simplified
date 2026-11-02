# JDER ER Editor

A lightweight desktop fork of JDER focused on creating Entity-Relationship diagrams for the Obsidian ERModel plugin.

The editor opens directly into the ER workspace. It keeps JDER's JSON format and supports Chen-style entities, relationships, attributes, connectors, notes, weak entities, and relationship cardinalities.

## Requirements

- Java 11 or later
- Windows, macOS, or Linux

## Run from the command line

```text
java -jar JDER.jar [--json <diagram.json>] [--png <preview.png>]
```

| Option | Description |
| --- | --- |
| `--json <file>` | Open an ER diagram. If the file does not exist, start a blank ER diagram associated with that path; it is saved automatically after the first edit. |
| `--png <file>` | Automatically export or refresh the PNG preview as the diagram changes. |
| `--file <file>` | Alias for `--json`, retained for compatibility with the earlier plugin integration. |
| `--preview <file>` | Alias for `--png`, retained for compatibility with the earlier plugin integration. |
| `-h`, `--help` | Show command-line help. |

File extensions are added when omitted. Paths containing spaces should be quoted. Missing output folders are created as needed. An existing JSON file that is invalid or is not an ER diagram is not opened or overwritten.

Example Obsidian vault layout:

```text
ERModel/
  jder.jar
  json/customer.json
  png/customer.png
```

Launch the editor with:

```text
java -jar "ERModel/jder.jar" --json "ERModel/json/customer.json" --png "ERModel/png/customer.png"
```

For an existing JSON diagram, use **File → Save** (or enable **File → Automatic Save**) to persist edits. The PNG is refreshed automatically. For a new JSON path, automatic JSON saving starts after the first edit.

## Build the standalone JAR

From the repository root, run:

```bash
./gradlew jar
```

On Windows PowerShell:

```powershell
.\gradlew.bat jar
```

The universal standalone JAR is written to `build/libs/JDER.jar`. It includes native libraries for all supported targets.

To also build platform-specific JARs, run:

```powershell
.\gradlew.bat jar jarWindowsX64 jarLinuxX64 jarLinuxArm64 jarMacosX64 jarMacosArm64
```

The platform-specific JARs are written to `build/libs/`:

| Target | JAR |
| --- | --- |
| Windows x64 | `JDER-windows-x64.jar` |
| Linux x64 | `JDER-linux-x64.jar` |
| Linux ARM64 | `JDER-linux-arm64.jar` |
| macOS x64 | `JDER-macos-x64.jar` |
| macOS ARM64 | `JDER-macos-arm64.jar` |

Include the universal JAR and any platform-specific JARs as assets in the same GitHub Release. Users should download the platform-specific file matching their system, or the universal JAR if they need one file that supports every listed target.

## Examples

[`examples/universita.json`](examples/universita.json) is an ER diagram that can be opened in the editor.

## License and credits

This project is licensed under the MIT License; see [LICENSE.md](LICENSE.md).

JDER is a rewrite inspired by the work of [Alessandro Ballini](https://ballini.it/Software/ProgER) and [Gianvito Pio](https://gianvitopio.wordpress.com/jder).
