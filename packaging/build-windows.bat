@echo off
rem Builds the MDViewer Windows install wizard (Inno Setup) from the Compose Desktop distribution.
rem Mirrors packaging/build-appimage.sh on the Linux side.
setlocal enabledelayedexpansion

set "ROOT=%~dp0.."
cd /d "%ROOT%"

set "VERSION=1.0.1"
set "APP_NAME=MDViewer"
set "APP_IMAGE=%ROOT%\build\compose\binaries\main\app\%APP_NAME%"
set "RELEASE=%ROOT%\release"
set "ISS=%ROOT%\packaging\windows\MDViewer.iss"

rem --- locate a JDK 17+ (jpackage lives in a full JDK) ------------------------
if defined JAVA_HOME (
  set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
) else (
  where jpackage.exe >nul 2>nul
  if errorlevel 1 (
    echo error: set JAVA_HOME to a JDK 17+ ^(or put jpackage.exe on PATH^) >&2
    exit /b 1
  )
  set "JPACKAGE=jpackage.exe"
)

rem --- generate the .ico if missing -------------------------------------------
if not exist "%ROOT%\packaging\icon.ico" (
  echo ==^> drawing icon
  java "%ROOT%\packaging\make-ico.java" "%ROOT%\packaging\icon.ico"
  if errorlevel 1 (
    echo error: could not generate icon.ico ^(need java on PATH^) >&2
    exit /b 1
  )
)

rem --- build the runtime app-image with gradle/jpackage ------------------------
echo ==^> building runtime image with jpackage
call "%ROOT%\gradlew.bat" createDistributable --console=plain
if errorlevel 1 (
  echo error: gradle createDistributable failed >&2
  exit /b 1
)
if not exist "%APP_IMAGE%\bin\%APP_NAME%.exe" if not exist "%APP_IMAGE%\%APP_NAME%.exe" (
  echo error: jpackage output not found at %APP_IMAGE% >&2
  exit /b 1
)

rem --- locate the Inno Setup compiler -----------------------------------------
set "ISCC="
where iscc.exe >nul 2>nul && set "ISCC=iscc.exe"
if not defined ISCC if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if not defined ISCC if exist "%ProgramFiles%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles%\Inno Setup 6\ISCC.exe"
if not defined ISCC (
  echo error: Inno Setup 6 compiler ^(iscc.exe^) not found. Install with: winget install JRSoftware.InnoSetup >&2
  exit /b 1
)

rem --- compile the installer ---------------------------------------------------
echo ==^> compiling installer with Inno Setup
if not exist "%RELEASE%" mkdir "%RELEASE%"
"%ISCC%" /DMyAppVersion=%VERSION% /DMyAppExeDir=%APP_IMAGE% "%ISS%"
if errorlevel 1 (
  echo error: iscc failed >&2
  exit /b 1
)

echo ==^> done: %RELEASE%\%APP_NAME%-%VERSION%-Setup.exe
endlocal
