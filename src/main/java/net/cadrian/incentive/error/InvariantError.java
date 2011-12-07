package net.cadrian.incentive.error;

public class InvariantError extends AssertionError {

	private static final long serialVersionUID = -8735944225872006033L;

	public InvariantError(String msg) {
		super(msg);
	}

	public InvariantError(String msg, Throwable t) {
		super(msg, t);
	}

}
