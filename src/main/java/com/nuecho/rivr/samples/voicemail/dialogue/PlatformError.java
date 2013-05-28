/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

/**
 * @author Nu Echo Inc.
 */
public class PlatformError extends Exception {

    private static final long serialVersionUID = 1L;

    public PlatformError() {
        super();
    }

    public PlatformError(String message, Throwable cause) {
        super(message, cause);
    }

    public PlatformError(String message) {
        super(message);
    }

    public PlatformError(Throwable cause) {
        super(cause);
    }

}
