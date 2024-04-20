package ar.edu.itba.pod.server.repositories;

import ar.edu.itba.pod.server.exceptions.AlreadyExistsException;
import ar.edu.itba.pod.server.models.Checkin;
import ar.edu.itba.pod.server.repositories.CheckinRepository;
import ar.edu.itba.pod.server.repositories.CheckinRepositoryImpl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    @Test
    public final void testConcurrentAdd() throws AlreadyExistsException {
        List<List<Checkin>> repeatedCheckins = new LinkedList<>();

        for (Checkin checkin : checkins) {
            List<Checkin> aux = new LinkedList<>();

            for (int i = 0; i < 1000; i++) {
                aux.add(
                        new Checkin(
                                checkin.sector(),
                                checkin.counter(),
                                checkin.airline(),
                                checkin.flight(),
                                String.valueOf(Integer.parseInt(checkin.booking()) + i)));
            }

            repeatedCheckins.add(aux);
        }

        CountDownLatch latch = new CountDownLatch(repeatedCheckins.size());
        Map<String, Boolean> checkinsContainedInOrder = new ConcurrentHashMap<>();

        for (List<Checkin> checkins : repeatedCheckins) {
            new Thread(
                            () -> {
                                for (Checkin checkin : checkins) {
                                    try {
                                        checkinRepository.addCheckin(checkin);

                                        Thread.sleep((long) (Math.random() * 10));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }

                                checkinsContainedInOrder.put(
                                        checkins.get(0).booking(), checkListContainedInOrder(checkins, checkinRepository.getCheckins()));

                                latch.countDown();
                            })
                    .start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (List<Checkin> checkins : repeatedCheckins) {
            Assertions.assertTrue(
                    checkListContainedInOrder(checkins, checkinRepository.getCheckins()));
        }

        for (Boolean contained : checkinsContainedInOrder.values()) {
            Assertions.assertTrue(contained);
        }
    }

    // Returns true if the subList is contained in the list in the same order
    // if the sublist is [A, B, C] and the list is [A, D, B, E, C, F] then the method should return
    // true
    private boolean checkListContainedInOrder(List<Checkin> subList, List<Checkin> list) {
        int currentListIndex = 0;

        for (Checkin checkin : subList) {
            List<Checkin> currentList = list.subList(currentListIndex, list.size());

            int checkinIndex = currentList.indexOf(checkin);

            if (checkinIndex == -1) {
                return false;
            }

            currentListIndex = checkinIndex;
        }

        return true;
    }
}
