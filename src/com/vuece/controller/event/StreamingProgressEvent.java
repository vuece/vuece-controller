package com.vuece.controller.event;

public class StreamingProgressEvent {
	private int progress;
	public StreamingProgressEvent(int progress){
		this.progress=progress;
	}
	public int getProgress() {
		return progress;
	}

}
