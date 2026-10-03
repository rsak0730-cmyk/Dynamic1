@echo off
setlocal
for /f "tokens=2 delims=-" %%a in ('findstr /c:"distributionUrl" gradle\wrapper\gradle-wrapper.properties') do set GRADLE_VERSION=%%a
set GRADLE_VERSION=%GRADLE_VERSION:-bin.zip=%
set CACHE_DIR=%USERPROFILE%\.gradle\dynamic-island-wrapper\%GRADLE_VERSION%
if not exist "%CACHE_DIR%\bin\gradle.bat" (
  mkdir "%CACHE_DIR%"
  curl.exe -fL --retry 3 "https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip" -o "%CACHE_DIR%\gradle.zip"
  powershell -NoProfile -Command "Expand-Archive -Force '%CACHE_DIR%\gradle.zip' '%CACHE_DIR%\unpacked'"
  move "%CACHE_DIR%\unpacked\gradle-%GRADLE_VERSION%\*" "%CACHE_DIR%\" >nul
  rmdir /s /q "%CACHE_DIR%\unpacked"
  del "%CACHE_DIR%\gradle.zip"
)
call "%CACHE_DIR%\bin\gradle.bat" %*
