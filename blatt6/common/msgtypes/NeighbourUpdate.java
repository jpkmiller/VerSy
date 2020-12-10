package common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;
import common.Direction;

public class NeighbourUpdate implements Serializable {

    private final Direction direction;
    private final InetSocketAddress neighbour;

    public NeighbourUpdate(InetSocketAddress neighbour, Direction direction) {
        this.neighbour = neighbour;
        this.direction = direction;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public InetSocketAddress getNeighbour() {
        return this.neighbour;
    }
}
