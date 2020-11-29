package client;

import common.Direction;
import common.FishModel;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected volatile String id;
    protected final Set<FishModel> fishies;
    protected int fishCounter = 0;
    protected final client.ClientCommunicator.ClientForwarder forwarder;
    protected InetSocketAddress leftNeighbour;
    protected InetSocketAddress rightNeighbour;
    protected boolean hasToken;
    protected Timer timer;

    public TankModel(client.ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
        this.forwarder = forwarder;
        this.timer = new Timer();
    }

    synchronized void onRegistration(String id) {
        this.id = id;
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
            y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y, rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishies.add(fish);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        fish.setToStart();
        fishies.add(fish);
    }

    public String getId() {
        return id;
    }

    public InetSocketAddress getLeftNeighbour() {
        return leftNeighbour;
    }

    public InetSocketAddress getRightNeighbour() {
        return rightNeighbour;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge()) {
                if (hasToken()) {
                    forwarder.handOff(fish, fish.getDirection() == Direction.RIGHT ? getRightNeighbour() : getLeftNeighbour());
                } else {
                    fish.reverse();
                }
            }

            if (fish.disappears()) {
                it.remove();
            }
        }
    }

    public void receiveToken() {
        this.hasToken = true;
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                hasToken = false;
                forwarder.giveBackToken(leftNeighbour);
            }
        }, 2000L);
    }


    public boolean hasToken() {
        return this.hasToken;
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        forwarder.deregister(id);
    }

}
