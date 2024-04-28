package ar.edu.itba.pod.server.repositories.counterRepository;

import ar.edu.itba.pod.server.exceptions.*;
import ar.edu.itba.pod.server.models.Assignment;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
import ar.edu.itba.pod.server.repositories.CounterRepository;
import ar.edu.itba.pod.server.utils.Pair;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class CounterRepositoryTest<T extends CounterRepository> {

    private T counterRepository;

    abstract T createCounterRepository();

    @BeforeEach
    public void setUp() {
        counterRepository = createCounterRepository();
    }

    // ---- Sectors
    @Test
    public void testAddSector() throws AlreadyExistsException {
        counterRepository.addSector("D");
        counterRepository.addSector("C");

        Assertions.assertTrue(counterRepository.hasSector("D"));
        Assertions.assertTrue(counterRepository.hasSector("C"));

        List<Sector> sectors = counterRepository.getSectors();

        Assertions.assertEquals(2, sectors.size());

        // Sectors are sorted
        Assertions.assertEquals("C", sectors.get(0).sectorName());
        Assertions.assertEquals("D", sectors.get(1).sectorName());

        Optional<Sector> sector = counterRepository.getSector("D");
        Assertions.assertTrue(sector.isPresent());
        Assertions.assertEquals("D", sector.get().sectorName());

        sector = counterRepository.getSector("C");
        Assertions.assertTrue(sector.isPresent());
        Assertions.assertEquals("C", sector.get().sectorName());

        sector = counterRepository.getSector("A");

        Assertions.assertFalse(sector.isPresent());
    }

    @Test
    public void testAddSectorAlreadyExists() throws AlreadyExistsException {
        counterRepository.addSector("D");

        Assertions.assertThrows(
                AlreadyExistsException.class, () -> counterRepository.addSector("D"));
    }

    // ---- Counters

    @Test
    public void testAddCounters() throws AlreadyExistsException {
        counterRepository.addSector("D");
        counterRepository.addSector("C");

        counterRepository.addCounters("D", 5);
        counterRepository.addCounters("C", 3);

        Assertions.assertTrue(counterRepository.hasCounters());

        Sector sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().size());
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(5, sector.countersRangeList().get(0).range().to());

        sector = counterRepository.getSector("C").get();
        Assertions.assertEquals(1, sector.countersRangeList().size());
        Assertions.assertEquals(6, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(8, sector.countersRangeList().get(0).range().to());

        counterRepository.addCounters("D", 3);
        sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(2, sector.countersRangeList().size());
        Assertions.assertEquals(9, sector.countersRangeList().get(1).range().from());
        Assertions.assertEquals(11, sector.countersRangeList().get(1).range().to());

        // Check that the counters are merged
        counterRepository.addCounters("D", 2);
        sector = counterRepository.getSector("D").get();

        Assertions.assertEquals(2, sector.countersRangeList().size());
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(5, sector.countersRangeList().get(0).range().to());
        Assertions.assertEquals(9, sector.countersRangeList().get(1).range().from());
        Assertions.assertEquals(13, sector.countersRangeList().get(1).range().to());
    }

    @Test
    public void testAddCountersNoSector() {
        Assertions.assertThrows(
                NoSuchElementException.class, () -> counterRepository.addCounters("D", 5));
    }

    // ---- Assignments
    @Test
    public void testAssignCounterAssignment()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException {
        counterRepository.addSector("D");
        counterRepository.addSector("C");

        counterRepository.addCounters("D", 5);
        counterRepository.addCounters("C", 3);
        counterRepository.addCounters("D", 3);

        // D has counters from 1 to 5 and from 9 to 11
        // C has counters from 6 to 8

        Pair<Range, Integer> result;

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        result = counterRepository.assignCounterAssignment("D", assignmentD1);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(1, result.first().from());
        Assertions.assertEquals(2, result.first().to());

        // Test getFlightCounters and getFlightCountersAndSector
        for (String flight : flights) {
            Optional<CountersRange> countersRange = counterRepository.getFlightCounters(flight);
            Assertions.assertTrue(countersRange.isPresent());
            Assertions.assertEquals(1, countersRange.get().range().from());
            Assertions.assertEquals(2, countersRange.get().range().to());

            Optional<Pair<CountersRange, String>> countersRangeAndSector =
                    counterRepository.getFlightCountersAndSector(flight);
            Assertions.assertTrue(countersRangeAndSector.isPresent());
            Assertions.assertEquals(1, countersRangeAndSector.get().first().range().from());
            Assertions.assertEquals(2, countersRangeAndSector.get().first().range().to());
            Assertions.assertEquals("D", countersRangeAndSector.get().second());
        }

        // Should assign counters from 6 to 7
        flights = List.of("AA125", "AA126");
        Assignment assignmentC1 = new Assignment("AmericanAirlines", flights, 2);

        result = counterRepository.assignCounterAssignment("C", assignmentC1);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(6, result.first().from());
        Assertions.assertEquals(7, result.first().to());

        // Test getFlightCounters and getFlightCountersAndSector
        for (String flight : flights) {
            Optional<CountersRange> countersRange = counterRepository.getFlightCounters(flight);
            Assertions.assertTrue(countersRange.isPresent());
            Assertions.assertEquals(6, countersRange.get().range().from());
            Assertions.assertEquals(7, countersRange.get().range().to());

            Optional<Pair<CountersRange, String>> countersRangeAndSector =
                    counterRepository.getFlightCountersAndSector(flight);
            Assertions.assertTrue(countersRangeAndSector.isPresent());
            Assertions.assertEquals(6, countersRangeAndSector.get().first().range().from());
            Assertions.assertEquals(7, countersRangeAndSector.get().first().range().to());
            Assertions.assertEquals("C", countersRangeAndSector.get().second());
        }

        // Should assign counters from 3 to 5
        Assignment assignmentD2 = new Assignment("AmericanAirlines", List.of("AA127"), 3);
        result = counterRepository.assignCounterAssignment("D", assignmentD2);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(3, result.first().from());
        Assertions.assertEquals(5, result.first().to());

        // Test getFlightCounters and getFlightCountersAndSector
        Optional<CountersRange> countersRange = counterRepository.getFlightCounters("AA127");
        Assertions.assertTrue(countersRange.isPresent());
        Assertions.assertEquals(3, countersRange.get().range().from());
        Assertions.assertEquals(5, countersRange.get().range().to());

        Optional<Pair<CountersRange, String>> countersRangeAndSector =
                counterRepository.getFlightCountersAndSector("AA127");
        Assertions.assertTrue(countersRangeAndSector.isPresent());
        Assertions.assertEquals(3, countersRangeAndSector.get().first().range().from());
        Assertions.assertEquals(5, countersRangeAndSector.get().first().range().to());
        Assertions.assertEquals("D", countersRangeAndSector.get().second());

        // Should assign counters from 8 to 8
        Assignment assignmentC2 = new Assignment("AmericanAirlines", List.of("AA128"), 1);
        result = counterRepository.assignCounterAssignment("C", assignmentC2);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(8, result.first().from());
        Assertions.assertEquals(8, result.first().to());

        // Test getFlightCounters and getFlightCountersAndSector
        countersRange = counterRepository.getFlightCounters("AA128");
        Assertions.assertTrue(countersRange.isPresent());
        Assertions.assertEquals(8, countersRange.get().range().from());
        Assertions.assertEquals(8, countersRange.get().range().to());

        countersRangeAndSector = counterRepository.getFlightCountersAndSector("AA128");
        Assertions.assertTrue(countersRangeAndSector.isPresent());
        Assertions.assertEquals(8, countersRangeAndSector.get().first().range().from());
        Assertions.assertEquals(8, countersRangeAndSector.get().first().range().to());
        Assertions.assertEquals("C", countersRangeAndSector.get().second());

        // Should assign counters from 9 to 11
        Assignment assignmentD3 = new Assignment("AmericanAirlines", List.of("AA129"), 3);
        result = counterRepository.assignCounterAssignment("D", assignmentD3);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(9, result.first().from());
        Assertions.assertEquals(11, result.first().to());

        // Test getFlightCounters and getFlightCountersAndSector
        countersRange = counterRepository.getFlightCounters("AA129");
        Assertions.assertTrue(countersRange.isPresent());
        Assertions.assertEquals(9, countersRange.get().range().from());
        Assertions.assertEquals(11, countersRange.get().range().to());

        countersRangeAndSector = counterRepository.getFlightCountersAndSector("AA129");
        Assertions.assertTrue(countersRangeAndSector.isPresent());
        Assertions.assertEquals(9, countersRangeAndSector.get().first().range().from());
        Assertions.assertEquals(11, countersRangeAndSector.get().first().range().to());
        Assertions.assertEquals("D", countersRangeAndSector.get().second());

        // Validate getSectors
        List<Sector> sectors = counterRepository.getSectors();

        Assertions.assertEquals(2, sectors.size());

        Sector sectorC = sectors.get(0);
        Assertions.assertEquals("C", sectorC.sectorName());
        Assertions.assertEquals(2, sectorC.countersRangeList().size());

        Assertions.assertEquals(6, sectorC.countersRangeList().get(0).range().from());
        Assertions.assertEquals(7, sectorC.countersRangeList().get(0).range().to());

        Assertions.assertTrue(sectorC.countersRangeList().get(0).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sectorC.countersRangeList().get(0).assignedInfo().get().airline());
        Assertions.assertEquals(
                List.of("AA125", "AA126"),
                sectorC.countersRangeList().get(0).assignedInfo().get().flights());

        Assertions.assertEquals(8, sectorC.countersRangeList().get(1).range().from());
        Assertions.assertEquals(8, sectorC.countersRangeList().get(1).range().to());

        Assertions.assertTrue(sectorC.countersRangeList().get(1).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sectorC.countersRangeList().get(1).assignedInfo().get().airline());
        Assertions.assertEquals(
                List.of("AA128"),
                sectorC.countersRangeList().get(1).assignedInfo().get().flights());

        Sector sectorD = sectors.get(1);
        Assertions.assertEquals("D", sectorD.sectorName());

        Assertions.assertEquals(3, sectorD.countersRangeList().size());

        Assertions.assertEquals(1, sectorD.countersRangeList().get(0).range().from());
        Assertions.assertEquals(2, sectorD.countersRangeList().get(0).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(0).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sectorD.countersRangeList().get(0).assignedInfo().get().airline());
        Assertions.assertEquals(
                List.of("AA123", "AA124"),
                sectorD.countersRangeList().get(0).assignedInfo().get().flights());

        Assertions.assertEquals(3, sectorD.countersRangeList().get(1).range().from());
        Assertions.assertEquals(5, sectorD.countersRangeList().get(1).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(1).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sectorD.countersRangeList().get(1).assignedInfo().get().airline());
        Assertions.assertEquals(
                List.of("AA127"),
                sectorD.countersRangeList().get(1).assignedInfo().get().flights());

        Assertions.assertEquals(9, sectorD.countersRangeList().get(2).range().from());
        Assertions.assertEquals(11, sectorD.countersRangeList().get(2).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(2).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sectorD.countersRangeList().get(2).assignedInfo().get().airline());
        Assertions.assertEquals(
                List.of("AA129"),
                sectorD.countersRangeList().get(2).assignedInfo().get().flights());
    }

    @Test
    public void testAssignCounterAssignmentFlightAlreadyAssigned()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 5);

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        // Should assign counters from 3 to 5
        Assignment assignmentD2 = new Assignment("AmericanAirlines", List.of("AA127"), 3);
        counterRepository.assignCounterAssignment("D", assignmentD2);

        // Both flights are already assigned
        Assertions.assertThrows(
                FlightAlreadyAssignedException.class,
                () -> counterRepository.assignCounterAssignment("D", assignmentD1));
        // One of the flights is already assigned other is not
        Assertions.assertThrows(
                FlightAlreadyAssignedException.class,
                () ->
                        counterRepository.assignCounterAssignment(
                                "D",
                                new Assignment("AmericanAirlines", List.of("AA123", "AA128"), 2)));
    }

    @Test
    public void testAssignCountersAddToAssignmentQueue()
            throws FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    AlreadyExistsException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 5);

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        // Should assign counters from 3 to 5
        Assignment assignmentD2 = new Assignment("AmericanAirlines", List.of("AA127"), 3);
        counterRepository.assignCounterAssignment("D", assignmentD2);

        // Should add to the queue
        Assignment assignmentD3 = new Assignment("AmericanAirlines", List.of("AA128"), 2);
        Pair<Range, Integer> result = counterRepository.assignCounterAssignment("D", assignmentD3);

        // Validate the result
        Assertions.assertEquals(0, result.second());
        Assertions.assertNull(result.first());

        // Should add to the queue
        Assignment assignmentD4 = new Assignment("AmericanAirlines", List.of("AA129"), 3);
        result = counterRepository.assignCounterAssignment("D", assignmentD4);

        // Validate the result
        Assertions.assertEquals(1, result.second());
        Assertions.assertNull(result.first());

        // Check the queue
        List<Assignment> queue = counterRepository.getQueuedAssignments("D").stream().toList();
        Assertions.assertEquals(2, queue.size());
        Assertions.assertEquals(assignmentD3, queue.get(0));
        Assertions.assertEquals(assignmentD4, queue.get(1));
    }

    @Test
    public void testAssignCounterAssignmentFlightAlreadyQueued()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        Pair<Range, Integer> result;

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        // Should add to the queue
        Assignment assignmentD2 = new Assignment("AmericanAirlines", List.of("AA127"), 2);
        result = counterRepository.assignCounterAssignment("D", assignmentD2);

        Assertions.assertEquals(0, result.second());
        Assertions.assertNull(result.first());

        // should fail because the flight is already queued
        Assertions.assertThrows(
                FlightAlreadyQueuedException.class,
                () -> counterRepository.assignCounterAssignment("D", assignmentD2));
    }

    @Test
    public void testAssignCounterAssignmentFlightAlreadyCheckedIn()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    HasPendingPassengersException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        Pair<Range, Integer> result;

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        result = counterRepository.assignCounterAssignment("D", assignmentD1);

        Assertions.assertEquals(0, result.second());
        Assertions.assertEquals(1, result.first().from());
        Assertions.assertEquals(2, result.first().to());

        // Free the counters
        counterRepository.freeCounters("D", 1, "AmericanAirlines");

        // Should fail because both flights are already checked in
        Assertions.assertThrows(
                FlightAlreadyCheckedInException.class,
                () -> counterRepository.assignCounterAssignment("D", assignmentD1));
        // Should fail because one of the flights is already checked in
        Assertions.assertThrows(
                FlightAlreadyCheckedInException.class,
                () ->
                        counterRepository.assignCounterAssignment(
                                "D",
                                new Assignment("AmericanAirlines", List.of("AA123", "AA127"), 2)));
    }

    @Test
    public void testFreeCountersSectorNotFound() {
        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> counterRepository.freeCounters("D", 1, "AmericanAirlines"));
    }

    @Test
    public void testFreeCountersCounterNotFound()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        Assertions.assertThrows(
                NoSuchElementException.class,
                () -> counterRepository.freeCounters("D", 2, "AmericanAirlines"));
    }

    @Test
    public void testFreeCountersHasPendingPassengers()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        // Add a passenger to the queue
        counterRepository.addPassengerToQueue(new Range(1, 2), "XYZ123");

        Assertions.assertThrows(
                HasPendingPassengersException.class,
                () -> counterRepository.freeCounters("D", 1, "AmericanAirlines"));
    }

    @Test
    public void testFreeCountersSuccessNoPendingAssignmentsMergesWithNext()
            throws AlreadyExistsException,
                    FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    HasPendingPassengersException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        // Should assign counters from 1 to 2
        List<String> flights = List.of("AA123", "AA124");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 2);
        counterRepository.assignCounterAssignment("D", assignmentD1);

        // Free the counters
        CountersRange result = counterRepository.freeCounters("D", 1, "AmericanAirlines");

        Assertions.assertEquals(1, result.range().from());
        Assertions.assertEquals(2, result.range().to());
        Assertions.assertTrue(result.assignedInfo().isPresent());
        Assertions.assertEquals("AmericanAirlines", result.assignedInfo().get().airline());
        Assertions.assertEquals(flights, result.assignedInfo().get().flights());
        Assertions.assertEquals(0, result.assignedInfo().get().passengersInQueue());

        // Check that the counters are free
        Optional<CountersRange> countersRange = counterRepository.getFlightCounters("AA123");
        Assertions.assertFalse(countersRange.isPresent());

        countersRange = counterRepository.getFlightCounters("AA124");
        Assertions.assertFalse(countersRange.isPresent());

        // Check that sector d has a range from 1 to 3
        Sector sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(0).range().to());
    }

    @Test
    public void testFreeCountersSuccessNoPendingAssignmentsMergesWithNextOrPrevious()
            throws FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    AlreadyExistsException,
                    HasPendingPassengersException {
        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        // Assign 1 to 1
        List<String> flights = List.of("AA123");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 1);

        // Assign 2 to 2
        List<String> flights2 = List.of("AA124");
        Assignment assignmentD2 = new Assignment("AmericanAirlines", flights2, 1);

        // Assign 3 to 3
        List<String> flights3 = List.of("AA125");
        Assignment assignmentD3 = new Assignment("AmericanAirlines", flights3, 1);

        counterRepository.assignCounterAssignment("D", assignmentD1);
        counterRepository.assignCounterAssignment("D", assignmentD2);
        counterRepository.assignCounterAssignment("D", assignmentD3);

        // Free 2 to 2
        CountersRange result = counterRepository.freeCounters("D", 2, "AmericanAirlines");

        Assertions.assertEquals(2, result.range().from());
        Assertions.assertEquals(2, result.range().to());

        // Check that counters are (1-1) assigned (2-2) free (3-3) assigned
        Sector sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().to());
        Assertions.assertTrue(sector.countersRangeList().get(0).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sector.countersRangeList().get(0).assignedInfo().get().airline());
        Assertions.assertEquals(
                flights, sector.countersRangeList().get(0).assignedInfo().get().flights());

        Assertions.assertEquals(2, sector.countersRangeList().get(1).range().from());
        Assertions.assertEquals(2, sector.countersRangeList().get(1).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(1).assignedInfo().isPresent());

        Assertions.assertEquals(3, sector.countersRangeList().get(2).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(2).range().to());
        Assertions.assertTrue(sector.countersRangeList().get(2).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sector.countersRangeList().get(2).assignedInfo().get().airline());
        Assertions.assertEquals(
                flights3, sector.countersRangeList().get(2).assignedInfo().get().flights());

        // Free 1 to 1
        result = counterRepository.freeCounters("D", 1, "AmericanAirlines");

        Assertions.assertEquals(1, result.range().from());
        Assertions.assertEquals(1, result.range().to());

        // Check that counters are (1-2) free (3-3) assigned
        sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(2, sector.countersRangeList().get(0).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(0).assignedInfo().isPresent());

        Assertions.assertEquals(3, sector.countersRangeList().get(1).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(1).range().to());
        Assertions.assertTrue(sector.countersRangeList().get(1).assignedInfo().isPresent());

        // Free 3 to 3
        result = counterRepository.freeCounters("D", 3, "AmericanAirlines");

        Assertions.assertEquals(3, result.range().from());
        Assertions.assertEquals(3, result.range().to());

        // Check that counters are (1-3) free
        sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(0).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(0).assignedInfo().isPresent());
    }

    @Test
    public void testFreeCountersSuccessNoPendingAssignmentsMergesWithNextAndPrevious()
            throws FlightAlreadyCheckedInException,
                    FlightAlreadyAssignedException,
                    FlightAlreadyQueuedException,
                    AlreadyExistsException,
                    HasPendingPassengersException {

        counterRepository.addSector("D");

        counterRepository.addCounters("D", 3);

        // Assign 1 to 1
        List<String> flights = List.of("AA123");
        Assignment assignmentD1 = new Assignment("AmericanAirlines", flights, 1);

        // Assign 2 to 2
        List<String> flights2 = List.of("AA124");
        Assignment assignmentD2 = new Assignment("AmericanAirlines", flights2, 1);

        // Assign 3 to 3
        List<String> flights3 = List.of("AA125");
        Assignment assignmentD3 = new Assignment("AmericanAirlines", flights3, 1);

        counterRepository.assignCounterAssignment("D", assignmentD1);
        counterRepository.assignCounterAssignment("D", assignmentD2);
        counterRepository.assignCounterAssignment("D", assignmentD3);

        // Free 1 to 1 and 3 to 3
        CountersRange result = counterRepository.freeCounters("D", 1, "AmericanAirlines");

        Assertions.assertEquals(1, result.range().from());
        Assertions.assertEquals(1, result.range().to());

        result = counterRepository.freeCounters("D", 3, "AmericanAirlines");

        Assertions.assertEquals(3, result.range().from());
        Assertions.assertEquals(3, result.range().to());

        // Check that counters are (1-1) free (2-2) assigned (3-3) free

        Sector sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(0).assignedInfo().isPresent());

        Assertions.assertEquals(2, sector.countersRangeList().get(1).range().from());
        Assertions.assertEquals(2, sector.countersRangeList().get(1).range().to());
        Assertions.assertTrue(sector.countersRangeList().get(1).assignedInfo().isPresent());
        Assertions.assertEquals(
                "AmericanAirlines",
                sector.countersRangeList().get(1).assignedInfo().get().airline());
        Assertions.assertEquals(
                flights2, sector.countersRangeList().get(1).assignedInfo().get().flights());

        Assertions.assertEquals(3, sector.countersRangeList().get(2).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(2).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(2).assignedInfo().isPresent());

        // Free 2 to 2
        result = counterRepository.freeCounters("D", 2, "AmericanAirlines");

        Assertions.assertEquals(2, result.range().from());
        Assertions.assertEquals(2, result.range().to());

        // Check that counters are (1-3) free

        sector = counterRepository.getSector("D").get();
        Assertions.assertEquals(1, sector.countersRangeList().get(0).range().from());
        Assertions.assertEquals(3, sector.countersRangeList().get(0).range().to());
        Assertions.assertFalse(sector.countersRangeList().get(0).assignedInfo().isPresent());
    }
}
