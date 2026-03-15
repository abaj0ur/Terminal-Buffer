@rem Gradle wrapper for Windows
@if "%DEBUG%"=="" @echo off

set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)

"%JAVACMD%" -classpath "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" ^
  org.gradle.wrapper.GradleWrapperMain %*
