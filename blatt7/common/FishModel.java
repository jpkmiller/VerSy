package common;

import client.TankModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

@SuppressWarnings("serial")
public final class FishModel implements Serializable {
	private final static int xSize = 100;
	private final static int ySize = 50;
	private final static Random rand = new Random();

	private final String id;
	private int x;
	private int y;
	private common.Direction direction;

	private boolean toggled;

	public FishModel(String id, int x, int y, common.Direction direction) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.direction = direction;
	}

	public String getId() {
		return id;
	}

	public String getTankId() {
		return id.substring(id.indexOf("@") + 1);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public common.Direction getDirection() {
		return direction;
	}

	public void reverse() {
		direction = direction.reverse();
	}

	public static int getXSize() {
		return xSize;
	}

	public static int getYSize() {
		return ySize;
	}

	public void toggle() {
		toggled = !toggled;
	}

	public boolean isToggled() {
		return toggled;
	}

	public boolean hitsEdge() {
		return (direction == common.Direction.LEFT && x == 0)
				|| (direction == common.Direction.RIGHT && x == TankModel.WIDTH - xSize);
	}

	public boolean disappears() {
		return (direction == common.Direction.LEFT && x == -xSize)
				|| (direction == common.Direction.RIGHT && x == TankModel.WIDTH);
	}

	public void update() {
		x += direction.getVector();

		double discreteSin = Math.round(Math.sin(x / 30.0));
		discreteSin = rand.nextInt(10) < 8 ? 0 : discreteSin;
		y += discreteSin;
		y = y < 0 ? 0 : java.lang.Math.min(y, TankModel.HEIGHT - FishModel.getYSize());
	}

	public void setToStart() {
		x = direction == common.Direction.LEFT ? TankModel.WIDTH : -xSize;
	}

	public boolean isDeparting() {
		return (direction == common.Direction.LEFT && x < 0)
				|| (direction == common.Direction.RIGHT && x > TankModel.WIDTH - xSize);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FishModel) {
			return this.getId().equals(((FishModel) obj).getId());
		}
		return false;
	}
}
