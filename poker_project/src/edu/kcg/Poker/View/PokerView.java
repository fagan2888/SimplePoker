package edu.kcg.Poker.View;

import edu.kcg.Poker.Chair;
import edu.kcg.Poker.Table;

public interface PokerView {

	public void communityCardStatus(Table table);

	public void lastPlayStatus(Chair chair, int index);

	public void phaseStatus(int status);

	public void playerBankroll(Chair chair, int index);

	public void playerHands(int hands, int hand, int index);

	public void playerStatus(Chair chair, Table table);
}