package ar.edu.itba.pod.client;

import ar.edu.itba.pod.grpc.common.CounterRange;
import ar.edu.itba.pod.grpc.counter.CounterServiceGrpc;
import ar.edu.itba.pod.grpc.counter.ListSectorsResponse;
import ar.edu.itba.pod.grpc.counter.SectorInfo;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CounterClient {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("tpe1-g4 Counter Client Starting ...");
        logger.info("grpc-com-patterns Counter Client Starting ...");


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

        CounterServiceGrpc.CounterServiceBlockingStub stub = CounterServiceGrpc.newBlockingStub(channel);

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

    private static void executeAction(String action, CounterServiceGrpc.CounterServiceBlockingStub stub) {
        switch (action) {
            case "listSectors":
                ListSectorsResponse listSectorsResponse = stub.listSectors(Empty.getDefaultInstance());
                List<SectorInfo> sectorsList = listSectorsResponse.getSectorsList();
                System.out.println("Sectors   Counters");
                System.out.println("###################");
                for(SectorInfo sector : sectorsList) {
                    System.out.print(sector.getSectorName() + "         ");
                    for(CounterRange range : sector.getCounterRangesList()) {
                        System.out.print("("+range.getTo()+"-"+range.getFrom()+")");
                    }
                    System.out.println();
                }
                break;

            case "listCounters":

                break;

            case "assignCounters":

                break;

            case "freeCounters":

                break;

            case "checkinCounters":

                break;

            case "listPendingAssignments":

                break;
            default:
                // TODO: Exception?
                logger.error("Unknown action: {}", action);
                break;
        }
    }
}
