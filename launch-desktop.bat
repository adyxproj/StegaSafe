@echo off
title StegaSafe Desktop
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "JAR=%~dp0target\stegasafe-1.0.0.jar"

if not exist "%JAR%" (
    echo ERROR: stegasafe-1.0.0.jar not found. Please build first with build.bat
    pause
    exit /b 1
)

echo Starting StegaSafe Desktop Application...
"%JAVA_HOME%\bin\java.exe" ^
    -Dsun.java2d.uiScale=1.0 ^
    -cp "%JAR%" ^
    com.stegasafe.desktop.StegaSafeDesktopApp
