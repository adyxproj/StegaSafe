@echo off
title StegaSafe Desktop
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "JAR=%~dp0target\stegasafe-1.0.0.jar"

if not exist "%JAR%" (
    echo [ERROR] stegasafe-1.0.0.jar not found!
    echo Please build first by running build.bat
    echo.
    pause
    exit /b 1
)

echo Starting StegaSafe Desktop Application...
start "" "%JAVA_HOME%\bin\javaw.exe" ^
    --enable-native-access=ALL-UNNAMED ^
    -Dsun.java2d.uiScale=1.0 ^
    -Dloader.main=com.stegasafe.desktop.StegaSafeDesktopApp ^
    -cp "%JAR%" ^
    org.springframework.boot.loader.launch.PropertiesLauncher

if %ERRORLEVEL% NEQ 0 (
    echo Launching with console mode...
    "%JAVA_HOME%\bin\java.exe" ^
        --enable-native-access=ALL-UNNAMED ^
        -Dsun.java2d.uiScale=1.0 ^
        -Dloader.main=com.stegasafe.desktop.StegaSafeDesktopApp ^
        -cp "%JAR%" ^
        org.springframework.boot.loader.launch.PropertiesLauncher
    pause
)
