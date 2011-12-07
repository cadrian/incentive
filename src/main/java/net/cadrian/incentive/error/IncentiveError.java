package net.cadrian.incentive.error;

public abstract class IncentiveError extends Error {

	private static final long serialVersionUID = 1358399975736870833L;

	public IncentiveError(String msg) {
		super(msg);
	}

	public IncentiveError(String msg, Throwable t) {
		super(msg, t);
	}

}
