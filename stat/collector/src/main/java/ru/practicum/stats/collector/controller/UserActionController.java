
package ru.practicum.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.stats.collector.service.CollectorService;

@GrpcService
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final CollectorService collectorService;

    public UserActionController(CollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        collectorService.collectUserAction(request);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
