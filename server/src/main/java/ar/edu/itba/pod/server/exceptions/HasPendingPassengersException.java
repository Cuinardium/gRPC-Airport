package ar.edu.itba.pod.server.exceptions;

public class HasPendingPassengersException extends Exception {
    public HasPendingPassengersException(String message) {
        super(message);
    }
}
