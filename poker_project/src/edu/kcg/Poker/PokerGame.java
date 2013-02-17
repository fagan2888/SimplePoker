package edu.kcg.Poker;

import java.util.ArrayList;

import edu.kcg.Poker.Factory.TableAndViewFactory;
import edu.kcg.Poker.Strategy.AdaptStrategy;
import edu.kcg.Poker.View.PokerView;

/**
 * テーブルのデータを処理し、ゲームを定義するクラス.
 * 
 * @author Shun.S
 * 
 */
public class PokerGame extends TableAndViewFactory implements GameRules,
		Runnable {

	public PokerGame() {
		createTable();
	}

	public PokerGame(PokerView view) {
		createTable(view);
	}

	public PokerGame(Table table) {
		createPokerView(table);
	}

	public PokerGame(Table table, PokerView view) {
		this.table = table;
		this.view = view;
		view.setTable(this.table);
	}

	public void addPlayer(Player player) {
		table.addChair(player);
	}

	@Override
	public void chancePhase() {

		int countFold = countFold();
		int countAllin = countAllin();
		int chairSize = table.chairSize();
		int notFolder = chairSize - countFold;

		if (notFolder == 1) {
			int finalRound = 4;
			int[] communityCards = { 0, 1, 2, 3, 4 };
			table.setCommunityCards(communityCards);
			table.setRound(finalRound);
			return;
		}

		int round = table.getRound();
		round = updateRound(round);
		dealCommunityCard(round);

		/****************************/
		view.communityCardStatus();
		/*****************************/

		if ((notFolder - countAllin) < 1) {
			if (table.getRound() == 4) {
				return;
			}
			chancePhase();
		}
	}

	@Override
	public void execute() {
		int status;
		initGame();

		while (true) {
			status = gameStatus();

			/******************/
			view.phaseStatus(status);
			/******************/

			gameGraph(status);

			nextPhase();

			/**************/
			view.phaseLast();
			/**************/
		}
	}

	@Override
	public void finalPhase() {

		divisionProfit();

		/***************/
		view.communityCardStatus();
		/***************/

		// firstフェーズに戻るためにポットを初期化。
		table.setPot(0);
	}

	@Override
	public void firstPhase() {

		int anty = table.getAnty();
		// スモールブラインドを決定する。
		int small = table.getDealer() + 1;
		if (small > table.chairSize() - 1) {
			small = 0;
		}
		// ビッグブラインドを決定する。
		int big = small + 1;
		if (big > table.chairSize() - 1) {
			big = 0;
		}
		ArrayList<Chair> chairs = table.getChairs();

		// スモールブラインドにアンティを支払わせる。
		Chair smallblind = chairs.get(small);
		smallblind.payAnty(anty);

		// ビッグブラインドにアンティを支払わせる。
		Chair bigblind = chairs.get(big);
		bigblind.payAnty(anty * 2);

		// ポットにアンティを支払い分を追加。
		addPot(anty * 3);

		// レイズの最大値と現在手番のプレイヤーを更新。
		table.setMaxRaise(anty * 2);
		table.setCurrentPlayer(big);
	}

	@Override
	public int gameStatus() {
		int pot = table.getPot();
		int round = table.getRound();
		ArrayList<Chair> chairs = table.getChairs();
		// ポットが0なら最初のフェーズ。
		if (pot == 0) {
			return GameRules.FIRST;
		}
		// ラウンドが4なら最終フェーズ。
		if (round == 4) {
			return GameRules.FINAL;
		}
		// あるプレイヤーがオールインもフォルドもしていない場合、
		// 全員の掛け金の最大と同じ額を賭けていなければ人為手番。
		// または、最終プレイの値がIntegerの最小値になっていたら人為手番。
		for (Chair chair : chairs) {
			boolean b1 = chair.getCurrentRaise() != table.getMaxRaise();
			boolean b2 = !(chair.isAllin() || chair.isFold());
			boolean b3 = chair.getLastPlay() == Integer.MIN_VALUE;
			if (b1 && b2 || b3) {
				return GameRules.HUMAN;
			}
		}
		// 現在手番が偶然手番の時の処理。
		if (table.getCurrentPhase() == GameRules.CHANCE) {
			// 現在手番を人為手番に設定。
			table.setCurrentPhase(GameRules.HUMAN);
			// フォルドしていないかオールインしていない場合、
			// 最終プレイの値をIntegerの最小値に設定する。
			for (Chair chair : table.getChairs()) {
				if (!(chair.isFold() || chair.isAllin())) {
					chair.setLastPlay(Integer.MIN_VALUE);
				}
			}
			// 形式的に偶然手番をリターンする。
			return GameRules.CHANCE;
		}

		// それ以外は偶然手番。
		return GameRules.CHANCE;
	}

	public Table getTable() {
		return table;
	}

	@Override
	public void humanPhase() {

		int index = table.getCurrentPlayer();
		int maxBet = table.getMaxRaise();
		int limit = table.getLimit();
		Chair chair = table.getChairs().get(index);
		int pastBankroll = chair.getBankroll();

		if (pastBankroll == 0) {
			chair.setAllin(true);
		}

		/******************/
		view.playerStatus(index);
		/******************/

		if (!chair.isFold()) {
			int option = 0;
			int chairSize = table.chairSize();
			int countFold = countFold();
			int notFolder = chairSize - countFold;
			int countAllin = countAllin();

			// プレイヤーが戦略で使えるパラメータを渡し、選択肢を選択させる。
			if ((notFolder > 1) && (notFolder - countAllin != 0)) {
				AdaptStrategy strategy = (AdaptStrategy) chair.getPlayer()
						.getStrategy();
				strategy.setParams(table.packParams(index));
				option = chair.choice(maxBet, limit);
			}

			// 選択肢がフォルドでないなら上乗せ分をポットに加算。
			if (option > -1) {
				int bet = option + maxBet;
				if (pastBankroll < bet) {
					bet = pastBankroll + chair.getCurrentRaise();
				}
				table.setMaxRaise(bet);
				addPot(bet - chair.getCurrentRaise());
				chair.setCurrentRaise(bet);
			}
		}
		/****************************/
		view.lastPlayStatus(index);
		/****************************/
	}

	@Override
	public int nextPhase() {
		int currentPlayer = table.getCurrentPlayer();
		int playerNum = table.chairSize();
		int currentPhase = this.gameStatus();

		if (currentPhase == GameRules.HUMAN || currentPhase == GameRules.FIRST) {
			currentPlayer++;
			if (currentPlayer > playerNum - 1) {
				currentPlayer = 0;
			}
		} else {
			currentPlayer = table.getDealer();
		}
		table.setCurrentPlayer(currentPlayer);
		table.setCurrentPhase(currentPhase);

		return currentPhase;
	}

	@Override
	public void run() {
		execute();
	}

	public void setTable(Table table) {
		this.table = table;
	}

	/**
	 * ポットを加算
	 * 
	 * @param x
	 */
	private void addPot(int x) {
		table.addPot(x);
	}

	private void chairsInit() {
		for (Chair chair : table.getChairs()) {
			chair.setLastPlay(0);
			chair.setAllin(false);
			chair.setFold(false);
			chair.setHand(0);
			chair.setHands(0);
			chair.setWinner(false);
			chair.setAddedBet(0);
		}
	}

	private void checkBankrollOverAnty() {
		ArrayList<Chair> chairs = this.table.getChairs();
		for (int i = 0; i < chairs.size(); i++) {
			Chair chair = chairs.get(i);
			int bankroll = chair.getBankroll();
			int doubleAnty = this.table.getAnty() * 2;
			if (bankroll < doubleAnty) {
				chairs.remove(i);
			}
		}
	}

	private int countAllin() {
		int count = 0;
		for (Chair chair : this.table.getChairs()) {
			if (chair.isAllin()) {
				count++;
			}
		}
		return count;
	}

	private int countFold() {
		int count = 0;
		for (Chair chair : this.table.getChairs()) {
			if (chair.isFold()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * カードを配る。
	 */
	private void deal() {
		for (Chair chair : table.getChairs()) {
			int cardLeft = table.popDeck() << 6;
			int cardRight = table.popDeck();
			int hands = (cardLeft | cardRight);
			chair.setHands(hands);
		}
	}

	private void dealCommunityCard(int round) {
		// ラウンド1ならデックからコミュニティカードに3枚出す。
		// ラウンド2,3なら、デックからコミュニティカードに1枚出す。
		// ラウンド4なら何もしない。
		if (round == 1) {
			for (int i = 0; i < 3; i++) {
				int card = table.popDeck();
				table.pushCommunityCards(card);
			}
		} else if (round == 2 || round == 3) {
			int card = table.popDeck();
			table.pushCommunityCards(card);
		} else if (round == 4) {
		}
	}

	/**
	 * 利益の分配。
	 */
	private void divisionProfit() {
		int maxAddedBet = 0;
		int sumWinner = 0;
		int max = 0;
		int hand = 0;
		int i = 0;
		int pot = table.getPot();
		ArrayList<Chair> chairs = table.getChairs();

		// 最大ハンドを計算。
		for (Chair chair : chairs) {
			// ハンドを取得
			int hands = chair.getHands();
			hand = 0;
			int[] comcard = table.getCommunityCards();
			// 役の強さを計算。
			if (!chair.isFold()) {
				hand = HandChecker.checkHand(hands, comcard);
			}
			chair.setHand(hand);
			// 最大ハンドより大きければ、
			if (hand >= max) {
				max = hand;
			}
			/***************/
			view.playerHands(i);
			/***************/

			i++;
		}

		// 勝者を決定。
		for (Chair chair : chairs) {
			if (chair.getHand() == max) {
				chair.setWinner(true);
				sumWinner += chair.getAddedBet();
			}
		}

		// 勝者の掛け金の最大を計算。
		for (Chair chair : chairs) {
			if (chair.isWinner()) {
				int x = chair.getAddedBet();
				if (x > maxAddedBet) {
					maxAddedBet = x;
				}
			}
		}
		// 勝者よりも掛け金が多い敗北プレイヤーに過多分の返上。
		for (Chair chair : chairs) {
			if (!chair.isWinner()) {
				int x = chair.getAddedBet();
				if (x > maxAddedBet) {
					x = chair.getAddedBet() - maxAddedBet;
					pot -= x;
					chair.profit(x);
				}
			}
		}
		i = 0;
		// 勝者に利益を配分。
		// ある勝者の合計掛け金をxとすると、x/sumWinnerが分配される。
		for (Chair chair : chairs) {
			if (chair.isWinner()) {
				int x = chair.getAddedBet();
				int profit = (x * pot) / sumWinner;
				chair.profit(profit);
			}
			/*****/
			view.playerBankroll(i);
			/*****/
			i++;
		}
	}

	private void finalize(int status) {
		if (status == GameRules.FINAL) {
			initGame();
		}
		if (table.chairSize() < 2) {
			System.exit(status);
		}
	}

	private void gameGraph(int status) {
		switch (status) {
		case GameRules.FIRST:
			firstPhase();
			break;
		case GameRules.HUMAN:
			humanPhase();
			break;
		case GameRules.CHANCE:
			chancePhase();
			break;
		case GameRules.FINAL:
			finalPhase();
			break;
		}
		finalize(status);
	}

	private void initGame() {
		checkBankrollOverAnty();
		chairsInit();
		tableInit();
		shuffle();
		deal();
		nextDealer();
	}

	/**
	 * 次のディーラーを決定。
	 */
	private void nextDealer() {
		int dealer = table.getDealer();
		int playerNum = table.chairSize();
		dealer++;
		if (dealer > playerNum - 1) {
			dealer = 0;
		}
		table.setDealer(dealer);
	}

	/**
	 * デックをシャッフル
	 */
	private void shuffle() {
		int[] bufDeck = new int[52];
		for (int i = 0; i < bufDeck.length; i++) {
			bufDeck[i] = i;
		}

		for (int i = bufDeck.length - 1; i > 0; i--) {
			int t = (int) (Math.random() * i);
			int tmp = bufDeck[i];
			bufDeck[i] = bufDeck[t];
			bufDeck[t] = tmp;
		}

		table.setDeck(bufDeck);
	}

	private void tableInit() {
		int[] comcard = new int[5];
		for (int i = 0; i < comcard.length; i++) {
			comcard[i] = -1;
		}
		table.setMaxRaise(0);
		table.setRound(0);
		table.setCommunityCardsIndex(0);
		table.setCommunityCards(comcard);
		table.setDeckIndex(51);
		table.setPot(0);
	}

	private int updateRound(int round) {
		++round;
		table.setRound(round);
		table.setMaxRaise(0);
		for (Chair chair : table.getChairs()) {
			chair.setCurrentRaise(0);
		}
		return round;
	}
}
