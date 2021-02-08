package common.msgtypes;

import java.io.Serializable;

public class LocationUpdate implements Serializable {

    private final String requestId;

    public LocationUpdate(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

}
