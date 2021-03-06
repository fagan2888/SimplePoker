package edu.kcg.Poker.Tester;

import edu.kcg.Poker.Common.HandChecker;

/**
 * 役判定モジュールのテスター。
 */
public class HandCheckerBehaviorTester {
	public static void main(String[] args) {

		// コミュニティカード
		int[] com = { 1, 3, 4, 2, 11 };

		// プレイヤーの手札
		int handl = 10;
		int handr = 5;

		handl <<= 6;

		System.out.println(HandChecker.checkHand(handl | handr, com));

	}
}
