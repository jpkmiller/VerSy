package broker;

import common.Direction;
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
        this.endpoint = new Endpoint(4712);
    }

    public void broker () {
        System.out.println("All Systems running...");
        while (true) {
            Message msg = this.endpoint.blockingReceive();
            Serializable payload = msg.getPayload();

            if (payload instanceof RegisterRequest) {
                register(msg.getSender());
            } else if (payload instanceof DeregisterRequest) {
                deregister((DeregisterRequest) payload);
            } else if (payload instanceof HandoffRequest) {
                handoffFish((HandoffRequest) payload, msg.getSender());
            }
        }
    }

    public void register (InetSocketAddress sender) {
        String clientId = "tank" + this.clientCollection.size();
        this.endpoint.send(sender, new RegisterResponse(clientId));
        this.clientCollection.add(clientId, sender);
    }

    private boolean testingNeighbours () {
        if (this.clientCollection.size() == 1) {
            return this.clientCollection.getClient(0) == this.clientCollection.getLeftNeighborOf(0) &&
                    this.clientCollection.getClient(0) == this.clientCollection.getRightNeighborOf(0);
        } else if (this.clientCollection.size() == 2) {
            return this.clientCollection.getClient(1) == this.clientCollection.getLeftNeighborOf(0) &&
                    this.clientCollection.getClient(0) == this.clientCollection.getRightNeighborOf(1) &&
                    this.clientCollection.getClient(1) == this.clientCollection.getRightNeighborOf(0) &&
                    this.clientCollection.getClient(0) == this.clientCollection.getLeftNeighborOf(1);
        } else if (this.clientCollection.size() == 3) {
            return this.clientCollection.getClient(2) == this.clientCollection.getLeftNeighborOf(0) &&
                    this.clientCollection.getClient(0) == this.clientCollection.getLeftNeighborOf(1) &&
                    this.clientCollection.getClient(1) == this.clientCollection.getRightNeighborOf(0);
        }
        return false;
    }

    public void deregister (DeregisterRequest payload) {
        this.clientCollection.remove(this.clientCollection.indexOf(payload.getId()));
    }

    public void handoffFish(HandoffRequest payload, InetSocketAddress sender) {
        FishModel fish = payload.getFish();
        int indexOfSender = this.clientCollection.indexOf(sender);
        InetSocketAddress receiver = fish.getDirection() == Direction.RIGHT ? this.clientCollection.getRightNeighborOf(indexOfSender) : this.clientCollection.getLeftNeighborOf(indexOfSender);
        this.endpoint.send(receiver, payload);
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}
