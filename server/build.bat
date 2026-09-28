@echo off
echo =============================================
echo   Build Server - Bai Do Xe Thong Minh
echo =============================================
cl /EHsc /W3 /Fe:server.exe server.cpp Ws2_32.lib
if %ERRORLEVEL% == 0 (
    echo.
    echo [OK] Build thanh cong! Chay: server.exe
) else (
    echo.
    echo [LOI] Build that bai. Kiem tra lai MSVC.
)
pause
