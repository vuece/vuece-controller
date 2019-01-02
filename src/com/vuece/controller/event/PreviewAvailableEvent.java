package com.vuece.controller.event;

public class PreviewAvailableEvent {
	private String path;
	public PreviewAvailableEvent(String path){
		this.path=path;
	}
	public String getPath(){
		return path;
	}
}
