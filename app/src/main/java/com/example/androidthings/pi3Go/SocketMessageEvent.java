package com.example.androidthings.pi3Go;

/**
 * Created by kp004 on 2017/2/6.
 */

public class SocketMessageEvent {
    private String mMessage;

    public SocketMessageEvent(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return mMessage;
    }
}