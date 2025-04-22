@echo off
set CONTAINER_NAME=iot-container

:: Check if container exists
docker ps -a --format "{{.Names}}" | findstr /I /C:"%CONTAINER_NAME%" >nul

if %errorlevel%==0 (
    echo Starter eksisterende container "%CONTAINER_NAME%"...
    docker start -ai %CONTAINER_NAME%
) else (
    echo Container "%CONTAINER_NAME%" findes ikke!
    echo Du skal først oprette den med:
    echo docker run --name iot-container -v "%cd%:/app" -w /app -it sep4-java-env bash
)
