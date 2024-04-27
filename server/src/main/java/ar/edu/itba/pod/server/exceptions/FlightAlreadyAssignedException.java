package ar.edu.itba.pod.server.exceptions;

public class FlightAlreadyAssignedException extends Exception{
    public FlightAlreadyAssignedException(String message) {
        super(message);
    }
}
