package common;

public enum ReferenceState {
    HERE(0), LEFT(-1), RIGHT(+1);

    private int state;

    ReferenceState(int state) {
        this.state = state;
    }

    public int getState() {
         return this.state;
    }
}
