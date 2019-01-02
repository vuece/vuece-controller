package com.vuece.controller.event;

public class PlayerStateChangedEvent {
	public int state;
	public int event;
	public PlayerStateChangedEvent(int event, int state){
		this.event=event;
		this.state=state;
	}

}
