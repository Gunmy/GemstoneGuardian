@echo off
rem Trains the computer fighters' judge (see tools\ai\train.py). Picks up where the last run stopped;
rem press Ctrl+C to stop, everything is saved. If Python crashes it is started again after 30 seconds (at most
rem 5 times), going on from the last checkpoint and keeping the stop time. Overnight:
rem train.bat --run v4 --eval-depth 3 --eval-fights 1000 --until 07:00
cd /d "%~dp0..\.."
set tries=0
:again
python tools\ai\train.py %*
if not errorlevel 1 goto done
set /a tries+=1
if %tries% geq 5 (
    echo Crashed 5 times, giving up. See console.log in the run's folder.
    goto done
)
echo Training crashed, starting again in 30 seconds (Ctrl+C to stop)...
timeout /t 30 /nobreak >nul
goto again
:done
pause
