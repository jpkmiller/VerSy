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
					System.out.println("left of " + tankModel.getId() + " " + tankModel.leftNeighbour);
					System.out.println("right of " + tankModel.getId() + " " + tankModel.rightNeighbour);
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
