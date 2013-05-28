 /*
  * Copyright (c) 2002-2013 Nu Echo Inc.  All rights reserved. 
  */

package com.nuecho.rivr.samples.voicemail.model;


/**
 * @author Nu Echo Inc.
 */
public final class User {

    private String mUsername;
    private String mPassword;

    public User(String username, String password) {
        mUsername = username;
        mPassword = password;
    }
    
    /**
     * @return the password
     */
    public String getPassword() {
        return mPassword;
    }
    
    /**
     * @return the username
     */
    public String getUsername() {
        return mUsername;
    }
}
