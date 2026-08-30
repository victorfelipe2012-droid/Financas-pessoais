@echo off
title Compilando PrivaFin Windows Desktop
cd /d "%~dp0"

echo ============================================================
echo   Compilando PrivaFin Desktop Nativo (WPF / .NET Framework)
echo ============================================================
echo.

set CSC="C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if not exist %CSC% (
    set CSC="C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe"
)

%CSC% /target:winexe /out:PrivaFin.exe /r:C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF\PresentationCore.dll /r:C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF\PresentationFramework.dll /r:C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF\WindowsBase.dll /r:System.Xaml.dll /r:System.Net.Http.dll windows\PrivaFinDesktop.cs

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCESSO] PrivaFin.exe compilado com sucesso!
) else (
    echo.
    echo [ERRO] Ocorreu uma falha na compilacao.
    pause
)
