package edu.kcg.Poker.Strategy;

public class DefaultStrategy extends AdaptStrategy {

	@Override
	public void setParams(Params params) {
		this.params = params;
	}

	public int solveRaise() {
		return 0;
	}

}
