package common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final long leaseDuration;

	public RegisterResponse(String id, long durationLease) {
		this.id = id;
		this.leaseDuration = durationLease;
	}

	public String getId() {
		return id;
	}

	public long getLeaseDuration() {
		return leaseDuration;
	}
}
