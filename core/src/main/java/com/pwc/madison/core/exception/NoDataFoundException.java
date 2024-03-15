package com.pwc.madison.core.exception;
/**
 *
 * Custom exception class for handling the no data found condition.
 *
 */
public class NoDataFoundException extends Exception {

	public NoDataFoundException(String message)
	{
		super(message);
	}
	public NoDataFoundException()
	{
		super();
	}

}
