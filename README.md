# POD-TPE1
A gRPC-based project simulating an airport check-in counter allocation system

This system has 5 services:

## Airport Administration Service
* <u>Functionality:</u> Adds sectors, adds counters and updates the list of airlines and passengers waiting in the airport.
* <u>User:</u> Airport Administrator Company.
* <u>Client:</u> The information about which action to perform is received through command line arguments when calling the airport administration client script **adminClient.sh** and the result must be printed on screen.

```$> sh adminClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName [ -Dsector=sectorName | -Dcounters=counterCount | -DinPath=manifestPath ]```

where
* <u>xx.xx.xx.xx:yyyy</u> is the IP address and port where the airport administration service is published
* <u>actionName</u> it the name of the action to perform

### Add sector
* <u>Functionality:</u> Adds a sector from a name <u>sectorName</u>. The sector initializes without counters.
* <u>Fails if:</u>
  * Sector name already exists
* <u>Example:</u> Add sector "C" to the airport
``` 
$> sh adminClient.sh -DserverAddress=10.6.0.1:50051 -Daction=addSector -Dsector=C
   
   Sector C added successfully
```

### Add range of counters
* <u>Functionality:</u> Adds a range of continuous counters in a sector from the name of the sector <u>sectorName</u> and the number of counters <u>counterCount</u> to add. The counters are initialized without assignments. The numbering of counters is incremental and shared between all sectors of the airport where the first has the number 1. Therefore, in a sector, the numbering of counters may contain gaps.
* <u>Fails if:</u>
  * Sector with indicated name doesn´t exists
  * The number of counters indicated isn´t positive
* <u>Example:</u> Add 3 counters to sector "C" of the airport after having added a counter in another sector
``` 
$> sh adminClient.sh -DserverAddress=10.6.0.1:50051 -Daction=addCounters -Dsector=C -Dcounters=3
   
   3 new counters (2-4) in Sector C added successfully
```

### Add waiting passenger
* <u>Functionality:</u> Adds a waiting passenger that must board at the airport based on the booking code, flight code and name of the airline.
* <u>Fails if:</u>
  * Passenger was already added with that booking code
  * Flight was already added that flight code but in another airline
* <u>Example:</u> Add a batch of passenger from a <u>manifestPath</u> CSV file with the following structure:
  * booking: booking code (6 alphanumeric characters)
  * flight: flight code
  * airline: name of the airline
  
  where each line represents a passenger waiting at the airport
``` 
$> sh adminClient.sh -DserverAddress=10.6.0.1:50051 -Daction=manifest -DinPath=../manifest.csv
   
   Booking ABC123 for AirCanada AC987 added successfully
   ...

```

## Counter Booking Service
* <u>Functionality:</u> Consult sectors, counters and request and free a range of counters to then do check-ins for airline flights.
* <u>User:</u> Airline that has one or more flights departing from the airport.
* <u>Client:</u> The information about which action to perform is received through command line arguments when calling the counter booking client script **counterClient.sh** and the result must be printed on screen.

```
$> sh counterClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName [ -Dsector=sectorName | -DcounterFrom=fromVal | -DcounterTo=toVal | -Dflights=flights | -Dairline=airlineName | -DcounterCount=countVal ]
```

where
* <u>xx.xx.xx.xx:yyyy</u> is the IP address and port where the airport administration service is published
* <u>actionName</u> it the name of the action to perform

### Consult sectors
* <u>Functionality:</u> Consults sectors and ranges of counters located in each one of them, in alphabetical order by sector
* <u>Fails if:</u>
  * Sectors don´t exist in the airport
* <u>Example:</u> Consult the sectors, where sector A has 1 counter, sector C has 5 counters, sector D has 2 counters and sector Z has no counters
``` 
$> sh counterClient.sh -DserverAddress=10.6.0.1:50051 -Daction=listSectors
   Sectors   Counters
   ###################
   A         (1-1)
   C         (2-4)(7-8)
   D         (5-6)
   Z         -
```

### Consult range of counters
* <u>Functionality:</u> Consult a range of counters [<u>fromVal</u>, <u>toVal</u>] of a sector <u>sectorName</u>, in ascending order by counter, indicating whether the counter is free or if its assigned to an airline (in which case indicate the flight codes of the assignment and the quantity of people waiting to be served in line at the counter range)
* <u>Fails if:</u>
  * Sector with that name doesn´t exists.
  * <u>fromVal</u> and <u>toVal</u> do not form a range of one or more counters.
* <u>Example:</u> Consult counters “2” to “5” inclusive of sector “C” indicating that counters 2 and 3 are being used by American Airlines to serve passengers on flights AA123, AA124 and AA125 with 6 people in the range queue waiting to be attended, that counter 4 is free and that counter 5 does not exist in the sector.
``` 
$> sh counterClient.sh -DserverAddress=10.6.0.1:50051 -Daction=listCounters -Dsector=C -DcounterFrom=2 -DcounterTo=5 
   Counters  Airline          Flights             People
   ##########################################################
   (2-3)     AmericanAirlines AA123|AA124|AA125   6
   (4-4)     -                -                   -
```

In case the range of counters is empty or if the sector has no counters

``` 
$> sh counterClient.sh -DserverAddress=10.6.0.1:50051 -Daction=listCounters -Dsector=C -DcounterFrom=10 -DcounterTo=20 
   Counters  Airline          Flights             People
   ##########################################################
```

### Assign a range of counters
* <u>Functionality:</u> Assign a range of <u>countVal</u> to adjacent counters located in the <u>sectorName</u> sector so that passengers can check-in for flights of the airline <u>airlineName</u>. For the <u>deterministic assignment algorithm</u>, the following criteria must be respected:
  * Priority: Consider counters with lower identifiers first
  * Contiguity: If, for example, in a sector there are 5 counters from 1 to 5 where counters 2 and 3 are in use, if you want to request a range of 3 counters it is not possible to assign it because although counters 1, 4 and 5 are available these are not contiguous.
  * <u>Pending Assignment:</u> If you cannot make the assignment, you must store the assignment in a pending state.
* <u>Fails if:</u>
  * 
* <u>Example:</u> 
``` 

```