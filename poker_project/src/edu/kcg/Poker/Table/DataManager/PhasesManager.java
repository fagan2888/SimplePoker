package edu.kcg.Poker.Table.DataManager;

public class PhasesManager {
	public static final int PREFLOP = 0, FLOP = 1, TURN = 2, RIVER = 3,	SHOWDOWN = 4;
	private int currentPhase;
	private int round;

	public int getCurrentPhase() {
		return currentPhase;
	}

	public int getRound() {
		return round;
	}

	public void setCurrentPhase(int currentPhase) {
		this.currentPhase = currentPhase;
	}

	public void setRound(int round) {
		this.round = round;
	}
}
