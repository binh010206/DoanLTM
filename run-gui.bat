@echo off
title PARKING MONITOR (JAVA DESKTOP GUI)
cd /d "%~dp0monitor"
if exist flatlaf.jar (
    java -cp ".;flatlaf.jar" ParkingMonitor
) else (
    java ParkingMonitor
)
