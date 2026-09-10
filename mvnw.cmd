@echo off
setlocal
set MVN_VERSION=3.9.16
set BASE_DIR=%~dp0
set DIST_DIR=%BASE_DIR%.mvn\apache-maven-%MVN_VERSION%
set ZIP=%BASE_DIR%.mvn\apache-maven-%MVN_VERSION%-bin.zip
set URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip

if not exist "%DIST_DIR%\bin\mvn.cmd" (
  if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%URL%' -OutFile '%ZIP%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP%' '%BASE_DIR%.mvn'"
  if errorlevel 1 exit /b 1
  del "%ZIP%"
)

call "%DIST_DIR%\bin\mvn.cmd" %*
