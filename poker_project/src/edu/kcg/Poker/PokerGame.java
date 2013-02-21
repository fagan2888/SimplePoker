package edu.kcg.Poker;

import java.util.ArrayList;
import java.util.Vector;

import edu.kcg.Poker.Common.DivideSolver;
import edu.kcg.Poker.Strategy.AdaptStrategy;
import edu.kcg.Poker.View.DefaultPokerLogger;
import edu.kcg.Poker.View.PokerGameLogger;

/**
 * テーブルのデータを処理し、ゲームを定義するクラス.
 * 
 * @author Shun.S
 * 
 */
public class PokerGame implements GameRules, Runnable {

	private PokerGameLogger logger;
	private Table table;
	
	
	public PokerGame() {
		createTable();
	}

	public PokerGame(PokerGameLogger view) {
		createTable(view);
	}
	
	public Table createTable() {
		Table table = new Table();
		this.table = table;
		if (this.logger == null) {
			this.logger = new DefaultPokerLogger(this.table);
		}
		this.logger.setTable(this.table);
		return this.table;
	}

	public Table createTable(PokerGameLogger view) {
		Table table = new Table();
		this.table = table;
		this.logger = view;
		this.logger.setTable(this.table);
		return this.table;
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
		int round = updateRound(table.getRound());

		dealCommunityCard(round);

		/****************************/
		logger.communityCardStatus();
		/*****************************/

		if (notFolder == 1 || (notFolder - countAllin) < 2) {
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
			logger.phaseNameStatus(status);
			/******************/

			gameGraph(status);

			if (table.chairSize() == 1) {
				break;
			}

			nextPhase();

			/**************/
			logger.lastPhaseStatus();
			/**************/
		}
	}

	@Override
	public void finalPhase() {

		divideProfit();

		/***************/
		logger.communityCardStatus();
		/***************/

		// firstフェーズに戻るためにポットを初期化。
		table.setPot(0);
	}

	@Override
	public void firstPhase() {
		int[] blind = decideBlind();
		int small = blind[0];
		int big = blind[1];
		payBlind(small, big);
	}

	@Override
	public int gameStatus() {

		if (isFirst()) {
			return GameRules.FIRST;
		}

		if (isFinal()) {
			return GameRules.FINAL;
		}

		if (isHuman()) {
			return GameRules.HUMAN;
		}

		if (updateChance()) {
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

		/******************/
		logger.playerStatus(index);
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
		logger.lastPlayStatus(index);
		/****************************/
	}

	@Override
	public int nextPhase() {
		int currentPhase = this.gameStatus();
		updateCurrentPlayer(currentPhase);
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
			chair.setFold(false);
			chair.setHands(0);
			chair.setAddedBet(0);
		}
	}

	private int countAllin() {
		int count = 0;
		for (Chair chair : this.table.getChairs()) {
			if (chair.getBankroll() == 0/* isallin */) {
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

	/**
	 * コミュニティカードを配る。
	 * 
	 * @param round
	 */
	private void dealCommunityCard(int round) {
		// ラウンド1ならデックからコミュニティカードに3枚出す。
		// ラウンド2,3なら、デックからコミュニティカードに1枚出す。
		// ラウンド4なら何もしない。
		if (round == Table.FLOP) {
			for (int i = 0; i < 3; i++) {
				int card = table.popDeck();
				table.pushCommunityCards(card);
			}
		} else if (round == Table.TURN || round == Table.RIVER) {
			int card = table.popDeck();
			table.pushCommunityCards(card);
		} else if (round == Table.SHOWDOWN) {
		}
	}

	private int[] decideBlind() {
		int[] blind = new int[2];

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
		blind[0] = small;
		blind[1] = big;

		return blind;
	}

	/**
	 * 利益の分配。
	 */
	private void divideProfit() {

		DivideSolver solver = new DivideSolver(this);
		int maxAddedBet = 0;
		int sumWinner = 0;
		int max = 0;
		int pot = 0;
		int chairSize = table.chairSize();
		int[] handrolls = new int[chairSize];
		boolean[] winners = new boolean[chairSize];

		handrolls = solver.solveHandrolls();
		max = solver.solveMaxHandroll(handrolls);
		winners = solver.solveWinner(max, handrolls);
		sumWinner = solver.solveSumWinnersBet(winners);
		maxAddedBet = solver.solveMaxAddedBet(winners);
		
		pot = solver.backOverRaise(maxAddedBet, winners);
		solver.divideProfit(pot, sumWinner, winners);

		logger.playersHandsStatus();
		logger.playersHandRollStatus(handrolls);
		logger.playersBankrollStatus();
		
	}

	private void finalize(int status) {
		if (status == GameRules.FINAL) {
			initGame();
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
		updateChairs();
		chairsInit();
		tableInit();
		shuffle();
		deal();
		nextDealer();
	}

	private boolean isFinal() {
		if (table.getRound() == Table.SHOWDOWN) {
			return true;
		}

		return false;
	}

	private boolean isFirst() {
		if (table.getPot() == 0) {
			return true;
		}

		return false;
	}

	private boolean isHuman() {
		ArrayList<Chair> chairs = table.getChairs();
		// あるプレイヤーがオールインもフォルドもしていない場合、
		// 全員の掛け金の最大と同じ額を賭けていなければ人為手番。
		// または、最終プレイの値がIntegerの最小値になっていたら人為手番。
		for (Chair chair : chairs) {
			boolean b1 = chair.getCurrentRaise() != table.getMaxRaise();
			boolean b2 = !(chair.isAllin() || chair.isFold());
			boolean b3 = chair.getLastPlay() == Integer.MIN_VALUE;
			if (b1 && b2 || b3) {
				return true;
			}
		}
		return false;
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

	private void payBlind(int small, int big) {
		int anty = table.getAnty();
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

	private void updateChairs() {
		ArrayList<Chair> chairs = this.table.getChairs();
		Vector<Chair> buffer = new Vector<Chair>();

		for (Chair chair : chairs) {
			if (chair.getBankroll() < table.getAnty() * 2) {
				buffer.add(chair);
			}
		}

		for (Chair removeItem : buffer) {
			chairs.remove(removeItem);
		}

	}

	private boolean updateChance() {
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
			return true;
		}
		return false;
	}

	private void updateCurrentPlayer(int currentPhase) {
		int currentPlayer = table.getCurrentPlayer();
		int playerNum = table.chairSize();
		if (currentPhase == GameRules.HUMAN || currentPhase == GameRules.FIRST) {
			currentPlayer++;
			if (currentPlayer > playerNum - 1) {
				currentPlayer = 0;
			}
		} else {
			currentPlayer = table.getDealer();
		}
		table.setCurrentPlayer(currentPlayer);
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
