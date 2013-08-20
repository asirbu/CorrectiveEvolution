CorrectiveEvolution
===================

Evolves process models to include corrections, at the same time ensuring that the resulting process models
continue to achieve the goal of the original process model. 
Can generate corrections based on process execution logs.

More details about the approach and the theory behind this implementation can be found at:
http://www.adinasirbu.net/corrective-evolution

-------------
Dependencies:
-------------

The following tools are required to run CorrectiveEvolution.
Detailed installation instructions for these tools are available on the official websites: 

- NuSMV - http://nusmv.fbk.eu/NuSMV/download/getting-v2.html (version used is 2.5.4)
- mCRL2 - http://www.mcrl2.org/release/user_manual/download.html (version used is the January 2009 release)
- Graphviz - http://www.graphviz.org/Download.php (version used is 2.28)


---------------------------
Configuration instructions:
---------------------------

Change the paths to the NuSMV, mCLR2, and Graphviz installations in the config.properties configuration file.

The config.properties file contains also an 'intermediateFiles' parameter. If this parameter is set to true, 
the various steps performed by the tool (encoding all the inputs into a '.smv' file, the output from NuSMV, 
respectively WSynth, the minimized STS) will all be recorded as separate files.


----------------------
Building instructions:
----------------------

You will find the Ant build file "build.xml" in the root directory. 
If you don't already have Ant installed, you can download it from http://ant.apache.org/

After running the main target, a file "correctiveEvolution.jar" will be created and placed in the
root directory. 


---------------------
Running instructions:
---------------------

The prototype contains three different scenarios: 
* a car logistics scenario (in folder 'carLogistics'). The process model concerns the handling of a car
as it arrives from the manufacturer at a sea port, and until it is delivered to the retailer. 
In this scenario there are two corrections  
which are fixed and provided as input. These corrections are alternatively set to "strict" or "relaxed",
and should determine four different evolved process models.
* a scenario from the financial domain (in folder 'financial'), designed based on a real event log.
The process model represents a loan or overdraft application process. 
In this scenario, the corrections are not fixed, they are generated based on the event log. 
* a basic scenario. This scenario is a simplified car logistics scenario, 
designed for testing purposes.

To test each of the three scenarios, set the scenario parameter to "carLogistics", 
"financial" (default), or "basic" as in the following:

	java -jar correctiveEvolution.jar -scenario=carLogistics

For the financial scenario, you can also select which type of corrections should be used by 
setting the value "relaxed" (default) or "strict" to the type parameter:

	java -jar correctiveEvolution.jar -scenario=financial -type=strict

The event log for the financial scenario is included in the 'financial/eventLogs' folder, 
and is a fragment of the complete log provided
in the 2012 BPI Challenge (roughly one sixth). The log fragment can also be substituted with the complete log
available here:
http://www.win.tue.nl/bpi2013/doku.php?id=challenge
