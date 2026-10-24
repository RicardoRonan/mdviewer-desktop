# MDViewer Desktop

The Linux desktop version of MDViewer, an offline Markdown reader with portable, block-anchored
annotations. It reuses the Android app's parser, anchoring engine, and reader design, and runs as a
native Compose Desktop application.

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

- Linux x86_64 with a desktop session
- JDK 17 (only needed to build; the AppImage ships its own runtime)

## Run from source

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew run --args="$HOME/Documents/notes.md"
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

## Where your data lives

| Path | Contents |
| --- | --- |
| `~/.config/mdviewer/settings.json` | Theme, text size, image preference, library folder |
| `~/.local/share/mdviewer/library.json` | Document index, reading positions, annotations |

Both honour `XDG_CONFIG_HOME` and `XDG_DATA_HOME`.

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
