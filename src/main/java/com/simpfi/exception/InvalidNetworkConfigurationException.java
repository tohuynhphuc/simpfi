package com.simpfi.exception;

/**
 * Exception thrown when the network configuration is invalid or incomplete.
 * This includes missing edges, junctions, lanes, or other required network components.
 */
public class InvalidNetworkConfigurationException extends Exception {

	/** Serial version UID for serialization. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an InvalidNetworkConfigurationException with a message.
	 *
	 * @param message the detail message
	 */
	public InvalidNetworkConfigurationException(String message) {
		super(message);
	}

	/**
	 * Constructs an InvalidNetworkConfigurationException with a message and cause.
	 *
	 * @param message the detail message
	 * @param cause   the cause of the exception
	 */
	public InvalidNetworkConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
}