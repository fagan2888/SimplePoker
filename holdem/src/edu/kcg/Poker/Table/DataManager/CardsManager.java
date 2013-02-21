package edu.kcg.Poker.Table.DataManager;

import java.util.Arrays;

public class CardsManager{
	public static final char[] MARK = { 'H', 'C', 'D', 'S' };

	// community card
	private int[] communityCards = new int[5];
	private int communityCardsIndex;

	// Deck
	private int[] deck = new int[52];
	private int deckIndex;

	public CardsManager() {
		communityCardsIndex = 0;
		Arrays.fill(communityCards, -1);

		deckIndex = 0;
		Arrays.fill(deck, 0);
	}

	public int[] getCommunityCards() {
		return communityCards;
	}

	public int getCommunityCardsIndex() {
		return communityCardsIndex;
	}

	public int getDeckIndex() {
		return deckIndex;
	}

	public int popDeck() {
		return deck[deckIndex--];
	}

	public void pushCommunityCards(int x) {
		this.communityCards[communityCardsIndex++] = x;
	}

	public void setCommunityCards(int[] communityCards) {
		this.communityCards = communityCards;
	}

	public void setCommunityCardsIndex(int communityCardsIndex) {
		this.communityCardsIndex = communityCardsIndex;
	}

	public void setDeck(int[] deck) {
		this.deck = deck;
	}

	public void setDeckIndex(int deckIndex) {
		this.deckIndex = deckIndex;
	}

}
