@REM Maven wrapper batch script for Windows
@echo off

setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@REM Download maven-wrapper.jar if not present
if not exist %WRAPPER_JAR% (
    echo Downloading maven-wrapper.jar...
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
)

@REM Set MAVEN_HOME
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
set MAVEN_BIN=%MAVEN_HOME%\apache-maven-3.9.6\bin\mvn.cmd

@REM Download maven if not present
if not exist "%MAVEN_BIN%" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%MAVEN_HOME%'"
)

@REM Execute maven
echo Executing Maven...
call "%MAVEN_BIN%" %*

endlocal
