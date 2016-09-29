package com.tharvey.blocklybot;

/**
 * Interface for classes to act as a callback for Listen class
 */
public interface IEventListener {
	boolean onEvent(String type, String param);
}
