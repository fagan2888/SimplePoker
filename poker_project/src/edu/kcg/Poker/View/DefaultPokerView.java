package edu.kcg.Poker.View;

import edu.kcg.Poker.Chair;
import edu.kcg.Poker.GameRules;
import edu.kcg.Poker.HandChecker;
import edu.kcg.Poker.Table;

public class DefaultPokerView extends PokerView {

	public DefaultPokerView(Table table) {
		super(table);
	}

	@Override
	public void communityCardStatus() {
		for (int card : table.getCommunityCards()) {
			if (card == -1) {
				System.out.print(Messages.getString("PokerGame.0")); //$NON-NLS-1$
			} else {
				System.out
						.print(Messages.getString("PokerGame.1") + (card % 13 + 1) + Messages.getString("PokerGame.2") //$NON-NLS-1$ //$NON-NLS-2$
								+ Table.MARK[card / 13]
								+ Messages.getString("PokerGame.3")); //$NON-NLS-1$
			}
		}
		System.out.println();
	}

	@Override
	public void lastPlayStatus(int index) {
		Chair chair = table.getChairs().get(index);
		int lastplay = chair.getLastPlay();
		String stringLastPlay = String.valueOf(lastplay);
		if (chair.isFold()) {
			stringLastPlay = Messages.getString("PokerGame.30"); //$NON-NLS-1$
		} else if (chair.isAllin()) {
			stringLastPlay = Messages.getString("PokerGame.31"); //$NON-NLS-1$
		} else if (lastplay == 0) {
			stringLastPlay = Messages.getString("PokerGame.32"); //$NON-NLS-1$
		} else {
			stringLastPlay = Messages.getString("PokerGame.33") + String.valueOf(lastplay) + Messages.getString("PokerGame.34"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		System.out.println(index
				+ Messages.getString("PokerGame.35") + stringLastPlay); //$NON-NLS-1$
	}

	@Override
	public void phaseLast() {
		System.out.println(Messages.getString("PokerGame.10")); //$NON-NLS-1$
	}

	@Override
	public void phaseStatus(int status) {
		switch (status) {
		case GameRules.FIRST:
			System.out.println(Messages.getString("PokerGame.6")); //$NON-NLS-1$
			break;
		case GameRules.HUMAN:
			System.out.println(Messages.getString("PokerGame.7")); //$NON-NLS-1$
			break;
		case GameRules.CHANCE:
			System.out.println(Messages.getString("PokerGame.8")); //$NON-NLS-1$
			break;
		case GameRules.FINAL:
			System.out.println(Messages.getString("PokerGame.9")); //$NON-NLS-1$
			break;
		}
	}

	@Override
	public void playerBankroll(int index) {
		Chair chair = table.getChairs().get(index);
		System.out.println(index
				+ Messages.getString("PokerGame.5") + chair.getBankroll()); //$NON-NLS-1$		
	}

	@Override
	public void playerHands(int index) {
		Chair chair = table.getChairs().get(index);
		int hands = chair.getHands();
		int hand = chair.getHand();
		// ハンドを２枚に分解。
		int handl = (hands & HandChecker.HAND_L) >> 6;
		int handr = (hands & HandChecker.HAND_R);
		// それぞれ数字とマークに分解。
		int handln = handl % 13 + 1;
		char handlm = Table.MARK[handl / 13];
		int handrn = handr % 13 + 1;
		char handrm = Table.MARK[handr / 13];
		System.out.printf(
				Messages.getString("PokerGame.4"), index, handln, handlm, //$NON-NLS-1$
				handrn, handrm, hand);
	}

	@Override
	public void playerStatus(int index) {
		Chair chair = table.getChairs().get(index);
		int hands = chair.getHands();
		int hand_l = (hands & HandChecker.HAND_L) >> 6;
		int hand_r = (hands & HandChecker.HAND_R);
		System.out
				.println(Messages.getString("PokerGame.15")
						+ table.getPot()
						+ Messages.getString("PokerGame.16")
						+ Messages.getString("PokerGame.17")
						+ table.getMaxRaise() 
						+ Messages.getString("PokerGame.18")
						+ Messages.getString("PokerGame.19")
						+ chair.getAddedBet()
						+ Messages.getString("PokerGame.20") //$NON-NLS-1$ 
						+ Messages.getString("PokerGame.21")
						+ chair.getCurrentRaise()
						+ Messages.getString("PokerGame.22") //$NON-NLS-1$ 
						+ Messages.getString("PokerGame.23") + Messages.getString("PokerGame.24") + (hand_l % 13 + 1) + Messages.getString("PokerGame.25") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ Table.MARK[hand_l / 13]
						+ Messages.getString("PokerGame.26") + Messages.getString("PokerGame.27") + (hand_r % 13 + 1) + Messages.getString("PokerGame.28") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ Table.MARK[hand_r / 13]
						+ Messages.getString("PokerGame.29")); //$NON-NLS-1$
	}

	@Override
	public void setTable(Table table) {
		this.table = table;
	}

}
