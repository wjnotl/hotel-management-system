@echo off
SETLOCAL EnableDelayedExpansion

:: ---------------------------------------------------------------------
:: Configuration
:: ---------------------------------------------------------------------
SET LIB_DIR=lib
SET FORMATTER_JAR=%LIB_DIR%\google-java-format-1.35.0-all-deps.jar
SET DOWNLOAD_URL=https://github.com/google/google-java-format/releases/download/v1.35.0/google-java-format-1.35.0-all-deps.jar

echo ===================================================
echo  Running Local Code Quality Verification
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
    echo Check internet connection or download it manually.
    goto :dry_fail
  )
  echo [SUCCESS] Download complete.
  echo.
)

:: ---------------------------------------------------------------------
:: Verify Java Environment
:: ---------------------------------------------------------------------
echo [1/3] Verifying Java environment...
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  set JAVAVER=%%g
)
set JAVAVER=%JAVAVER:"=%

echo Current Java version: %JAVAVER%
echo %JAVAVER% | findstr "^26\." >nul
if %errorlevel% neq 0 (
  echo [WARNING] Expected Java version 26.
  echo.
)

:: ---------------------------------------------------------------------
:: Check Code Formatting
:: ---------------------------------------------------------------------
echo [2/3] Checking Google Java Format style compliance...

set FORMAT_ERRORS=0
for /R src %%f in (*.java) do (
  java -jar "%FORMATTER_JAR%" --set-exit-if-changed --dry-run "%%f" >nul 2>&1

  if !errorlevel! neq 0 (
    echo  [STYLE MISMATCH] %%f
    set FORMAT_ERRORS=1
  )
)

if %FORMAT_ERRORS% neq 0 (
  echo.
  echo [FAIL] Code formatting check failed. The files listed above have style mismatches.
  goto :dry_fail
  ) else (
  echo [PASS] All source files are correctly styled.
)
echo.

:: ---------------------------------------------------------------------
:: Build with Ant
:: ---------------------------------------------------------------------
echo [3/3] Compiling and building JAR with Ant...
call ant clean jar

if %errorlevel% neq 0 (
  echo.
  echo [FAIL] Compilation or build step failed.
  goto :dry_fail
)

echo.
echo ===================================================
echo  [SUCCESS] All checks completed successfully!
echo ===================================================
ENDLOCAL
exit /b 0

:dry_fail
echo.
echo ===================================================
echo  [FAILURE] Verification failed. Fix the errors above.
echo ===================================================
if %FORMAT_ERRORS% neq 0 (
  echo * Formatting Tip: Run "format.bat" in your terminal to auto-format your project.
)
echo.
ENDLOCAL
exit /b 1
