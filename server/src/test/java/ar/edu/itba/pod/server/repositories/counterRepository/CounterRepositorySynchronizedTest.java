package ar.edu.itba.pod.server.repositories.counterRepository;

import ar.edu.itba.pod.server.repositories.CounterRepositorySynchronized;

public class CounterRepositorySynchronizedTest extends CounterRepositoryTest<CounterRepositorySynchronized> {

    @Override
    protected CounterRepositorySynchronized createCounterRepository() {
        return new CounterRepositorySynchronized();
    }
}
