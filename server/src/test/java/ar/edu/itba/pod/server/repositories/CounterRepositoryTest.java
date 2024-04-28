package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.exceptions.FlightAlreadyAssignedException;
import ar.edu.itba.pod.server.exceptions.FlightAlreadyCheckedInException;
import ar.edu.itba.pod.server.exceptions.FlightAlreadyQueuedException;
import ar.edu.itba.pod.server.models.Assignment;
import ar.edu.itba.pod.server.models.CountersRange;
import ar.edu.itba.pod.server.models.Range;
import ar.edu.itba.pod.server.models.Sector;
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

        Assertions.assertThrows(AlreadyExistsException.class, () -> counterRepository.addSector("D"));
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
        Assertions.assertThrows(NoSuchElementException.class, () -> counterRepository.addCounters("D", 5));
    }

    // ---- Assignments
    @Test
    public void testAssignCounterAssignment() throws AlreadyExistsException, FlightAlreadyCheckedInException, FlightAlreadyAssignedException, FlightAlreadyQueuedException {
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

            Optional<Pair<CountersRange, String>> countersRangeAndSector = counterRepository.getFlightCountersAndSector(flight);
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

            Optional<Pair<CountersRange, String>> countersRangeAndSector = counterRepository.getFlightCountersAndSector(flight);
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

        Optional<Pair<CountersRange, String>> countersRangeAndSector = counterRepository.getFlightCountersAndSector("AA127");
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
        Assertions.assertEquals("AmericanAirlines", sectorC.countersRangeList().get(0).assignedInfo().get().airline());
        Assertions.assertEquals(List.of("AA125", "AA126"), sectorC.countersRangeList().get(0).assignedInfo().get().flights());

        Assertions.assertEquals(8, sectorC.countersRangeList().get(1).range().from());
        Assertions.assertEquals(8, sectorC.countersRangeList().get(1).range().to());

        Assertions.assertTrue(sectorC.countersRangeList().get(1).assignedInfo().isPresent());
        Assertions.assertEquals("AmericanAirlines", sectorC.countersRangeList().get(1).assignedInfo().get().airline());
        Assertions.assertEquals(List.of("AA128"), sectorC.countersRangeList().get(1).assignedInfo().get().flights());


        Sector sectorD = sectors.get(1);
        Assertions.assertEquals("D", sectorD.sectorName());

        Assertions.assertEquals(3, sectorD.countersRangeList().size());

        Assertions.assertEquals(1, sectorD.countersRangeList().get(0).range().from());
        Assertions.assertEquals(2, sectorD.countersRangeList().get(0).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(0).assignedInfo().isPresent());
        Assertions.assertEquals("AmericanAirlines", sectorD.countersRangeList().get(0).assignedInfo().get().airline());
        Assertions.assertEquals(List.of("AA123", "AA124"), sectorD.countersRangeList().get(0).assignedInfo().get().flights());

        Assertions.assertEquals(3, sectorD.countersRangeList().get(1).range().from());
        Assertions.assertEquals(5, sectorD.countersRangeList().get(1).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(1).assignedInfo().isPresent());
        Assertions.assertEquals("AmericanAirlines", sectorD.countersRangeList().get(1).assignedInfo().get().airline());
        Assertions.assertEquals(List.of("AA127"), sectorD.countersRangeList().get(1).assignedInfo().get().flights());

        Assertions.assertEquals(9, sectorD.countersRangeList().get(2).range().from());
        Assertions.assertEquals(11, sectorD.countersRangeList().get(2).range().to());

        Assertions.assertTrue(sectorD.countersRangeList().get(2).assignedInfo().isPresent());
        Assertions.assertEquals("AmericanAirlines", sectorD.countersRangeList().get(2).assignedInfo().get().airline());
        Assertions.assertEquals(List.of("AA129"), sectorD.countersRangeList().get(2).assignedInfo().get().flights());
    }

}
