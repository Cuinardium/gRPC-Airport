package ar.edu.itba.pod.server.exceptions;

public class FlightBelongsToOtherAirlineException extends Exception {
    public FlightBelongsToOtherAirlineException(String message) {
        super(message);
    }
}
