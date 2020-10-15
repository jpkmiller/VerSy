package common;

public enum Direction {
	LEFT(-2), RIGHT(+2);

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
