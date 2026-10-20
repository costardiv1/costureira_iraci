@echo off
rem Compila e executa o sistema (Windows). Requer JDK 8 ou superior no PATH.
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
dir /s /b src\main\java\*.java > build\fontes.txt
javac -encoding UTF-8 -d build\classes @build\fontes.txt
if errorlevel 1 goto erro
echo Main-Class: costureira.Main> build\manifest.txt
jar cfm build\costureira-iraci.jar build\manifest.txt -C build\classes .
if errorlevel 1 goto erro
java -jar build\costureira-iraci.jar
goto fim
:erro
echo.
echo Erro ao compilar. Verifique se o JDK esta instalado (comando javac).
pause
:fim
