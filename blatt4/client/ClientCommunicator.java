package client;

import java.net.InetSocketAddress;

import common.Direction;
import common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;
import common.FishModel;
import common.Properties;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress neighbour) {
			endpoint.send(neighbour, new HandoffRequest(fish));
		}

		public void giveBackToken(InetSocketAddress receiver) {
			endpoint.send(receiver, new Token());
		}

		public void sendMarker(InetSocketAddress receiver) { endpoint.send(receiver, new SnapshotMarker());}

		public void updateGlobalState(CollectToken payload, InetSocketAddress receiver) {
			endpoint.send(receiver, payload);
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighbourUpdate) {
					NeighbourUpdate neighbourMsg = ((NeighbourUpdate) msg.getPayload());
					if (neighbourMsg.getDirection() == Direction.LEFT) {
						tankModel.leftNeighbour = neighbourMsg.getNeighbour();
					} else {
						tankModel.rightNeighbour = neighbourMsg.getNeighbour();
					}
				}

				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}

				if (msg.getPayload() instanceof SnapshotMarker) {
					System.out.println(tankModel.id + " received SnapshotMarker");
					tankModel.receiveMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof CollectToken) {
					System.out.println(tankModel.id + " received GlobalState");
					tankModel.receiveGlobalState((CollectToken) msg.getPayload());
				}

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
