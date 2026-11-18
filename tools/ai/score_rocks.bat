@echo off
rem Scores every rock for balancing (see tools\ai\score_rocks.py). Stop with Ctrl+C and run again to go on.
cd /d "%~dp0..\.."
python tools\ai\score_rocks.py %*
pause
