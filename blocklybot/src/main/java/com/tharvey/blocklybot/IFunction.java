package com.tharvey.blocklybot;

/* Interface for asynchronous block functions */
public interface IFunction {
	boolean isBusy();

	boolean doFunction(String param1, int param2, int param3);
}
