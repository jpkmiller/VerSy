package broker;

import common.Direction;
import common.FishModel;
import common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    Boolean stopRequested;
    ClientCollection<InetSocketAddress> clientCollection;
    Endpoint endpoint;
    ExecutorService pool;
    private static final int POOL_SIZE = (int) (Runtime.getRuntime().availableProcessors() / 0.5);
    ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    boolean hasToken;


    class BrokerTask implements Runnable {
        Message msg;

        private BrokerTask(Message msg) {
            System.out.println("Created Task");
            this.msg = msg;
        }

        @Override
        public void run() {
            Serializable payload = this.msg.getPayload();
            InetSocketAddress sender = this.msg.getSender();

            if (payload instanceof RegisterRequest) {
                register(sender);
            } else if (payload instanceof DeregisterRequest) {
                deregister((DeregisterRequest) payload);
            } else if (payload instanceof PoisonPill) {
                stopRequested = true;
            }
        }
    }

    class StopRequested implements Runnable {

        @Override
        public void run() {
            JOptionPane.showMessageDialog(null, "Press OK button to stop server!");
            stopRequested = true;
        }
    }

    public Broker () {
        this.stopRequested = false;
        this.hasToken = true;
        this.clientCollection = new ClientCollection<>();
        this.endpoint = new Endpoint(4712);
        this.pool = Executors.newFixedThreadPool(POOL_SIZE);
        pool.execute(new StopRequested());
    }

    public void broker () {
        Message msg;
        while (!this.stopRequested) {
            if ((msg = endpoint.nonBlockingReceive()) != null) {
                pool.execute(new BrokerTask(msg));
            }
        }
        pool.shutdown();
        System.out.println("Exited Broker");
    }

    public void register (InetSocketAddress sender) {
        // acquire lock
        this.reentrantReadWriteLock.readLock().lock();
        int sizeClients = this.clientCollection.size();
        String clientId = "tank" + sizeClients;
        this.clientCollection.add(clientId, sender);

        // get neighbours
        InetSocketAddress leftNeighbour = this.clientCollection.getLeftNeighborOf(sizeClients);
        InetSocketAddress rightNeighbour = this.clientCollection.getRightNeighborOf(sizeClients);

        // update existing neighbours
        this.endpoint.send(leftNeighbour, new NeighbourUpdate(sender, Direction.RIGHT));
        this.endpoint.send(rightNeighbour, new NeighbourUpdate(sender, Direction.LEFT));

        // give new client neighbours
        this.endpoint.send(sender, new NeighbourUpdate(leftNeighbour, Direction.LEFT));
        this.endpoint.send(sender, new NeighbourUpdate(rightNeighbour, Direction.RIGHT));

        // register new client
        this.endpoint.send(sender, new RegisterResponse(clientId));

        // send first client token
        if (this.hasToken)
            this.endpoint.send(sender, new Token());

        // release lock
        this.reentrantReadWriteLock.readLock().unlock();
    }

    public void deregister (DeregisterRequest payload) {
        this.reentrantReadWriteLock.readLock().lock();
        InetSocketAddress leftNeighbour = this.clientCollection.getLeftNeighborOf(this.clientCollection.indexOf(payload.getId()));
        InetSocketAddress rightNeighbour = this.clientCollection.getLeftNeighborOf(this.clientCollection.indexOf(payload.getId()));
        this.endpoint.send(leftNeighbour, new NeighbourUpdate(rightNeighbour, Direction.RIGHT));
        this.endpoint.send(rightNeighbour, new NeighbourUpdate(leftNeighbour, Direction.LEFT));
        this.clientCollection.remove(this.clientCollection.indexOf(payload.getId()));
        this.reentrantReadWriteLock.readLock().unlock();
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}
