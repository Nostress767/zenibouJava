@echo off
if not exist external mkdir external
cd external
if not exist jna-5.11.0 curl -L --output jna-5.11.0.zip --url https://github.com/java-native-access/jna/archive/refs/tags/5.11.0.zip
if not exist jna-5.11.0 tar -xf jna-5.11.0.zip
if exist jna-5.11.0.zip del jna-5.11.0.zip

if not exist jdk-19 curl -L --output jdk-19.zip --url https://download.java.net/java/early_access/jdk19/21/GPL/openjdk-19-ea+21_windows-x64_bin.zip
if not exist jdk-19 tar -xf jdk-19.zip
if exist jdk-19.zip del jdk-19.zip
cd ..

@set PATH=%path%;%CD%\external\jdk-19\bin
@echo on
@cmd
