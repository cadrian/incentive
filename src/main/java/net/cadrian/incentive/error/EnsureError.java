package net.cadrian.incentive.error;

public class EnsureError extends AssertionError {

	private static final long serialVersionUID = 2967833574401987016L;

	public EnsureError(String msg) {
		super(msg);
	}

	public EnsureError(String msg, Throwable t) {
		super(msg, t);
	}

}
