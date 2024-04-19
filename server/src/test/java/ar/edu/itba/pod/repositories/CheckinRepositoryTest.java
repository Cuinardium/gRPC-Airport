package ar.edu.itba.pod.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CheckinRepositoryImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class CheckinRepositoryTest {

    private final List<Checkin> checkins =
            List.of(
                    new Checkin("A", 1, "Aerolineas Argentinas", "AR1234", "111111"),
                    new Checkin("B", 2, "LATAM", "LA4321", "222222"),
                    new Checkin("C", 3, "American Airlines", "AA1111", "333333"),
                    new Checkin("A", 4, "British Airways", "BA2222", "444444"),
                    new Checkin("B", 5, "Iberia", "IB3333", "555555"),
                    new Checkin("C", 6, "Air France", "AF4444", "666666"));
    private CheckinRepository checkinRepository;

    @BeforeEach
    public final void setUp() {
        this.checkinRepository = new CheckinRepositoryImpl();
    }

    @Test
    public final void testMultipleCheckinOrder() throws AlreadyExistsException {
        for (Checkin checkin : checkins) {
            checkinRepository.addCheckin(checkin);
        }

        List<Checkin> checkins = checkinRepository.getCheckins();

        Assertions.assertEquals(6, checkins.size());

        for (int i = 0; i < checkins.size(); i++) {
            Assertions.assertEquals(this.checkins.get(i), checkins.get(i));
        }
    }

    @Test
    public final void testOneCheckin() throws AlreadyExistsException {
        checkinRepository.addCheckin(checkins.get(0));

        Optional<Checkin> checkin = checkinRepository.getCheckin("111111");

        Assertions.assertTrue(checkin.isPresent());
        Assertions.assertEquals(checkins.get(0), checkin.get());
    }

    @Test
    public final void testAlreadyExists() throws AlreadyExistsException {
        checkinRepository.addCheckin(checkins.get(0));

        Assertions.assertThrows(
                AlreadyExistsException.class, () -> checkinRepository.addCheckin(checkins.get(0)));
    }

    @Test
    public final void testNoCheckin() {
        Optional<Checkin> checkin = checkinRepository.getCheckin("111111");

        Assertions.assertTrue(checkin.isEmpty());
    }

    @Test
    public final void testHasCheckinsEmpty() {
        Assertions.assertFalse(checkinRepository.hasCheckins());
    }

    @Test
    public final void testHasCheckinsNotEmpty() throws AlreadyExistsException {
        checkinRepository.addCheckin(checkins.get(0));

        Assertions.assertTrue(checkinRepository.hasCheckins());
    }

    @Test
    public final void testHasCheckinEmpty() {
        Assertions.assertFalse(checkinRepository.hasCheckin("111111"));
    }

    @Test
    public final void testHasCheckinNotEmpty() throws AlreadyExistsException {
        checkinRepository.addCheckin(checkins.get(0));

        Assertions.assertTrue(checkinRepository.hasCheckin("111111"));
    }

    @Test
    public final void testGetCheckinsPredicate() throws AlreadyExistsException {
        for (Checkin checkin : checkins) {
            checkinRepository.addCheckin(checkin);
        }

        List<Checkin> checkins =
                checkinRepository.getCheckins(checkin -> checkin.sector().equals("A"));

        Assertions.assertEquals(2, checkins.size());
        Assertions.assertEquals(this.checkins.get(0), checkins.get(0));
        Assertions.assertEquals(this.checkins.get(3), checkins.get(1));
    }


}
