package com.vuece.controller.event;

public class ClientStateChangedEvent {

	public int event;
	public int state;
	public ClientStateChangedEvent(int event, int state){
		this.state=state;
		this.event=event;
	}
}
