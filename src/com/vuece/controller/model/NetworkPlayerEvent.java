package com.vuece.controller.model;

public enum NetworkPlayerEvent {
	PLAY_COMMAND_SENT(100),
	BUFFERING_NEW_MEDIA(110),
	PLAYER_STARTED(200),
	PLAYER_RESUMED(210),
	PLAYER_PAUSED(220),
	END_OF_SONG(230),
	TIMEOUT(400),
	MEDIA_FILE_NOT_FOUND(500),
	NETWORK_ERR(501);
	private int value;

	private NetworkPlayerEvent(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
