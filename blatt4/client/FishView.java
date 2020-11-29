package client;

import java.awt.Image;

import javax.swing.ImageIcon;

import common.Direction;
import common.FishModel;

public class FishView {
	private static final Image imgBlackLeft = new ImageIcon(
			FishView.class.getResource("/client/resources/trump.png"))
			.getImage().getScaledInstance(FishModel.getXSize(), -1, java.awt.Image.SCALE_SMOOTH);

	private static final Image imgBlackRight = new ImageIcon(
			FishView.class.getResource("/client/resources/trump.png"))
			.getImage().getScaledInstance(FishModel.getXSize(), -1, java.awt.Image.SCALE_SMOOTH);

	private static final Image imgRedLeft = new ImageIcon(
			FishView.class.getResource("/client/resources/trump.png"))
			.getImage().getScaledInstance(FishModel.getXSize(), -1, java.awt.Image.SCALE_SMOOTH);

	private static final Image imgRedRight = new ImageIcon(
			FishView.class.getResource("/client/resources/trump.png"))
			.getImage().getScaledInstance(FishModel.getXSize(), -1, java.awt.Image.SCALE_SMOOTH);

	public Image getImage(FishModel fishModel) {
		return fishModel.isToggled() ? (fishModel.getDirection() == Direction.LEFT ? imgRedLeft
				: imgRedRight) : (fishModel.getDirection() == Direction.LEFT ? imgBlackLeft
				: imgBlackRight);
	}
}
