package edu.kcg.Poker.Logger;

import edu.kcg.Poker.Table.Table;

abstract public class PokerGameLogger {

	protected Table table;

	public PokerGameLogger(Table table) {
		this.table = table;
	}

	abstract public void communityCardStatus();

	abstract public void lastPlayStatus(int index);

	abstract public void lastPhaseStatus();

	abstract public void phaseNameStatus(int status);

	abstract public void playersBankrollStatus();

	abstract public void playersHandsStatus();
	
	abstract public void playersHandRollStatus(int[] handrolls);

	abstract public void playerStatus(int index);

	abstract public void setTable(Table table);
}
