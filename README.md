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

---------------------
Running instructions:
---------------------

The prototype contains two different scenarios: 
* a car logistics scenario (in folder 'carLogistics'). The process model concerns the handling of a car
as it arrives from the manufacturer at a sea port, and until it is delivered to the retailer.
* a scenario from the financial domain (in folder 'financial'), designed based on a real event log.
The process model represents a loan or overdraft application process.  

The first scenario can be tested by running eu.fbk.soa.evolution.CarLogisticsScenarioTest (in the
'functional-tests' folder). In this scenario, there are two
corrections, which are fixed and provided as input.

The second scenario can be tested by running eu.fbk.soa.evolution.FinancialScenarioTest (same 
'functional-tests' folder). In this scenario, the corrections are generated based on the event log. 

The event log is included in the 'financial/eventLogs' folder, and is a fragment of the complete log provided
in the 2012 BPI Challenge (roughly one sixth). The log fragment can also be substituted with the complete log
available here:
http://www.win.tue.nl/bpi2013/doku.php?id=challenge
