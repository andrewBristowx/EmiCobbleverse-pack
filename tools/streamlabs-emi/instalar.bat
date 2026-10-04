@echo off
cd /d "%~dp0"
echo Instalando (solo hace falta una vez)...
call npm install --omit=dev
pause
