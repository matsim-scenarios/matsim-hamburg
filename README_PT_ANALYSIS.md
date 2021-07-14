# Analysing real world and simulation data

## CompareSimulationRealWorld ---> main method call

passing the following parameters as arguments
* transitScheduleFile
* eventsFile
* realWorldCountsDirectory
* outputResultsDirectory
* outputMissingStationsDirectory
* scalingFactor

---

## Read the real world data using the following method
readRealWorldData -- pass the directory location as the parameter.
The return value is a LinkedHashMap --> key is the Line No: and value is another LinkedHashMap (Line No: is obtained from the first row)
The value LinkedHashMap has a key of stations and value of PersonCounts object
PersonCounts --> this object contains the count of Einsteiger and Aussteiger in both directions for each station (Outbound and Inbound),
Outbound --> travelling away from the first station (first leg of a return journey)
Inbound --> travelling towards the first station from last station (so after completing first leg of a return journey)

---

## Map line number to year -- > mapLineNoYear
mapLineNoYear(String directoryToScanForRuns)
Reads the file name and takes line no: and year
Method returns a LinkedHashMap<String, String>
Key is line number and value is year

---

## Read transitschedule file
readTransitSchedule(String transitScheduleFile)
Return object is HashMap<String, MyTransitObject> , key is line no: and value is MyTransitObject
line no: is obtained from the transitLine id (eg: <transitLine id="10---1281_3">, here 10 is the line no:)
Each line no: has a MyTransitObject which contains all the transit route stations (station ID and name) and the list of vehicles.

---

## Next step is to read the simulation file.
Method ----> readSimulationData
MyEventHandler tracks all the events
From this class finall we get HashMap<Id<Person>, MyPerson>
MyPerson object contains the list of transit usages of a particular person, basically a single transit contains start station, end station and vehicle Id

---

## Map vehicle id to Line no:
Next step is mapping vehicle id to Line no: (a sigle vehicle used only in a single line) ---> mapVehicleIdToLineNo
Here we compare the vehicle id of the transit usage of persons with transit schedule data

---

## Iterate simulation results (HashMap<Id<Person>, MyPerson>)
* Iterating each person and taking there transit usage list to iterate again
* Mapping the vehicle Id with line No: using the results from the mapVehicleIdToLineNo method
* Using this line no: take transit from real world data (readRealWorldData)
* simulationLinesMatchingRealWorld --> Keep track of simulation lines matching with real world lines (If real world station contains both start station and end station of    simulation)
* missingStationLines --> make a list of missing station lines (if either start station or end station or both (from simulation) is missing in realworld)
* Some of the simulation stations might not available in real world and vice versa, keeping track of them is important
* allSimulationStations --> is a list of all simulation lines and corresponding list of stops regardless of its availability in real world
* Compare start station and end station of the person with the index of real world stations, based on the index deciding in which direction person is travelling (inbound or outbound) and incrementing person counts by setEinsteigerOutboundSim() and setAussteigerOutboundSim() or setEinsteigerInboundSim() and setAussteigerInboundSim() in PersonCounts object

* If either of start station or end station is missing in real world it is tracked using setEinsteigerSim() and setAussteigerSim() respectively in PersonCounts object
* If both of start station and end station is missing in real world it is tracked using MissingStationPersonCounts object and added in 'HashMap<String, LinkedHashMap<String, MissingStationPersonCounts>> missingStation' where key is line no: and LinkedHashMap<String, MissingStationPersonCounts> key is station


*Note: In many cases real world and simulations has same stations but the string is not exactly same eg: Parkstraße/Elbchaussee (simulation), Parkstraße / Elbchaussee (real world), here there is a space after the name in real world but not in sim, such many scenarios are there which cannot be handled here, such station are considered non matching


In the end print results files,
* File 1: ** missingStations.csv **
This file contains lines which has stations not matching with real world

* File 2: ** results.csv **
This file cotains lines which has stations exactly matching with real world

---

## Charts are drawn with the same data used for generating csv files.
Following charts are generated for all lines (in results.csv)
* PercentageChangepercentageChangeEinsteigerInbound
* PercentageChangepercentageChangeEinsteigerOutbound
* TotalPtUsageBarChart
* transitAussteigerRealWorld
* transitAussteigerSim
* transitEinsteigerRealWorld
* transitEinsteigerSim

 



