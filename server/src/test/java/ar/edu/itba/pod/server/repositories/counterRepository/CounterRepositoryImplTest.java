package ar.edu.itba.pod.server.repositories.counterRepository;

import ar.edu.itba.pod.server.repositories.CounterRepositoryImpl;

public class CounterRepositoryImplTest extends CounterRepositoryTest<CounterRepositoryImpl> {

    @Override
    protected CounterRepositoryImpl createCounterRepository() {
        return new CounterRepositoryImpl();
    }
}
