package com.vuece.controller.event;

public class ToastMessageEvent {
	private String message;
	public ToastMessageEvent(String message) {
		this.message=message;
	}
	public String getMessage() {
		return message;
	}
	
}
