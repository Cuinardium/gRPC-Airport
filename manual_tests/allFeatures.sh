#!/bin/bash


# CounterClient FAIL 1:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listSectors"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listSectors
echo


# AdminClient OK:
echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=C"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=C
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=A"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=A
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=3"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=3
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=7"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=7
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=A -Dcounters=5"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=A -Dcounters=5
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=manifest -DinPath=./prueba.csv"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=manifest -DinPath=./prueba.csv
echo


# AdminClient Fail:
echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=C"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=C
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=A"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addSector -Dsector=A
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=Z -Dcounters=3"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=Z -Dcounters=3
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=-3"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=-3
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=manifest -DinPath=./falla.csv"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=manifest -DinPath=./falla.csv
echo


# CounterClient OK 1:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listSectors"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listSectors
echo

# Vacio
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=50 -DcounterTo=100"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=50 -DcounterTo=100
echo


# CounterClient FAIL 2:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=Z -DcounterFrom=2 -DcounterTo=5"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=Z -DcounterFrom=2 -DcounterTo=5
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=5 -DcounterTo=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=5 -DcounterTo=2
echo


# CounterClient OK 2:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AC987' -Dairline=AirCanada -DcounterCount=100"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AC987' -Dairline=AirCanada -DcounterCount=100
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=100"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=100
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=1 -DcounterTo=116"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listCounters -Dsector=C -DcounterFrom=1 -DcounterTo=116
echo


# CounterClient FAIL 3:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=Z -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=Z -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123|AA124|AA125' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123|AA124|AA125' -Dairline=AmericanAirlines -DcounterCount=2
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AC987|AA123|BB134' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AC987|AA123|BB134' -Dairline=AmericanAirlines -DcounterCount=2
echo


# CounterClient OK 3:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=C -DcounterFrom=1 -Dairline=AmericanAirlines"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=C -DcounterFrom=1 -Dairline=AmericanAirlines
echo


# CounterClient FAIL 4:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=Z -DcounterFrom=1 -Dairline=AmericanAirlines"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=Z -DcounterFrom=1 -Dairline=AmericanAirlines
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=Z -DcounterFrom=1 -Dairline=AmericanAirlines"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=freeCounters -Dsector=C -DcounterFrom=16 -Dairline=AmericanAirlines
echo


#CounterClient OK 4:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=16 -Dairline=AirCanada"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=16 -Dairline=AirCanada
echo

#PassengerClient OK 1:
echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ABC123 -Dsector=C -Dcounter=16"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ABC123 -Dsector=C -Dcounter=16 
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=3"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=3
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='AA123' -Dairline=AmericanAirlines -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB134' -Dairline=PepeAirlines -DcounterCount=3
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=DFG123 -Dsector=C -Dcounter=1"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=DFG123 -Dsector=C -Dcounter=1 
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=1 -Dairline=PepeAirlines"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=1 -Dairline=PepeAirlines
echo



echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1341' -Dairline=PepeAirlines2 -DcounterCount=1000"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1341' -Dairline=PepeAirlines2 -DcounterCount=1000
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1342' -Dairline=PepeAirlines3 -DcounterCount=1000"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1342' -Dairline=PepeAirlines3 -DcounterCount=1000
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1343' -Dairline=PepeAirlines3 -DcounterCount=3"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1343' -Dairline=PepeAirlines4 -DcounterCount=3
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1344' -Dairline=PepeAirlines5 -DcounterCount=3"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=C -Dflights='BB1344' -Dairline=PepeAirlines5 -DcounterCount=333
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C
echo

echo "Running: sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=333"
sh adminClient.sh -DserverAddress=localhost:50051 -Daction=addCounters -Dsector=C -Dcounters=333
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=listPendingAssignments -Dsector=C
echo


#Passenger Client OK 1:

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116 
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=tt -Dsector=C -Dcounter=116 
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=fetchCounter -Dbooking=ll"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=fetchCounter -Dbooking=ll
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=116 -Dairline=PepeAirlines5"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=C -DcounterFrom=116 -Dairline=PepeAirlines5
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=ll"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=ll
echo


#Passenger Client Fail 1
echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=fetchCounter -Dbooking=BB1344"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=fetchCounter -Dbooking=kk
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=sdjg -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=sdjg -Dsector=C -Dcounter=116 
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=nn -Dsector=Z -Dcounter=116 
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=nn -Dsector=C -Dcounter=118 
echo


#Passenger Client OK 2:
echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=ll -Dsector=C -Dcounter=116"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=nn -Dsector=C -Dcounter=116 
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=nn"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=nn
echo

echo "Running: sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=zz"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerStatus -Dbooking=zz
echo


#QueryClient OK 1:
echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=A -Dflights='fA' -Dairline=aA -DcounterCount=2"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=assignCounters -Dsector=A -Dflights='fA' -Dairline=aA -DcounterCount=2
echo

echo "sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=bA -Dsector=A -Dcounter=11"
sh passengerClient.sh -DserverAddress=localhost:50051 -Daction=passengerCheckin -Dbooking=bA -Dsector=A -Dcounter=11 
echo

echo "Running: sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=A -DcounterFrom=11 -Dairline=aA"
sh counterClient.sh -DserverAddress=localhost:50051 -Daction=checkinCounters -Dsector=A -DcounterFrom=11 -Dairline=aA
echo

echo "Running: sh queryClient.sh -DserverAddress=localhost:50051 -Daction=queryCounters -DoutPath=./query1a.txt"
sh queryClient.sh -DserverAddress=localhost:50051 -Daction=queryCounters -DoutPath=./query1a.txt
echo

echo "Running: sh queryClient.sh -DserverAddress=localhost:50051 -Daction=queryCounters -DoutPath=./query1b.txt -Dsector=C"
sh queryClient.sh -DserverAddress=localhost:50051 -Daction=queryCounters -DoutPath=./query1b.txt -Dsector=C
echo

echo "Running: sh queryClient.sh -DserverAddress=localhost:50051 -Daction=checkins -DoutPath=./query2a.txt"
sh queryClient.sh -DserverAddress=localhost:50051 -Daction=checkins -DoutPath=./query2a.txt
echo

echo "Running: sh queryClient.sh -DserverAddress=localhost:50051 -Daction=checkins -DoutPath=./query2b.txt"
sh queryClient.sh -DserverAddress=localhost:50051 -Daction=checkins -DoutPath=./query2b.txt -Dsector=A
echo

