package ar.edu.itba.pod.server.exceptions;

public class FlightAlreadyQueuedException extends Exception{
    public FlightAlreadyQueuedException(String message) {
        super(message);
    }
}
