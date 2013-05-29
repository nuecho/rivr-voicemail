/*
 * Copyright (c) 2002-2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.helpers;

import org.hamcrest.*;

import com.nuecho.rivr.voicexml.test.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;

/**
 * Match the last interaction turn made on the channel with a matcher.
 * 
 * @author Nu Echo Inc.
 */
public class DialogueChannelLastInteractionMatcher extends BaseMatcher<VoiceXmlTestDialogueChannel> {
    private Matcher<InteractionTurn> mTurnMatcher;

    public DialogueChannelLastInteractionMatcher(Matcher<InteractionTurn> matcher) {
        mTurnMatcher = matcher;
    }

    @Override
    public boolean matches(Object arg0) {
        if (!(arg0 instanceof VoiceXmlTestDialogueChannel)) { return false; }
        VoiceXmlTestDialogueChannel channel = (VoiceXmlTestDialogueChannel) arg0;

        return mTurnMatcher.matches(channel.getLastInteractionTurn());
    }

    @Override
    public void describeTo(Description arg0) {
        arg0.appendText("last interaction ").appendDescriptionOf(mTurnMatcher);
    }
}
