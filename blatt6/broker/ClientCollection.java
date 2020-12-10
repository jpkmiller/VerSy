package broker;

import java.sql.Timestamp;
import java.util.*;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or
 * externally synchronized.
 */

public class ClientCollection<T> {
    private class Client {
        final String id;
        final T client;
        final Timestamp timestamp;

        Client(String id, Timestamp timestamp, T client) {
            this.id = id;
            this.client = client;
            this.timestamp = timestamp;
        }
    }

    private final List<Client> clients;

    public ClientCollection() {
        clients = new ArrayList<>();
    }

    public List<String> cleanDeadClients(long leaseDuration) {
        long actualTime = new Timestamp(new Date().getTime()).getTime();
        List<String> deadClients = new ArrayList<>();
        for (Client client : clients)
            if ((client.timestamp.getTime() - actualTime) > leaseDuration)
                deadClients.add(client.id);
        return deadClients;
    }

    public ClientCollection<T> updateTimestamp(T client, Timestamp timestamp) {
        int clientIndex = indexOf(client);
        Client c = clients.get(clientIndex);
        clients.set(clientIndex, new Client(c.id, timestamp, c.client));
        return this;
    }

    public ClientCollection<T> add(String id, Timestamp timestamp, T client) {
        clients.add(new Client(id, timestamp, client));
        return this;
    }

    public ClientCollection<T> remove(int index) {
        clients.remove(index);
        return this;
    }

    public int indexOf(String id) {
        for (int i = 0; i < clients.size(); i++)
            if (clients.get(i).id.equals(id))
                return i;
        return -1;
    }

    public int indexOf(T client) {
        for (int i = 0; i < clients.size(); i++)
            if (clients.get(i).client.equals(client))
                return i;
        return -1;
    }

    public T getClient(int index) {
        return clients.get(index).client;
    }

    public String getId(int index) {
        return clients.get(index).id;
    }

    public int size() {
        return clients.size();
    }

    public T getLeftNeighborOf(int index) {
        // return clients.get((index - 1 + clients.size()) % clients.size()).client;
        return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
    }

    public T getRightNeighborOf(int index) {
        return clients.get((index + 1) % clients.size()).client;
        // return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
    }

}
