package com.vuece.controller.model;

public enum ClientEvent {
	CLIENT_INITIATED(100),
	LOGGING_IN(190),
	LOGGING_OUT(191),
	LOGIN_OK(200),
	LOGOUT_OK(201),
	OPERATION_TIMEOUT(400),
	AUTH_MISSING_PARAM(500),
	AUTH_ERR(501),
	NETWORK_ERR(502),
	SYSTEM_ERR(503);
	private int value;

	private ClientEvent(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
