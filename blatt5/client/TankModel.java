package client;

import common.Direction;
import common.FishModel;
import common.ReferenceState;
import common.State;
import common.msgtypes.CollectToken;
import common.msgtypes.NameResolutionResponse;

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
    protected boolean isInitiator;
    protected boolean finishedRecording;
    protected boolean finishedGlobalState;
    protected Set<FishModel> localState;
    protected Set<FishModel> leftState;
    protected Set<FishModel> rightState;
    protected State recordState;
    protected CollectToken globalState;
    protected final Map<String, ReferenceState> fishieReference;

    public TankModel(client.ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.forwarder = forwarder;
        this.timer = new Timer();
        this.recordState = State.IDLE;
        this.leftState = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.rightState = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.fishieReference = new ConcurrentHashMap<>();
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

            // add fishie to reference map
            this.fishieReference.put(fish.getId(), ReferenceState.HERE);

            fishies.add(fish);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        fish.setToStart();
        Direction dirNeighbour = fish.getDirection();
        if ((recordState == State.LEFT || recordState == State.BOTH) && dirNeighbour == Direction.LEFT) {
            rightState.add(fish);
        } else if ((recordState == State.RIGHT || recordState == State.BOTH) && dirNeighbour == Direction.RIGHT) {
            leftState.add(fish);
        }

        // update reference map
        if (!this.fishieReference.containsKey(fish.getId()))
            this.fishieReference.put(fish.getId(), ReferenceState.HERE);
        else
            this.fishieReference.replace(fish.getId(), ReferenceState.HERE);

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

    public void receiveMarker(InetSocketAddress neighbour) {
        if (recordState == State.IDLE) {
            finishedRecording = false;
            initiateSnapshot(false, neighbour);
        } else {
            stopSnapshot(neighbour);
        }
    }

    private void stopSnapshot(InetSocketAddress neighbour) {
        assert (neighbour != null);
        Direction dirNeighbour = neighbour.equals(leftNeighbour) ? Direction.LEFT : Direction.RIGHT;
        if (recordState == State.BOTH) {
            if (dirNeighbour == Direction.LEFT) {
                recordState = State.RIGHT;
            } else {
                recordState = State.LEFT;
            }
        } else if (recordState != State.IDLE) {
            recordState = State.IDLE;
            if (isInitiator)
                sendGlobalState();
            finishedRecording = true;
        }
        System.out.println(recordState);
    }

    public void initiateSnapshot(boolean isInitiator, InetSocketAddress neighbour) {
        if (isInitiator) {
            this.isInitiator = true;
            this.globalState = null;
        }
        finishedGlobalState = false;
        System.out.println("Initiate Snapshot");
        localState = new HashSet<>(fishies);

        if (neighbour != null) {
            if (neighbour.equals(leftNeighbour)) {
                // when getting a message from left only record right
                recordState = State.RIGHT;
            } else if (neighbour == rightNeighbour) {
                // when getting a message from right only record left
                recordState = State.LEFT;
            }
        } else {
            // start recording state for both channels
            recordState = State.BOTH;
        }

        System.out.println("Init with: " + recordState);

        // send marker to both neighbours
        forwarder.sendMarker(leftNeighbour);
        forwarder.sendMarker(rightNeighbour);
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
                    this.fishieReference.replace(fish.getId(), fish.getDirection() == Direction.RIGHT ? ReferenceState.RIGHT : ReferenceState.LEFT);
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

    public void receiveGlobalState(CollectToken payload) {
        this.globalState = payload;
        if (isInitiator && finishedRecording) {
            finishedGlobalState = true;
            isInitiator = false;
            finishedRecording = false;
            System.out.println("global State " + this.globalState);
        }
    }

    private void sendGlobalState() {
        if (this.globalState == null)
            this.globalState = new CollectToken();
        this.globalState.addGlobalState(this.leftState);
        this.globalState.addGlobalState(this.localState);
        this.globalState.addGlobalState(this.rightState);
        forwarder.updateGlobalState(this.globalState, leftNeighbour);
    }

    public Set<FishModel> getGlobalState() {
        if (this.finishedGlobalState) {
            return this.globalState.getGlobalState();
        }
        return Collections.emptySet();
    }

    public boolean isFinishedGlobalState() {
        return this.finishedGlobalState;
    }

    private void passOnGs() {
        if (finishedRecording) {
            if (!isInitiator) {
                sendGlobalState();
                finishedRecording = false;
            }
        }
    }

    public void locateFishGlobally(String fishId) {
        if (this.fishieReference.containsKey(fishId)) {
            ReferenceState fishieReference = this.fishieReference.get(fishId);
            if (fishieReference == ReferenceState.HERE) {
                for (FishModel fish : fishies) {
                    if (fish.getId().equals(fishId)) {
                        fish.toggle();
                        return;
                    }
                }
            } else if (fishieReference == ReferenceState.RIGHT) {
                forwarder.sendLocationRequest(fishId, rightNeighbour);
            } else {
                forwarder.sendLocationRequest(fishId, leftNeighbour);
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
        passOnGs();
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
