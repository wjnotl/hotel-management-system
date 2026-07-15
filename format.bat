@echo off
SETLOCAL EnableDelayedExpansion

:: ---------------------------------------------------------------------
:: Configuration
:: ---------------------------------------------------------------------
SET LIB_DIR=lib
SET FORMATTER_JAR=%LIB_DIR%\google-java-format-1.35.0-all-deps.jar
SET DOWNLOAD_URL=https://github.com/google/google-java-format/releases/download/v1.35.0/google-java-format-1.35.0-all-deps.jar

echo ===================================================
echo  Auto-Formatting Java Source Files
echo ===================================================
echo.

:: ---------------------------------------------------------------------
:: Auto-download Formatter JAR if missing
:: ---------------------------------------------------------------------
if not exist "%FORMATTER_JAR%" (
  echo [INFO] Google Java Format JAR not found locally.
  echo [INFO] Downloading to %FORMATTER_JAR%...
  
  if not exist "%LIB_DIR%" mkdir "%LIB_DIR%"
  
  curl -L -s -o "%FORMATTER_JAR%" "%DOWNLOAD_URL%"
  
  if !errorlevel! neq 0 (
    echo [ERROR] Failed to download the formatter JAR.
    echo Check your internet connection.
    goto :fail
  )
  echo [SUCCESS] Download complete.
  echo.
)

:: ---------------------------------------------------------------------
:: Format Code
:: ---------------------------------------------------------------------
echo Formatting all Java files in "src/"...

set FORMATTED_COUNT=0
for /R src %%f in (*.java) do (
  echo  Formatting: %%f
  java -jar "%FORMATTER_JAR%" --replace "%%f"
  set /a FORMATTED_COUNT+=1
)

echo.
echo ===================================================
echo  [SUCCESS] !FORMATTED_COUNT! file(s) formatted successfully!
echo ===================================================
ENDLOCAL
exit /b 0

:fail
echo.
echo ===================================================
echo  [FAILURE] Formatting failed.
echo ===================================================
ENDLOCAL
exit /b 1
