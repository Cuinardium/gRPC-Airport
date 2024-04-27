package ar.edu.itba.pod.server.models;


import ar.edu.itba.pod.grpc.query.CheckinInfo;

public record Checkin(String sector, int counter, String airline, String flight, String booking) {

    public CheckinInfo grpcMessage(){
        return CheckinInfo
                .newBuilder()
                .setSectorName(this.sector)
                .setCounter(this.counter)
                .setAirline(this.airline)
                .setFlight(this.flight)
                .setBooking(this.booking)
                .build();
    }
}
