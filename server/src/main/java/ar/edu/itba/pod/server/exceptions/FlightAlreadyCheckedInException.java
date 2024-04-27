package ar.edu.itba.pod.server.exceptions;

public class FlightAlreadyCheckedInException extends Exception{
    public FlightAlreadyCheckedInException(String message) {
        super(message);
    }
}
