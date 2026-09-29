@echo off
title SMART PARKING CLIENT (C++ TCP CLIENT)
cd /d "%~dp0client"
if not exist client.exe (
    echo [BUILD] Dang bien dich client.cpp...
    g++ -O2 -std=c++17 -Wall client.cpp -o client.exe -lws2_32
)
echo [START] Dang khoi dong C++ Client...
client.exe
pause
