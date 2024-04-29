# POD-TPE1
A gRPC-based project simulating an airport check-in counter allocation system

This system has 5 services:

* Airport Administration Service
* Counter Reservation Service
* Passenger Check-in Service
* Airline Notification Service
* Counter Query Service


It is required for this project to have <u>java 17</u> installed as JDK.

## Installation

Clone the project and execute the following command:
```
mvn clean install
```

In the directories server/target and client/target there will be .tar.gz extension files. You will have to decompress them.
You can do it with the following commands:
```
tar -xvf server/target/tpe1-g4-server-1.0-SNAPSHOT-bin.tar.gz
tar -xvf client/target/tpe1-g4-client-1.0-SNAPSHOT-bin.tar.gz
```

After that, you will need to give execution permissions to the scripts inside the directories created from the compressed files

For the Server:
```
cd tpe1-g4-server-1.0-SNAPSHOT
chmod u+x ./run-server.sh
```

For the Clients:
```
cd tpe1-g4-client-1.0-SNAPSHOT
chmod u+x ./adminClient.sh
chmod u+x ./counterClient.sh
chmod u+x ./eventsClient.sh
chmod u+x ./passengerClient.sh
chmod u+x ./queryClient.sh
```

## Use

Now everything is ready to use

### Server

First we have to enter the folder tpe1-g4-server-1.0-SNAPSHOT

Then we can run the server by executing the following command:
```
    ./run-server.sh
```

It is important to know that when the server is running, it is on the port 50051

### Clients

First we have to enter the folder tpe1-g4-client-1.0-SNAPSHOT

Then we can run the clients executing the following commands:

#### Airport Administration Service
```
    ./adminClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
[ -Dsector=sectorName | -Dcounters=counterCount | -DinPath=manifestPath ]
```

#### Counter Reservation Service
```
    ./counterClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    [ -Dsector=sectorName | -DcounterFrom=fromVal | -DcounterTo=toVal | 
    -Dflights=flights | -Dairline=airlineName | -DcounterCount=countVal ]
```

#### Passenger Check-in Service
```
    ./passengerClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    [ -Dbooking=booking | -Dsector=sectorName | -Dcounter=counterNumber ]
```

#### Airline Notification Service
```
    ./eventsClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    -Dairline=airlineName
```

#### Counter Query Service
```
    ./queryClient.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName 
    -DoutPath=query.txt [ -Dsector=sectorName | -Dairline=airlineName ]
```

## Manual Tests

We also provided a script for testing the overall functionality of the project. 

To run this test, the server must be running. Then execute the following command:
```
    ./script.sh
```