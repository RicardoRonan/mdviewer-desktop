# MDViewer Desktop

MDViewer is an offline Markdown reader for **Linux and Windows**. Your highlights and notes stay
portable. They stay attached to the right spot in the text, even after you edit the file. The app
shares the Android app's parser and reader design. It runs as a native desktop app.

## Download

Download a ready-made package from the latest release:

| Platform | File |
| --- | --- |
| Windows 10/11 (x64) | [`MDViewer-<version>-Setup.exe`](https://github.com/RicardoRonan/mdviewer-desktop/releases/latest) |
| Linux (AppImage) | [`MDViewer-<version>-x86_64.AppImage`](https://github.com/RicardoRonan/mdviewer-desktop/releases/latest) |
| Linux (Debian/Ubuntu) | the `.deb` asset on the same page |

- **Windows:** Run the installer. Pick your Start Menu and Desktop shortcuts. Choose to open
  `.md`, `.mdown`, and `.markdown` files with MDViewer. To uninstall, go to *Add or remove programs*.
- **Linux AppImage:** `chmod +x MDViewer-*.AppImage && ./MDViewer-*.AppImage` (add
  `APPIMAGE_EXTRACT_AND_RUN=1` on systems without FUSE).

> **Windows SmartScreen:** The installer is not signed, so the first run shows
> *"Windows protected your PC"*. This warning is normal for unsigned apps. It does not mean the
> file has a virus. Click **More info → Run anyway** to install. Signing the app would remove
> this. See [Code signing](#code-signing).

CI publishes a release every time you push a version tag (`v*`). The `releases/latest` page always
links to the newest installers.

## Features

- **Library folder browsing**: pick a folder, browse nested directories, and jump back into recents.
- **Reader**: headings, paragraphs, quotes, lists, task lists, fenced code, GFM tables, images,
  horizontal rules, and clickable links.
- **Annotations**: select text to highlight it in one of seven colors. Or attach a note with tags.
  Highlights survive later edits. Exact, prefix, and suffix anchoring makes that work. If a
  highlight can no longer be found, the app flags it as orphaned. It never vanishes on its own.
- **Notebook**: every highlight and note in the library. Filter them and jump back to the file.
- **Editor**: markdown formatting chips, save, and preview.
- **Export and import**: a JSON sidecar (`mdviewer.annotations/1`) plus a readable Markdown digest.
  Imports merge by id. The newest copy wins. Deleted highlights stay deleted.
- **Settings**: system, light, or dark theme. Four text sizes. Optional remote images. Max line
  width.
- **Local-first**: no accounts. No network calls unless you enable remote images.

## Requirements

- Windows 10/11 (x64) or Linux x86_64, with a desktop session
- JDK 17+ (only needed to build; the installers and AppImage ship their own runtime)
- Inno Setup 6 (only needed to build the Windows installer: `winget install JRSoftware.InnoSetup`)

## Run from source

```bash
export JAVA_HOME=/path/to/jdk-17   # Windows: set JAVA_HOME=C:\path\to\jdk-17
./gradlew run --args="$HOME/Documents/notes.md"   # Windows: .\gradlew.bat run --args="C:\path\to\notes.md"
```

Leave off `--args` to start at the library screen.

## Tests

```bash
./gradlew test
```

55 tests cover the main parts of the app: the Markdown parser, annotation anchoring, JSON storage,
repositories, sidecar transfer, and formatting helpers. They also cover the Compose UI flows for
the library and reader, plus the chapter rail outline and windowing. An off-screen Skia render check writes
`build/reports/ui/markdown-render.png`.

## Build a Linux AppImage

```bash
export JAVA_HOME=/path/to/jdk-17
./packaging/build-appimage.sh
```

The script builds the runtime image. It assembles an AppDir with a desktop entry and icon. It
then produces:

```text
release/MDViewer-1.1.0-x86_64.AppImage
```

Run it directly:

```bash
chmod +x release/MDViewer-1.1.0-x86_64.AppImage
./release/MDViewer-1.1.0-x86_64.AppImage ~/Documents/notes.md
```

On systems without FUSE, prefix the command with `APPIMAGE_EXTRACT_AND_RUN=1`.

To add it to your application menu, move the AppImage to a fixed location. Then add a desktop
entry:

```bash
mkdir -p ~/.local/bin ~/Applications
cp release/MDViewer-1.1.0-x86_64.AppImage ~/Applications/
ln -sf ~/Applications/MDViewer-1.1.0-x86_64.AppImage ~/.local/bin/mdviewer
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

You can also build a Debian package with `./gradlew packageDeb`.

## Build a Windows installer

On Windows, you need a JDK 17+ and [Inno Setup 6](https://jrsoftware.org/isinfo.php):

```powershell
# one-time: install the Inno Setup compiler
winget install JRSoftware.InnoSetup

# build the jpackage app-image, draw icon.ico if missing, and compile the wizard
.\packaging\build-windows.bat
```

The script produces:

```text
release\MDViewer-1.1.0-Setup.exe
```

Run it to install MDViewer for the current user. It adds Start Menu and Desktop shortcuts. It can
also register `.md`, `.mdown`, and `.markdown` files, so double-clicking a Markdown file opens
MDViewer. The script runs `gradlew createDistributable`. It then compiles
`packaging/windows/MDViewer.iss` with Inno Setup.

## Code signing

Release builds are **not signed**. So Windows SmartScreen shows *"Windows protected your PC"* on
first run. SmartScreen checks the reputation of downloads. It is not a malware scan. To bypass it,
click **More info → Run anyway**.

To remove the warning for good, sign the installer with a certificate:

- **Azure Trusted Signing** (about $9.99/month). This is Microsoft's service. It is the quickest
  path to a signed build with fast SmartScreen reputation.
- **OV cert** (SSL.com, Sectigo, DigiCert). About $70 to 200 a year. It clears "Unknown publisher".
  You may still see a warning until the app builds reputation.
- **EV cert**. About $300 to 500 a year. It has a history of granting reputation right away.

Today, signing uses a cloud key service. Options include Azure Trusted Signing, SSL.com eSigner,
and DigiCert KeyLocker. Your private key stays with them, not in a local `.pfx` file. CI signs
through their API. Once you have credentials, add a signing step to `.github/workflows/release.yml`.
Put it before the `softprops/action-gh-release` step.

## Where your data lives

| Platform | Settings | Library / annotations |
| --- | --- | --- |
| Linux | `~/.config/mdviewer/settings.json` | `~/.local/share/mdviewer/library.json` |
| Windows | `%APPDATA%\mdviewer\settings.json` | `%LOCALAPPDATA%\mdviewer\library.json` |

Linux paths respect `XDG_CONFIG_HOME` and `XDG_DATA_HOME`. Uninstalling the Windows app leaves
these files alone. Your library and highlights are never deleted with the program.

### Share your notes with an AI agent

Every highlight and note lives in `library.json`. That one file is what you hand to an AI agent
when you want it to read what you marked:

- **Windows:** `%LOCALAPPDATA%\mdviewer\library.json`
- **Linux:** `~/.local/share/mdviewer/library.json`

To open it on Windows, press Win+R, paste `%LOCALAPPDATA%\mdviewer`, and press Enter. Then
double-click `library.json` and paste its contents into the chat, or attach the file.

The file has two lists:

- `documents` is the index behind the library screen.
- `annotations` holds one entry per highlight, note, or bookmark.

```json
{
  "documents": [],
  "annotations": [
    {
      "docUri": "file:///C:/Users/me/Documents/plan.md",
      "kind": "NOTE",
      "color": "#FFE8A3",
      "selector": {
        "exact": "the sentence you selected",
        "prefix": "",
        "suffix": "",
        "start": 12,
        "end": 36,
        "blockId": "b7"
      },
      "note": "Check this against the spec",
      "tags": ["todo"],
      "deleted": false,
      "orphaned": false
    }
  ]
}
```

Read it like this:

- `docUri` names the Markdown file the entry belongs to.
- `selector.exact` repeats the text you selected, so an agent can quote it without opening the
  file.
- `note` and `tags` hold what you wrote.
- `orphaned` means the quote no longer matches after an edit. The note is still there.

Two things to know before you share it. The file lists the paths of every document in your
library. And the app rewrites the whole file on each save, so copy it or paste it rather than
pointing an agent at the live file.

Each entry also carries `id`, `createdAt`, and `updatedAt`, left out of the sample above for
length.

To share one document instead of the whole library, use **Export** in the reader. It writes a
`<name>.md.annot.json` sidecar and a readable Markdown digest.

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

- Room is replaced by a JSON file store. The data volume is small. This keeps the packaged app
  free of native SQLite files.
- DataStore settings are replaced by a JSON file. It keeps the same `Flow<UserSettings>` interface.
- Storage Access Framework code is replaced by a library folder and native file dialogs.
- Share intents are replaced by save dialogs for JSON and digest exports.
- Reading position is saved when the scroll settles, not on activity pause.

## Known gaps

- Remote images stay hidden until you enable **Load remote images** in settings.
- No drag and drop, tabs, or multi-window yet.
- The library index is flat (one root folder). Change it from the library screen.

## License

MIT. See [LICENSE](LICENSE).
