package com.vuece.controller.event;

public class PlayerProgressEvent {
	private int progress;
	public PlayerProgressEvent(int progress){
		this.progress=progress;
	}
	public int getProgress() {
		return progress;
	}
	
}
