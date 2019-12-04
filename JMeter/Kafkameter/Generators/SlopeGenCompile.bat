@echo off
"C:\Program Files\Java\jdk1.8.0_221\bin\javac.exe" SlopeGenerator.java -classpath ..\target\kafkameter-0.2.0.jar
"C:\Program Files\Java\jdk1.8.0_221\bin\jar.exe"  cvf SlopeGenerator.jar SlopeGenerator.class