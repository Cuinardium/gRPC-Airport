package ar.edu.itba.pod.server.repositories;

public class CounterRepositorySynchronizedTest extends CounterRepositoryTest<CounterRepositorySynchronized> {

    @Override
    protected CounterRepositorySynchronized createCounterRepository() {
        return new CounterRepositorySynchronized();
    }
}
