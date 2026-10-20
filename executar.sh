#!/bin/sh
# Compila e executa o sistema (Linux/macOS). Requer JDK 8 ou superior.
cd "$(dirname "$0")" || exit 1
rm -rf build && mkdir -p build/classes
find src/main/java -name '*.java' > build/fontes.txt
javac -encoding UTF-8 -d build/classes @build/fontes.txt || exit 1
printf 'Main-Class: costureira.Main\n' > build/manifest.txt
jar cfm build/costureira-iraci.jar build/manifest.txt -C build/classes . || exit 1
java -jar build/costureira-iraci.jar
