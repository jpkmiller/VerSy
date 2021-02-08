package client;
import common.FishModel;
import common.msgtypes.NameResolutionRequest;
import common.msgtypes.NameResolutionResponse;

import java.net.InetSocketAddress;
import java.rmi.*;

public interface AquaClient extends Remote {
    void register(InetSocketAddress sender);
    void deregister(String id);
}
