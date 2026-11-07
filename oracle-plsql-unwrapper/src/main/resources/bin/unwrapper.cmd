@echo off
rem SPDX-License-Identifier: BSD-3-Clause
rem
rem Runs the executable jar of this distribution. JAVA_HOME selects the JDK, JAVA_OPTS adds options of the JVM.

setlocal
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java"
for %%f in ("%~dp0..\*.jar") do "%JAVA%" %JAVA_OPTS% -jar "%%~ff" %*
exit /b %errorlevel%
