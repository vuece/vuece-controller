package com.vuece.controller.service;

public class VueceCommand {
	
	public final static int COMMAND_STOP=0;
	public final static int COMMAND_PREVIOUS=1;
	public final static int COMMAND_NEXT=2;
	public final static int COMMAND_PLAY=3;
	public final static int COMMAND_PAUSE=4;
	public final static int COMMAND_START=5;
	public final static int STATE_BOTH_STOPPED=0;
	public final static int STATE_BOTH_STARTED=1;
	public final static int STATE_PLAYER_STOPPED=2;
	public final static int STATE_PLAYER_STARTED=3;
	private int expectedState;
	private int cmd;
	private boolean hasNext;
	private boolean executed;
	private int startTime;

	public VueceCommand(int cmd) {
		this.cmd=cmd;
		this.executed=false;
		this.startTime=-1;
		switch (cmd) {
		case COMMAND_STOP:
			expectedState=STATE_BOTH_STOPPED;
			hasNext=false;
			break;
		case COMMAND_PREVIOUS:
			expectedState=STATE_BOTH_STARTED;
			hasNext=true;
			break;
		case COMMAND_NEXT:
			expectedState=STATE_BOTH_STARTED;
			hasNext=true;
			break;
		case COMMAND_PLAY:
			expectedState=STATE_BOTH_STARTED;
			hasNext=false;
			break;
		case COMMAND_PAUSE:
			expectedState=STATE_PLAYER_STOPPED;
			hasNext=false;
			break;
		case COMMAND_START:
			expectedState=STATE_BOTH_STARTED;
			hasNext=false;
			break;
		}
	}
	public int getCommand() {
		return this.cmd;
	}
	public boolean hasNext() {
		return hasNext;
	}
	public boolean isExecuted() {
		return this.executed;
	}
	public void setExecuted(boolean b) {
		this.executed=b;
	}
	public int getStartTime() {
		return startTime;
	}
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	public boolean blockNext() {
		// TODO Auto-generated method stub
		return true;
	}
	public int getExpectedState() {
		return expectedState;
	}
	public String getExpectedStateString() {
		switch (expectedState) {
			case STATE_BOTH_STOPPED: return "BOTH_STOPPED";
			case STATE_BOTH_STARTED: return "BOTH_STARTED";
			case STATE_PLAYER_STOPPED: return "PLAYER_STOPPED";
			case STATE_PLAYER_STARTED: return "PLAYER_STARTED";
		}
		return "N/A";
	}
	public String toString() {
		switch (cmd) {
		case COMMAND_STOP:
			return "stop";
		case COMMAND_PREVIOUS:
			return "previous";
		case COMMAND_NEXT:
			return "next";
		case COMMAND_PLAY:
			return "play";
		case COMMAND_PAUSE:
			return "pause";
		case COMMAND_START:
			return "start";
		}
		return "none";
	}
	
}
