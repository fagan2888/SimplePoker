package edu.kcg.Poker;

public class DefaultStrategy extends AdaptStrategy {

	@Override
	public void setParams(Params params) {
		// TODO 自動生成されたメソッド・スタブ
		this.params = params;
	}

	@Override
	public int solveRaise() {
		return 0;
	}

}
