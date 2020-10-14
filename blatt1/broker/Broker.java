package broker;

import common.FishModel;
import common.msgtypes.DeregisterRequest;
import common.msgtypes.HandoffRequest;
import common.msgtypes.RegisterRequest;
import common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {

    ClientCollection<InetSocketAddress> clientCollection;
    Endpoint endpoint;

    public Broker () {
        this.clientCollection = new ClientCollection<>();
        this.endpoint = new Endpoint(4711);
    }

    public void broker () {
        while (true) {
            Message msg = this.endpoint.blockingReceive();
            Serializable payload = msg.getPayload();
            InetSocketAddress sender = msg.getSender();

            if (payload instanceof RegisterRequest) {
                register(sender);
            } else if (payload instanceof DeregisterRequest) {
                deregister((DeregisterRequest) payload);
            } else if (payload instanceof HandoffRequest) {
                handoffFish((HandoffRequest) payload);
            }
        }
    }

    public void register (InetSocketAddress sender) {
        String clientId = "tank" + this.clientCollection.size();
        RegisterResponse response = new RegisterResponse(clientId);
        this.clientCollection.add(clientId, sender);
        this.endpoint.send(sender, response);
    }

    public void deregister (DeregisterRequest payload) {
        String id = payload.getId();
        int indexOfClient = this.clientCollection.indexOf(id);
        this.clientCollection.remove(indexOfClient);
    }

    public void handoffFish(HandoffRequest payload) {
        FishModel fish = payload.getFish();
        String senderName = fish.getTankId();
        int indexOfSender = this.clientCollection.indexOf(senderName);
        System.out.println(fish.getDirection());
        InetSocketAddress receiver = this.clientCollection.getRightNeighorOf(indexOfSender);
        this.endpoint.send(receiver, payload);
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}
