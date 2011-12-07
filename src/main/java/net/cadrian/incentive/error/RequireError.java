package net.cadrian.incentive.error;

public class RequireError extends IncentiveError {

	private static final long serialVersionUID = -7352645302882679892L;

	public RequireError(String msg) {
		super(msg);
	}

	public RequireError(String msg, Throwable t) {
		super(msg, t);
	}

}
