@echo off
setlocal
cd /d "%~dp0"
REM ====== Cambia esto si quieres otro dueno o nombre de repositorio ======
set REPO=andrewBristowx/EmiCobbleverse-pack
REM =======================================================================
where git >nul 2>nul || (echo Falta Git. Instalalo desde https://git-scm.com & pause & exit /b 1)
where gh  >nul 2>nul || (echo Falta GitHub CLI. Instalalo desde https://cli.github.com y ejecuta: gh auth login & pause & exit /b 1)

if not exist ".git" git init -b main
git add -A
git commit -m "EmiCobbleverse pack" 2>nul

git remote get-url origin >nul 2>nul
if errorlevel 1 (
  echo Creando el repositorio publico %REPO% ...
  gh repo create %REPO% --public --source . --remote origin --push
) else (
  echo Subiendo cambios a origin ...
  git push -u origin main
)
echo.
echo Listo. La instancia de Prism descarga desde:
echo   https://raw.githubusercontent.com/%REPO%/main/pack.toml
pause
