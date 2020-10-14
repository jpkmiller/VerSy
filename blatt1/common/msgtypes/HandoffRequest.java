package common.msgtypes;

import common.FishModel;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class HandoffRequest implements Serializable {
	private final FishModel fish;

	public HandoffRequest(FishModel fish) {
		this.fish = fish;
	}

	public FishModel getFish() {
		return fish;
	}
}
