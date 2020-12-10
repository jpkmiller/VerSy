package broker;

import common.Direction;
import common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import javax.swing.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.*;
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
    Map<String, InetSocketAddress> nameService;
    private static final long LEASE_DURATION = 5000L;
    private Timer deadClientsTimer;


    class BrokerTask implements Runnable {
        Message msg;

        private BrokerTask(Message msg) {
            System.out.printf("Broker: Created Task %s.\n", msg.getPayload().getClass().getName());
            this.msg = msg;
        }

        @Override
        public void run() {
            Serializable payload = this.msg.getPayload();
            InetSocketAddress sender = this.msg.getSender();

            if (payload instanceof RegisterRequest) {
                register(sender, LEASE_DURATION);
            } else if (payload instanceof DeregisterRequest) {
                deregister(((DeregisterRequest) payload).getId());
            } else if (payload instanceof PoisonPill) {
                stopRequested = true;
            } else if (payload instanceof NameResolutionRequest) {
                resoluteName((NameResolutionRequest) payload, sender);
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

    public Broker() {
        this.stopRequested = false;
        this.hasToken = true;
        this.clientCollection = new ClientCollection<>();
        this.endpoint = new Endpoint(4712);
        this.pool = Executors.newFixedThreadPool(POOL_SIZE);
        this.nameService = new HashMap<>();
        pool.execute(new StopRequested());
        this.deadClientsTimer = new Timer();
        this.deadClientsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanDeadClients();
            }
        }, 10000L, 10000L);
    }

    public void broker() {
        Message msg;
        while (!this.stopRequested) {
            if ((msg = endpoint.nonBlockingReceive()) != null) {
                pool.execute(new BrokerTask(msg));
            }
        }
        pool.shutdown();
        System.out.println("Exited Broker");
    }

    public void register(InetSocketAddress sender, Long duration) {
        // acquire lock
        this.reentrantReadWriteLock.readLock().lock();
        int sizeClients = this.clientCollection.size();
        String clientId = "tank" + sizeClients;

        // reregister process
        Timestamp timestamp = new Timestamp(new Date().getTime());
        int indexClient = this.clientCollection.indexOf(sender);
        if (indexClient != -1) {
            clientId = this.clientCollection.getId(indexClient);
            System.out.printf("Broker: Client %s reregistered.\n", clientId);
            this.clientCollection.updateTimestamp(sender, timestamp);
            this.endpoint.send(sender, new RegisterResponse(clientId, LEASE_DURATION));
            this.reentrantReadWriteLock.readLock().unlock();
            return;
        }

        // add new client to clientCollection and nameService
        this.clientCollection.add(clientId, timestamp, sender);
        this.nameService.put(clientId, sender);

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
        this.endpoint.send(sender, new RegisterResponse(clientId, LEASE_DURATION));

        // send first client token
        if (this.hasToken)
            this.endpoint.send(sender, new Token());

        // release lock
        this.reentrantReadWriteLock.readLock().unlock();
    }

    public void deregister(String id) {
        this.reentrantReadWriteLock.readLock().lock();
        InetSocketAddress leftNeighbour = this.clientCollection.getLeftNeighborOf(this.clientCollection.indexOf(id));
        InetSocketAddress rightNeighbour = this.clientCollection.getLeftNeighborOf(this.clientCollection.indexOf(id));
        this.endpoint.send(leftNeighbour, new NeighbourUpdate(rightNeighbour, Direction.RIGHT));
        this.endpoint.send(rightNeighbour, new NeighbourUpdate(leftNeighbour, Direction.LEFT));
        this.clientCollection.remove(this.clientCollection.indexOf(id));
        this.nameService.remove(id);
        this.reentrantReadWriteLock.readLock().unlock();
    }

    private void resoluteName(NameResolutionRequest payload, InetSocketAddress sender) {
        InetSocketAddress response = nameService.get(payload.getTankId());
        this.endpoint.send(sender, new NameResolutionResponse(response, payload.getRequestId()));
    }

    private void cleanDeadClients() {
        List<String> deadClients = clientCollection.cleanDeadClients(LEASE_DURATION);
        for (String id : deadClients) {
            deregister(id);
            System.out.printf("Broker: Deregistered Client %s.\n", id);
        }
    }

    public static void main(String[] args) {
        new Broker().broker();
    }
}
