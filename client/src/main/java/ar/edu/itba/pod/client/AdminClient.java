package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.admin.AddCountersRequest;
import ar.edu.itba.pod.grpc.admin.AddSectorRequest;
import ar.edu.itba.pod.grpc.admin.AdminServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AdminClient {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Admin Client Starting ...");
        logger.info("grpc-com-patterns Admin Client Starting ...");

        String svrAdd, host, port, action;

        try {
            svrAdd = Optional.ofNullable(System.getProperty("serverAddress")).orElseThrow(IllegalArgumentException::new);
        } catch (IllegalArgumentException e) {
            logger.error("You must specify a serverAddress");
            return;
        }

        try {
            String[] address = svrAdd.split(":");
            host = address[0];
            port = address[1];
        } catch (IndexOutOfBoundsException e) {
            logger.error("You must specify a port");
            return;
        }
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, Integer.parseInt(port))
                .usePlaintext()
                .build();

        AdminServiceGrpc.AdminServiceBlockingStub stub = AdminServiceGrpc.newBlockingStub(channel);

        try {
            action = Optional.ofNullable(System.getProperty("action")).orElseThrow(IllegalArgumentException::new);
        } catch (IllegalArgumentException e) {
            logger.error("You must specify an action");
            return;
        }
        try {
            executeAction(action, stub);
        } finally {
            channel.shutdown().awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private static void executeAction(String action, AdminServiceGrpc.AdminServiceBlockingStub stub) {
        if (action.equals("addSector") || action.equals("addCounters") || action.equals("manifest")) {
            switch (action) {
                case "addSector":
                    String sectorName = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                    AddSectorRequest addSectorRequest = AddSectorRequest
                            .newBuilder()
                            .setSectorName(sectorName)
                            .build();
                    stub.addSector(addSectorRequest);
                    break;

                case "addCounters":
                    String sector = Optional.ofNullable(System.getProperty("sector")).orElseThrow(IllegalArgumentException::new);
                    int counters = Integer.parseInt(Optional.ofNullable(System.getProperty("counters")).orElseThrow(IllegalArgumentException::new));
                    AddCountersRequest addCountersRequest = AddCountersRequest
                            .newBuilder()
                            .setSectorName(sector)
                            .setCounterCount(counters)
                            .build();
                    stub.addCounters(addCountersRequest);
                    break;

                case "manifest":

                    break;
            }

        }
    }
}
