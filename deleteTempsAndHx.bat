@echo off
            @rem Built by Viskit on December 20 2007 at 2242 hrs
        @rem Use with caution, will delete all files in the %TEMP%, or %TMP% locations on Windows XP boxes
cd %TEMP%
@rem wipe out all "temp" sub-directories and any contained files without confirmation to execute
for /d %%d in (*) do rd %%d /s /q
@rem wipe out all remaining files not locked in temp as well
del *.* /q
@rem then delete the .viskit_history.xml located in %USERPROFILE%
cd %USERPROFILE%
del .viskit_history.xml
@rem this cleaning process should give us a fresh start when launching Viskit within different applications, or standalone