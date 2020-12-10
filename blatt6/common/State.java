package common;

public enum State {
    IDLE(0), LEFT(-1), RIGHT(+1), BOTH(2);

    private int state;

    State(int state) {
        this.state = state;
    }

    public int getState() {
        return this.state;
    }
}
