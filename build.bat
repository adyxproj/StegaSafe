@echo off
title StegaSafe Build
set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
set "MVN=C:\Users\ELCOT\maven\apache-maven-3.9.6\bin\mvn.cmd"

echo Building StegaSafe...
"%MVN%" package -DskipTests -f "%~dp0pom.xml"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! JAR: target\stegasafe-1.0.0.jar
    echo Run desktop:  launch-desktop.bat
    echo Run web app:  launch-server.bat
) else (
    echo BUILD FAILED. Check output above.
)
pause
