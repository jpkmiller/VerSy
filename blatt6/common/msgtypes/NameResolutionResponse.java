package common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class NameResolutionResponse implements Serializable {
    private final InetSocketAddress response;
    private final String requestId;

    public NameResolutionResponse(InetSocketAddress response, String requestId) {
        this.response = response;
        this.requestId = requestId;
    }

    public InetSocketAddress getResponse() {
        return response;
    }

    public String getRequestId() {
        return requestId;
    }
}
