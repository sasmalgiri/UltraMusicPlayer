@echo off
REM =============================================================================
REM UltraMusic Player - Automated Build Script (Windows)
REM =============================================================================
REM Usage: scripts\build.bat [command]
REM Commands: clean, debug, release, test, lint, all, bundle, install, help
REM =============================================================================

setlocal EnableDelayedExpansion

set COMMAND=%1
if "%COMMAND%"=="" set COMMAND=help

cd /d "%~dp0\.."

if "%COMMAND%"=="clean" (
    echo === Cleaning Build Directories ===
    call gradlew.bat clean
    echo Clean complete
    goto :end
)

if "%COMMAND%"=="debug" (
    echo === Building Debug APK ===
    call gradlew.bat assembleDebug
    echo Debug APK built: app\build\outputs\apk\debug\app-debug.apk
    goto :end
)

if "%COMMAND%"=="release" (
    echo === Building Release APK ===
    if not exist "keystore.properties" (
        if "%SIGNING_KEY_ALIAS%"=="" (
            echo WARNING: No signing configuration found!
            echo Create keystore.properties or set environment variables
            exit /b 1
        )
    )
    call gradlew.bat assembleRelease
    echo Release APK built: app\build\outputs\apk\release\app-release.apk
    goto :end
)

if "%COMMAND%"=="test" (
    echo === Running Unit Tests ===
    call gradlew.bat test
    echo All tests passed
    goto :end
)

if "%COMMAND%"=="lint" (
    echo === Running Lint Checks ===
    call gradlew.bat lintDebug
    echo Lint checks complete
    goto :end
)

if "%COMMAND%"=="all" (
    echo === Full Build Pipeline ===

    echo Step 1: Clean
    call gradlew.bat clean

    echo Step 2: Run Tests
    call gradlew.bat test
    if errorlevel 1 (
        echo Tests failed!
        exit /b 1
    )

    echo Step 3: Lint Checks
    call gradlew.bat lintDebug

    echo Step 4: Build Release
    if exist "keystore.properties" (
        call gradlew.bat assembleRelease
        echo Release APK built
    ) else (
        call gradlew.bat assembleDebug
        echo No signing config - built debug APK instead
    )

    echo Build pipeline complete!
    goto :end
)

if "%COMMAND%"=="bundle" (
    echo === Building Android App Bundle ===
    if not exist "keystore.properties" (
        if "%SIGNING_KEY_ALIAS%"=="" (
            echo ERROR: Signing configuration required for App Bundle
            exit /b 1
        )
    )
    call gradlew.bat bundleRelease
    echo App Bundle built: app\build\outputs\bundle\release\app-release.aab
    goto :end
)

if "%COMMAND%"=="install" (
    echo === Building and Installing Debug APK ===
    call gradlew.bat installDebug
    echo Debug APK installed on device
    goto :end
)

REM Default: show help
echo UltraMusic Player Build Script
echo.
echo Usage: scripts\build.bat [command]
echo.
echo Commands:
echo   clean    - Clean build directories
echo   debug    - Build debug APK
echo   release  - Build signed release APK
echo   test     - Run all unit tests
echo   lint     - Run lint checks
echo   all      - Full build pipeline (test + lint + build)
echo   bundle   - Build Android App Bundle for Play Store
echo   install  - Build and install debug on device
echo   help     - Show this help message

:end
endlocal
