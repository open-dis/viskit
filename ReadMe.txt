2008 JUL 07

There are 2 Viskit launch files included within this base directory for quick and easy launching and building of Viskit. 

1) quick-run.bat: Launches Viskit standalone with no module dependencies
2) run.bat:	  First builds, then launches Viskit standalone with no module dependencies

2008 JUL 20

There are two difference between the trunk version of Viskit and the OA3302
branch.  Namely, the OA3302 branch is a simplified verion of the trunk.  The 
differences are in three source files and are annotated like this:

/* DIFF between OA3302 branch and trunk */
code that is commented out for OA3302 branch
/* End DIFF between OA3302 branch and trunk */

The affected source files are:
viskit.EventGraphAssemblyComboMainFrame
viskit.InternalAssemblyRunner
viskit.RunnerPanel2

A search on the above comments will expose where in the source these are.
