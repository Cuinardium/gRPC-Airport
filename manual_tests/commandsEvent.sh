
# Add passengers
./adminClient.sh -DserverAddress=localhost:50051 -Daction=manifest -DinPath=manifest.csv

# An Airline should register while sleeping
sleep 10

# Add sectors
./adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=c 

# Add counters
./adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=c -Dcounters=3
./adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=c -Dcounters=3

# Assign counters
./counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=c -DcounterCount=6 -Dairline=AmericanAirlines -Dflights=AC987

# Assign counters (goes to queue)
./counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=c -DcounterCount=6 -Dairline=AmericanAirlines -Dflights=AA123

# Passengers Arrive
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC123
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC124
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC125
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC126
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC127
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC128
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC129
./passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dsector=c -Dcounter=1 -Dbooking=ABC130

# Checkin Counters
./counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=c -DcounterFrom=1 -Dairline=AmericanAirlines
./counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=c -DcounterFrom=1 -Dairline=AmericanAirlines

# Free Counters (moves queued to assigned)
./counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=c -DcounterFrom=1 -Dairline=AmericanAirlines

# Assign Counters (goes to queue)
./counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=c -DcounterCount=6 -Dairline=AmericanAirlines -Dflights=AA124

# Add Counters (moves queued to assigned)
./adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=c -Dcounters=6

# Unregister Airline
./eventsClient.sh -DserverAddress=localhost:50051 -Daction=unregister -Dairline=AmericanAirlines
