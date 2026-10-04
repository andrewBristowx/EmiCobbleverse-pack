@echo off
cd /d "%~dp0"
title Donaciones Streamlabs - EmiCobbleverse
:inicio
node streamlabs-emi.js
echo.
echo El programa se ha cerrado. Reiniciando en 10 segundos (cierra esta ventana para parar)...
timeout /t 10 >nul
goto inicio
