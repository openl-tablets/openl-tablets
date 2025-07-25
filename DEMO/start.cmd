@echo off
REM ===========================================================================
REM | This batch file executes the accompanying PowerShell script 'start.ps1' |
REM | without requiring the user to change the system's execution policy.     |
REM ===========================================================================

set "THIS_SCRIPT_DIR=%~dp0"
powershell.exe -ExecutionPolicy Bypass -File "%THIS_SCRIPT_DIR%start.ps1"
