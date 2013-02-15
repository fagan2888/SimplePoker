package edu.kcg.Poker.Strategy;

import java.util.ArrayList;

import edu.kcg.Poker.Chair;
import edu.kcg.Poker.Table;

public class Params {

	private int anty;
	private int[] bankrolls;
	private int hands;
	private int[] lastPlays;
	private int limit;
	private int maxRaise;
	private int pot;
	private int round;

	public Params(Table table) {
		this.anty = table.getAnty();
		this.limit = table.getLimit();
		this.maxRaise = table.getMaxRaise();
		this.pot = table.getPot();
		this.round = table.getRound();
		this.setBankrolls(table.getChairs());
		this.setLastPlays(table.getChairs());
	}

	public int getAnty() {
		return anty;
	}

	public int[] getBankrolls() {
		return bankrolls;
	}

	public int getHands() {
		return this.hands;
	}

	public int[] getLastPlays() {
		return lastPlays;
	}

	public int getLimit() {
		return limit;
	}

	public int getMaxRaise() {
		return maxRaise;
	}

	public int getPot() {
		return pot;
	}

	public int getRound() {
		return round;
	}

	public void setAnty(int anty) {
		this.anty = anty;
	}

	public void setBankrolls(ArrayList<Chair> chairs) {
		this.bankrolls = new int[chairs.size()];
		int i = 0;
		for (Chair chair : chairs) {
			this.bankrolls[i] = chair.getBankroll();
			i++;
		}
	}

	public void setHands(int hands) {
		this.hands = hands;
	}

	public void setLastPlays(ArrayList<Chair> chairs) {
		this.lastPlays = new int[chairs.size()];
		int i = 0;
		for (Chair chair : chairs) {
			this.lastPlays[i] = chair.getLastPlay();
			i++;
		}
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void setMaxRaise(int maxRaise) {
		this.maxRaise = maxRaise;
	}

	public void setPot(int pot) {
		this.pot = pot;
	}

	public void setRound(int round) {
		this.round = round;
	}

}