/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

/**
 * @author Nu Echo Inc.
 */
public class HangUp extends Exception {

    private static final long serialVersionUID = 1L;

    public HangUp() {
        super();
    }

    public HangUp(String message, Throwable cause) {
        super(message, cause);
    }

    public HangUp(String message) {
        super(message);
    }

    public HangUp(Throwable cause) {
        super(cause);
    }

}
