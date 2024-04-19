package ar.edu.itba.pod.repositories;

import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CheckinRepositoryImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Optional;

public class CheckinRepositoryTest {

    private CheckinRepository checkinRepository;

    private final List<Checkin> checkins =
            List.of(
                    new Checkin("A", 1, "Aerolineas Argentinas", "AR1234", "123456"),
                    new Checkin("B", 2, "LATAM", "LA4321", "654321"),
                    new Checkin("C", 3, "American Airlines", "AA1111", "111111"),
                    new Checkin("A", 4, "British Airways", "BA2222", "222222"),
                    new Checkin("B", 5, "Iberia", "IB3333", "333333"),
                    new Checkin("C", 6, "Air France", "AF4444", "444444"));

    @BeforeEach
    public final void setUp() {
        this.checkinRepository = new CheckinRepositoryImpl();
    }

    @Test
    public final void testMultipleCheckinOrder() {
        checkins.forEach(checkinRepository::addCheckin);

        List<Checkin> checkins = checkinRepository.getCheckins();

        Assertions.assertEquals(6, checkins.size());

        for (int i = 0; i < checkins.size(); i++) {
            Assertions.assertEquals(this.checkins.get(i), checkins.get(i));
        }
    }

    @Test
    public final void testOneCheckin() {
        checkinRepository.addCheckin(checkins.get(0));


        Optional<Checkin> checkin = checkinRepository.getCheckin("123456");

        Assertions.assertTrue(checkin.isPresent());
        Assertions.assertEquals(checkins.get(0), checkin.get());
    }

    @Test
    public final void testNoCheckin() {
        Optional<Checkin> checkin = checkinRepository.getCheckin("123456");

        Assertions.assertTrue(checkin.isEmpty());
    }

    @Test
    public final void testHasCheckinsEmpty() {
        Assertions.assertFalse(checkinRepository.hasCheckins());
    }

    @Test
    public final void testHasCheckinsNotEmpty() {
        checkinRepository.addCheckin(checkins.get(0));

        Assertions.assertTrue(checkinRepository.hasCheckins());
    }

    @Test
    public final void testHasCheckinEmpty() {
        Assertions.assertFalse(checkinRepository.hasCheckin("123456"));
    }

    @Test
    public final void testHasCheckinNotEmpty() {
        checkinRepository.addCheckin(checkins.get(0));

        Assertions.assertTrue(checkinRepository.hasCheckin("123456"));
    }

    @Test
    public final void testGetCheckinsPredicate() {
        checkins.forEach(checkinRepository::addCheckin);

        List<Checkin> checkins = checkinRepository.getCheckins(checkin -> checkin.sector().equals("A"));

        Assertions.assertEquals(2, checkins.size());
        Assertions.assertEquals(this.checkins.get(0), checkins.get(0));
        Assertions.assertEquals(this.checkins.get(3), checkins.get(1));
    }


}
