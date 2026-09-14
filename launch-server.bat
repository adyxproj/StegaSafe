@echo off
title StegaSafe Web Server
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "JAR=%~dp0target\stegasafe-1.0.0.jar"

if not exist "%JAR%" (
    echo ERROR: stegasafe-1.0.0.jar not found. Please build first with build.bat
    pause
    exit /b 1
)

echo Starting StegaSafe Web Server on http://localhost:8080 ...
echo Press Ctrl+C to stop the server.
"%JAVA_HOME%\bin\java.exe" -jar "%JAR%"
