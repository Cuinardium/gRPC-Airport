package ar.edu.itba.pod.server.repositories;

public class CounterRepositoryImplTest extends CounterRepositoryTest<CounterRepositoryImpl> {

    @Override
    protected CounterRepositoryImpl createCounterRepository() {
        return new CounterRepositoryImpl();
    }
}
