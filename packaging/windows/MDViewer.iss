; MDViewer Windows install wizard: Inno Setup 6 script.
; Wraps the jpackage app-image (produced by `gradlew createDistributable`) into a
; proper Windows installer with Start Menu / Desktop shortcuts and markdown file
; associations. Build via packaging\build-windows.bat.
;
; Compile with:
;   iscc packaging\windows\MDViewer.iss
; The version and app-image path are passed on the command line by build-windows.bat.

#define MyAppName "MDViewer"
#ifndef MyAppVersion
  #define MyAppVersion "1.2.0"
#endif
#ifndef MyAppExeDir
  ; Default: the jpackage app-image folder produced by gradle createDistributable.
  #define MyAppExeDir "..\..\build\compose\binaries\main\app\MDViewer"
#endif
#define MyAppPublisher "Ricardo"
#define MyAppURL "https://github.com/RicardoRonan/mdviewer-desktop"
#define MyAppExeName "MDViewer.exe"

[Setup]
AppId={{8F3D6A21-5B7C-4E9A-9C41-2D7F0B6E8A55}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}/issues
AppUpdatesURL={#MyAppURL}/releases
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
; User data lives in %APPDATA% / %LOCALAPPDATA% and is never removed by uninstall,
; so disabledirpage=no is fine and we don't need a "remove user data" step.
UninstallDisplayName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
OutputDir=..\..\release
OutputBaseFilename={#MyAppName}-{#MyAppVersion}-Setup
SetupIconFile=..\icon.ico
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=lowest
DisableProgramGroupPage=yes
ChangesAssociations=yes
MinVersion=10.0.17763

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "associate"; Description: "Open .md / .mdown / .markdown files with {#MyAppName}"; GroupDescription: "File associations:"

[Files]
; Copy the whole jpackage app-image (launcher exe + app/ runtime) into {app}.
Source: "{#MyAppExeDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Registry]
; File associations (HKCU, per-user). {app}\{#MyAppExeName} "%1" passes the file path as arg 1,
; which Main.kt already accepts. ChangesAssociations=yes triggers the shell refresh.
Root: HKCU; Subkey: "Software\Classes\.md"; ValueType: string; ValueName: ""; ValueData: "MDViewer.Markdown"; Flags: uninsdeletevalue; Tasks: associate
Root: HKCU; Subkey: "Software\Classes\.mdown"; ValueType: string; ValueName: ""; ValueData: "MDViewer.Markdown"; Flags: uninsdeletevalue; Tasks: associate
Root: HKCU; Subkey: "Software\Classes\.markdown"; ValueType: string; ValueName: ""; ValueData: "MDViewer.Markdown"; Flags: uninsdeletevalue; Tasks: associate

Root: HKCU; Subkey: "Software\Classes\MDViewer.Markdown"; ValueType: string; ValueName: ""; ValueData: "Markdown document"; Flags: uninsdeletekey; Tasks: associate
Root: HKCU; Subkey: "Software\Classes\MDViewer.Markdown\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\{#MyAppExeName},0"; Tasks: associate
Root: HKCU; Subkey: "Software\Classes\MDViewer.Markdown\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Tasks: associate

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: nowait postinstall skipifsilent
