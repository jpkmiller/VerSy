package common;

public enum Direction {
	LEFT(-1), RIGHT(+1);

	private int vector;

	Direction(int vector) {
		this.vector = vector;
	}

	public int getVector() {
		return vector;
	}

	public Direction reverse() {
		return this == LEFT ? RIGHT : LEFT;
	}

}
