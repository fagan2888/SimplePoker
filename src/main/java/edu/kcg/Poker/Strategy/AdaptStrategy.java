package edu.kcg.Poker.Strategy;

public abstract class AdaptStrategy implements Strategy {
	protected Params params;

	abstract public void setParams(Params params);
}
