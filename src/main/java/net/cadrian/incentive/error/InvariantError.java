package net.cadrian.incentive.error;

/**
 * Error thrown when an invariant is not met
 * 
 * @author cadrian
 */
public class InvariantError extends IncentiveError {

	private static final long serialVersionUID = -8735944225872006033L;

	/**
	 * @param msg
	 */
	public InvariantError(final String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 * @param t
	 */
	public InvariantError(final String msg, final Throwable t) {
		super(msg, t);
	}

}
