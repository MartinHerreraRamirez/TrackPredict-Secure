@echo off
REM Borra la base de datos y vuelve a levantar todo con los datos de ejemplo.
cd /d "%~dp0"
docker compose down -v
call iniciar.bat
