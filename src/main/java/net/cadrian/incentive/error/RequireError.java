package net.cadrian.incentive.error;

/**
 * Error thrown when a precondition is not met
 * 
 * @author cadrian
 */
public class RequireError extends IncentiveError {

	private static final long serialVersionUID = -7352645302882679892L;

	/**
	 * @param msg
	 */
	public RequireError(final String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 * @param t
	 */
	public RequireError(final String msg, final Throwable t) {
		super(msg, t);
	}

}
