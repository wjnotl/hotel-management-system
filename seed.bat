@echo off
SETLOCAL EnableDelayedExpansion
cls

echo ===================================================
echo  [WARNING] DATABASE DESTRUCTIVE SEEDER SCRIPT
echo ===================================================
echo.
echo  This script will PERMANENTLY ERASE all binary data files
echo  (*.dat) in your project directory and re-seed fresh test records!
echo.
echo ===================================================
echo.

set /p CONFIRM="Are you SURE you want to wipe all data and re-seed? (Y/N): "

if /i "%CONFIRM%" neq "Y" (
    echo.
    echo [CANCELLED] Operation aborted by user. No data was modified.
    echo.
    ENDLOCAL
    exit /b 0
)

echo.
echo [1/3] Clearing existing binary database files (*.dat)...
if exist *.dat (
    del /f /q *.dat
    echo [SUCCESS] Binary data files wiped.
) else (
    echo [INFO] No existing *.dat files found to delete.
)

echo.
echo [2/3] Rebuilding JAR executable with Ant...
call ant jar

if %errorlevel% neq 0 (
    echo.
    echo [FAIL] Compilation or build failed. Aborting database initialization.
    ENDLOCAL
    exit /b 1
)

echo.
echo [3/3] Launching system in SEED mode...
call java -jar "dist\HotelManagementSystem.jar" --seed

ENDLOCAL
exit /b 0
