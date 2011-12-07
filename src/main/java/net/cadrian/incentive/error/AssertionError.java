package net.cadrian.incentive.error;

public abstract class AssertionError extends Error {

	private static final long serialVersionUID = 1358399975736870833L;

	public AssertionError(String msg) {
		super(msg);
	}

	public AssertionError(String msg, Throwable t) {
		super(msg, t);
	}

}
