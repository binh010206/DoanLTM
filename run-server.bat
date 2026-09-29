@echo off
title SMART PARKING SERVER (C++ TCP 8888 + HTTP 9999)
cd /d "%~dp0server"
if not exist server.exe (
    echo [BUILD] Dang bien dich server.cpp...
    g++ -O2 -std=c++17 -Wall server.cpp -o server.exe -lws2_32
)
echo [START] Dang khoi dong C++ Server...
server.exe
pause
