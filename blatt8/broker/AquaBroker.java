package broker;

import client.AquaClient;
import common.FishModel;
import common.msgtypes.NameResolutionRequest;
import common.msgtypes.NameResolutionResponse;

import java.net.InetSocketAddress;
import java.rmi.*;

public interface AquaBroker extends Remote {
    String register(AquaClient client) throws RemoteException;
    AquaClient resoluteName(String tankId) throws RemoteException;
    void deregister(String id) throws RemoteException;
}
