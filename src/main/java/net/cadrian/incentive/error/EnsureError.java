package net.cadrian.incentive.error;

/**
 * Error thrown when a postcondition is not met
 * 
 * @author cadrian
 */
public class EnsureError extends IncentiveError {

	private static final long serialVersionUID = 2967833574401987016L;

	/**
	 * @param msg
	 */
	public EnsureError(final String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 * @param t
	 */
	public EnsureError(final String msg, final Throwable t) {
		super(msg, t);
	}

}
