@echo off
echo     Bai Do Xe Thong Minh
cl /EHsc /W3 /Fe:server.exe server.cpp Ws2_32.lib
if %ERRORLEVEL% == 0 (
    echo.
    echo [OK] Build thanh cong! Chay: server.exe
) else (
    echo.
    echo [LOI] Build that bai. Kiem tra lai MSVC.
)
pause
