@echo off
echo   Build Client - Mock RFID ESP32
cl /EHsc /W3 /Fe:client.exe client.cpp Ws2_32.lib
if %ERRORLEVEL% == 0 (
    echo.
    echo [OK] Build thanh cong! Chay: client.exe
) else (
    echo.
    echo [LOI] Build that bai. Kiem tra lai MSVC.
)
pause
