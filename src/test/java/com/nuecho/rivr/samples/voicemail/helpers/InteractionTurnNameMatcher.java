 /*
  * Copyright (c) 2002-2013 Nu Echo Inc.  All rights reserved. 
  */

package com.nuecho.rivr.samples.voicemail.helpers;

import static java.lang.String.*;

import org.hamcrest.*;

import com.nuecho.rivr.voicexml.turn.output.interaction.*;

/**
  * @author Nu Echo Inc.
  */
public final class InteractionTurnNameMatcher extends BaseMatcher<InteractionTurn> {
    private final String mName;

    /**
     * @param name
     */
    public InteractionTurnNameMatcher(String name) {
        mName = name;
    }

    @Override
    public boolean matches(Object arg0) {
        if (!(arg0 instanceof InteractionTurn)) {
            return false;
        }
        InteractionTurn turn = (InteractionTurn) arg0;
        return turn.getName().equals(mName);
    }

    @Override
    public void describeTo(Description arg0) {
        arg0.appendText(format("name is [%s]", mName));
    }
}