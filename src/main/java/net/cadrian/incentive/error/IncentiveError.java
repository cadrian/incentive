package net.cadrian.incentive.error;

/**
 * General Incentive exception, when a contract is not met.
 * 
 * @author cadrian
 */
public abstract class IncentiveError extends Error {

	private static final long serialVersionUID = 1358399975736870833L;

	/**
	 * @param msg
	 */
	public IncentiveError(final String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 * @param t
	 */
	public IncentiveError(final String msg, final Throwable t) {
		super(msg, t);
	}

}
