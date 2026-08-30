@echo off
title PrivaFin - Financas Pessoais Windows
cd /d "%~dp0"

if not exist "PrivaFin.exe" (
    echo [PrivaFin] Compilando executavel nativo para Windows...
    call build-windows.bat
)

echo [PrivaFin] Iniciando aplicativo nativo Windows...
start "" "PrivaFin.exe"
exit
