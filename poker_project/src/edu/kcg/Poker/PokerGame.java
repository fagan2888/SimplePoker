package edu.kcg.Poker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.kcg.Poker.Strategy.AdaptStrategy;
import edu.kcg.Poker.View.DefaultPokerView;
import edu.kcg.Poker.View.PokerView;

/**
 * テーブルのデータを処理し、ゲームを定義するクラス.
 * 
 * @author Shun.S
 * 
 */
public class PokerGame implements GameRules, PokerHand {

	private Table table;
	private PokerView view;

	public PokerGame() {
		view = new DefaultPokerView();
	}

	public PokerGame(PokerView view) {
		this.view = view;
	}

	public PokerGame(Table table) {
		this();
		this.table = table;
	}

	public PokerGame(Table table, PokerView view) {
		this.table = table;
		this.view = view;
	}

	public void addPlayer(Player player) {
		table.addChair(player);
	}

	@Override
	public void chancePhase() {
		int round = table.getRound();
		// ラウンド番号を更新する。
		round++;
		table.setRound(round);
		table.setMaxRaise(0);
		for (Chair chair : table.getChairs()) {
			chair.setCurrentRaise(0);
		}

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

		/****************************/
		view.communityCardStatus(table);
		/*****************************/
	}

	@Override
	public int checkHand(int hands) {
		int[] comcard = table.getCommunityCards();
		int[] cards = { comcard[0], comcard[1], comcard[2], comcard[3],
				comcard[4], (hands & HAND_L) >> 6, hands & HAND_R };
		int bit = 0;
		bit |= checkFlush(cards);
		bit |= checkStraight(cards);
		if (bit > 0) {
			if ((bit & PokerGame.FL) > 0 && (bit & PokerGame.ST) > 0) {
				bit |= PokerGame.SF;
				return bit;
			}
		}
		int buf = bit;
		bit = checkOther(cards);
		if (buf > bit) {
			bit = buf;
		}
		return bit;
	}

	public Table createTable() {
		Table table = new Table();
		this.table = table;
		return table;
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
			if (status == GameRules.FINAL) {
				this.initGame();
				if(table.chairSize()==1){
					break;
				}
			}
			nextPhase();
		}
	}

	@Override
	public void finalPhase() {

		divisionProfit();

		/***************/
		view.communityCardStatus(table);
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
		int i = table.getCurrentPlayer();
		int maxBet = table.getMaxRaise();
		int limit = table.getLimit();
		Chair chair = table.getChairs().get(i);

		/******************/
		view.playerStatus(chair, table);
		/******************/

		// プレイヤーが戦略で使えるパラメータを渡し、選択肢を選択させる。
		AdaptStrategy strategy = (AdaptStrategy) chair.getPlayer()
				.getStrategy();
		strategy.setParams(table.packParams());
		int pastBankroll = chair.getBankroll();
		int option = chair.choice(maxBet, limit);
		if (pastBankroll < option) {
			option = pastBankroll;
		}

		// 選択肢がフォルドでないなら上乗せ分をポットに加算。
		if (option > -1) {
			int bet = option + maxBet;
			if(chair.isAllin()){
				bet -= maxBet;
			}
			table.setMaxRaise(bet);
			addPot(bet - chair.getCurrentRaise());
			chair.setCurrentRaise(bet);
		}

		/****************************/
		view.lastPlayStatus(chair, i);
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

	/**
	 * 各カードのマークを調べる。
	 * 
	 * @param cards
	 * @return
	 */
	private int[] cardMarks(int[] cards) {
		int[] buf = new int[cards.length];
		int i = 0;
		for (int x : cards) {
			buf[i++] = (int) (x / 13);
		}
		return buf;
	}

	/**
	 * 各カードの数字を調べる。
	 * 
	 * @param cards
	 * @return
	 */
	private int[] cardNums(int[] cards) {
		int[] buf = new int[cards.length];
		int i = 0;
		for (int x : cards) {
			buf[i++] = x % 13;
		}
		return buf;
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

	/**
	 * フラッシュかどうかチェック。
	 * 
	 * @param cards
	 * @return
	 */
	private int checkFlush(int[] cards) {
		int i = 0;
		int[] buf = cardMarks(cards);
		while (i < 4) {
			int count = 0;
			int max = 0;
			for (int j = buf.length - 1; j > -1; j--) {
				if (buf[j] == i) {
					++count;
					int num = cards[j] % 13;
					if (max < num) {
						max = num;
					}
				}
			}
			if (count > 4) {
				return max | PokerGame.FL;
			}
			i++;
		}
		return 0;
	}

	/**
	 * 数字の枚数を用いる役のチェッカー。
	 * 
	 * @param cards
	 * @return
	 */
	private int checkOther(int[] cards) {
		// 各数字の枚数をカウントした配列。
		int[] buf = countNums(cards);
		int bit = 0;
		boolean three = false, two = false, one = false, nopair = false;
		for (int i = 12; i > -1; i--) {
			if (buf[i] == 4) {
				// 4ペアなら即リターン
				return (PokerGame.FO | i);
			} else if (buf[i] == 3) {
				if (three && one) {
					// すでにフルハウスが決定しているなら何もしない。
				} else if (one || three) {
					// ワンペアまたはスリーカードが決まっているならフルハウス。
					bit &= PokerGame.HAND_R;
					bit |= PokerGame.FU;
				} else {
					// 役が決まっていないならスリーカード。
					bit = (PokerGame.TH | i);
				}
				// スリーカードのフラグをtrueにする。
				three = true;
			} else if (buf[i] == 2) {
				if (three && one) {
					// すでにフルハウスが決定しているなら何もしない。
				} else if (three) {
					// スリーカードが決まっているならフルハウス。
					bit &= PokerGame.HAND_R;
					bit |= PokerGame.FU;
				} else if (!two) {
					// ツーペアが決まっていない場合。
					if (one) {
						// ワンペアが決まっているならツーペア。
						bit = (bit & PokerGame.HAND_R) << 6;
						bit |= (PokerGame.TW | i);
						// ツーペアのフラグをtrueにする。
						two = true;
					} else {
						// ワンペアが決まっていないならワンペア。
						bit = (PokerGame.ON | i);
					}
				}
				// ワンペアのフラグをtrueにする。
				one = true;
			} else if (buf[i] == 1) {
				if (three || two || one || nopair) {
					// 何かの役が決まっているなら何もしない。
				} else {
					// 役が決まっていないならハイカード。
					bit = i;
					nopair = true;
				}
			}
		}
		// 役の強さを返す。
		return bit;
	}

	/**
	 * ストレートをチェック。
	 * 
	 * @param cards
	 * @return
	 */
	private int checkStraight(int[] cards) {
		int[] buf = cardNums(cards);
		int[] sorted = mysort(buf);

		if (sorted.length < 5)
			return (0);
		if (sorted[0] == 0) {
			buf = Arrays.copyOf(sorted, sorted.length);
			sorted = new int[buf.length + 1];
			int i = 0;
			for (i = 0; i < buf.length; i++) {
				sorted[i] = buf[i];
			}
			sorted[i] = 13;
		}
		int i = sorted.length - 1;
		while (i > 3) {
			if (sorted[i - 4] + 4 == sorted[i])
				return (sorted[i] | PokerGame.ST);
			i--;
		}

		return 0;
	}

	/**
	 * 各数字の枚数を数える。
	 * 
	 * @param cards
	 * @return 数えた回数を配列として返す。
	 */
	private int[] countNums(int[] cards) {
		int[] buf = cardNums(cards);
		int[] cardnum = new int[13];
		Arrays.fill(cardnum, 0);
		for (int x : buf) {
			cardnum[x]++;
		}
		return cardnum;
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
			// 役の強さを計算。
			hand = checkHand(hands);
			chair.setHand(hand);
			// 最大ハンドより大きければ、
			if (hand >= max) {
				max = hand;
			}
			/***************/
			view.playerHands(hands, hand, i);
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
			view.playerBankroll(chair, i);
			/*****/
			i++;
		}
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
	 * 重複を取り除いてソートする。
	 * 
	 * @param cards
	 * @return
	 */
	private int[] mysort(int[] cards) {
		Set<Integer> set = new HashSet<Integer>();
		// 集合クラスは重複を捨てる性質がある.
		for (int x : cards) {
			set.add(x);
		}
		Object[] buf = set.toArray();
		int[] array = new int[buf.length];
		int i = 0;
		for (Object x : buf) {
			array[i] = (Integer) x;
			i++;
		}
		return array;
	}

	/**
	 * 次のディーラーを決定。
	 */
	private int nextDealer() {
		int dealer = table.getDealer();
		int playerNum = table.chairSize();
		dealer++;
		if (dealer > playerNum - 1) {
			dealer = 0;
		}
		return 0;
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
}
