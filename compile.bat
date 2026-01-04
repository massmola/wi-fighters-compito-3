@echo off
if not exist bin mkdir bin
javac -d bin -sourcepath src src/server/*.java src/client/*.java src/worker/*.java src/common/*.java
echo Compilation finished.
pause
