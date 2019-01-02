package com.vuece.controller.model;

public enum AuthType {
	PASSWORD(1), OAUTH(2);
	private int value;

	private AuthType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static AuthType fromValue(int i) {
		if (i == 2)
			return AuthType.OAUTH;
		return AuthType.PASSWORD;
	}
}
