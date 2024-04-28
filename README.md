# POD-TPE1
A gRPC-based project simulating an airport check-in counter allocation system

This system has 5 services:

* Airport Administration Service
* Counter Reservation Service
* Passenger Check-in Service
* Airline Notification Service
* Counter Query Service

## Installation

Clone the project and execute the following command:
```
mvn clean install
```

## Use


Then we can run the clients in the following ways:

### Airport Administration Service
```
    ./adminClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
[ -Dsector=sectorName | -Dcounters=counterCount | -DinPath=manifestPath ]
```

### Counter Reservation Service
```
    ./counterClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    [ -Dsector=sectorName | -DcounterFrom=fromVal | -DcounterTo=toVal | 
    -Dflights=flights | -Dairline=airlineName | -DcounterCount=countVal ]
```

### Passenger Check-in Service
```
    ./passengerClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    [ -Dbooking=booking | -Dsector=sectorName | -Dcounter=counterNumber ]
```

### Airline Notification Service
```
    ./eventsClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    -Dairline=airlineName
```

### Counter Query Service
```
    ./queryClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    -DoutPath=query.txt [ -Dsector=sectorName | -Dairline=airlineName ]
```