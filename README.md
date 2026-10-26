# MDViewer Desktop

An offline Markdown reader with portable, block-anchored annotations, for **Linux and Windows**.
It reuses the Android app's parser, anchoring engine, and reader design, and runs as a native Compose
Desktop application.

## Download

Grab a prebuilt package from the latest release:

| Platform | File |
| --- | --- |
| Windows 10/11 (x64) | [`MDViewer-<version>-Setup.exe`](https://github.com/RicardoRonan/mdviewer-desktop/releases/latest) |
| Linux (AppImage) | [`MDViewer-<version>-x86_64.AppImage`](https://github.com/RicardoRonan/mdviewer-desktop/releases/latest) |
| Linux (Debian/Ubuntu) | the `.deb` asset on the same page |

- **Windows:** run the installer, choose Start Menu / Desktop shortcuts, and opt in to opening
  `.md` / `.mdown` / `.markdown` files with MDViewer. Uninstall from *Add or remove programs*.
- **Linux AppImage:** `chmod +x MDViewer-*.AppImage && ./MDViewer-*.AppImage` (add
  `APPIMAGE_EXTRACT_AND_RUN=1` on systems without FUSE).

> **Windows SmartScreen:** the installer is not code-signed, so the first run shows
> *"Windows protected your PC"*. This is a normal warning for unsigned apps, not a virus.
> Click **More info → Run anyway** to install. (Signing would remove this — see
> [Code signing](#code-signing).)

Releases are published automatically by CI whenever a version tag (`v*`) is pushed, so the
`releases/latest` page always links to current installers.

## Features

- **Library folder browsing**: pick a folder, browse nested directories, jump back into recents.
- **Reader**: headings, paragraphs, quotes, lists and task lists, fenced code, GFM tables, images,
  horizontal rules, and clickable links.
- **Annotations**: select text to highlight it in one of seven colors, or attach a note with tags.
  Highlights survive later edits thanks to exact/prefix/suffix anchoring, and unresolved ones are
  reported as orphaned instead of silently disappearing.
- **Notebook**: every highlight and note across the library, filterable and linked back to its file.
- **Editor**: markdown formatting chips, save and preview.
- **Export and import**: JSON sidecar (`mdviewer.annotations/1`) plus a human-readable Markdown
  digest. Imports merge by id, newest wins, and deletions stay deleted.
- **Settings**: system/light/dark theme, four text sizes, optional remote images, max line width.
- **Local-first**: no accounts, no network calls unless you enable remote images.

## Requirements

- Windows 10/11 (x64) or Linux x86_64, with a desktop session
- JDK 17+ (only needed to build; the installers and AppImage ship their own runtime)
- Inno Setup 6 (only needed to build the Windows installer — `winget install JRSoftware.InnoSetup`)

## Run from source

```bash
export JAVA_HOME=/path/to/jdk-17   # Windows: set JAVA_HOME=C:\path\to\jdk-17
./gradlew run --args="$HOME/Documents/notes.md"   # Windows: .\gradlew.bat run --args="C:\path\to\notes.md"
```

Omit `--args` to start at the library screen.

## Tests

```bash
./gradlew test
```

42 tests cover the Markdown parser, annotation anchoring, JSON storage, repositories, sidecar
transfer, formatting helpers, Compose UI flows (library and reader), and an off-screen Skia render
check (`build/reports/ui/markdown-render.png`).

## Build a Linux AppImage

```bash
export JAVA_HOME=/path/to/jdk-17
./packaging/build-appimage.sh
```

The script builds the jpackage runtime image, assembles an AppDir with a desktop entry and icon,
and produces:

```text
release/MDViewer-1.0.0-x86_64.AppImage
```

Run it directly:

```bash
chmod +x release/MDViewer-1.0.0-x86_64.AppImage
./release/MDViewer-1.0.0-x86_64.AppImage ~/Documents/notes.md
```

On systems without FUSE, prefix with `APPIMAGE_EXTRACT_AND_RUN=1`.

To register it with your application menu, place the AppImage somewhere stable and add a desktop
entry:

```bash
mkdir -p ~/.local/bin ~/Applications
cp release/MDViewer-1.0.0-x86_64.AppImage ~/Applications/
ln -sf ~/Applications/MDViewer-1.0.0-x86_64.AppImage ~/.local/bin/mdviewer
cat > ~/.local/share/applications/mdviewer.desktop <<EOF
[Desktop Entry]
Type=Application
Name=MDViewer
Comment=Offline Markdown reader with annotations
Exec=$HOME/.local/bin/mdviewer %f
Icon=$HOME/Applications/mdviewer.png
Terminal=false
Categories=Office;TextTools;Viewer;
MimeType=text/markdown;text/x-markdown;
EOF
update-desktop-database ~/.local/share/applications 2>/dev/null || true
```

A Debian package can also be produced with `./gradlew packageDeb`.

## Build a Windows installer

On Windows with a JDK 17+ and [Inno Setup 6](https://jrsoftware.org/isinfo.php) installed:

```powershell
# one-time: install the Inno Setup compiler
winget install JRSoftware.InnoSetup

# build the jpackage app-image, draw icon.ico if missing, and compile the wizard
.\packaging\build-windows.bat
```

The script produces:

```text
release\MDViewer-1.0.0-Setup.exe
```

Run it to install MDViewer for the current user, add Start Menu / Desktop shortcuts, and (optionally)
register `.md` / `.mdown` / `.markdown` file associations so double-clicking a Markdown file opens
MDViewer. Under the hood it runs `gradlew createDistributable` then compiles
`packaging/windows/MDViewer.iss` with Inno Setup.

## Code signing

Release builds are **not code-signed**, so Windows SmartScreen shows *"Windows protected your PC"*
on first run. It's a reputation check on unsigned downloads, not a malware flag — the bypass is
**More info → Run anyway**.

To remove the warning permanently, sign the installer with a certificate:

- **Azure Trusted Signing** (~$9.99/mo) — Microsoft's service, quickest path to a signed build with
  fast SmartScreen reputation.
- **OV cert** (SSL.com, Sectigo, DigiCert) — ~$70–200/yr; clears "Unknown publisher" but may warn
  briefly until reputation builds.
- **EV cert** — ~$300–500/yr; historically grants reputation immediately.

Modern signing uses a cloud key-holding service (Azure Trusted Signing, SSL.com eSigner, DigiCert
KeyLocker) rather than a local `.pfx`, so CI signs via their API. Once you have credentials, wire a
signing step into `.github/workflows/release.yml` before the `softprops/action-gh-release` step.

## Where your data lives

| Platform | Settings | Library / annotations |
| --- | --- | --- |
| Linux | `~/.config/mdviewer/settings.json` | `~/.local/share/mdviewer/library.json` |
| Windows | `%APPDATA%\mdviewer\settings.json` | `%LOCALAPPDATA%\mdviewer\library.json` |

Linux paths honour `XDG_CONFIG_HOME` and `XDG_DATA_HOME`. Uninstalling the Windows app leaves these
files in place so your library and highlights are never deleted with the program.

## Project layout

```text
src/main/kotlin/dev/ricardo/mdviewer/
├── domain/          Markdown AST and parser, anchor engine, models
├── data/            JSON stores, repositories, sidecar export/import, folder index
├── feature/         Library, reader, notebook, settings screens and state holders
├── ui/              Compose theme, components, icons, dialogs
├── di/AppGraph.kt   Hand-wired object graph
├── App.kt           Route state and screen composition
└── Main.kt          Window entry point
```

## Desktop port notes

- Room is replaced by a JSON file store. The data volume is small and this keeps the packaged app
  free of native SQLite artifacts.
- DataStore settings are replaced by a JSON file behind the same `Flow<UserSettings>` surface.
- Storage Access Framework code is replaced by a library folder plus native file dialogs.
- Share intents are replaced by save dialogs for JSON and digest exports.
- Reading position is saved after the scroll settles instead of on activity pause.

## Known gaps

- Remote images stay hidden until you enable **Load remote images** in settings.
- No drag and drop, tabs, or multi-window yet.
- The library index is flat (one root folder); change it from the library screen.

## License

MIT. See [LICENSE](LICENSE).
