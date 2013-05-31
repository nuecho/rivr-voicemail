 /*
  * Copyright (c) 2002-2013 Nu Echo Inc.  All rights reserved. 
  */

package com.nuecho.rivr.samples.voicemail.helpers;

import org.hamcrest.*;

import com.nuecho.rivr.core.channel.synchronous.step.*;
import com.nuecho.rivr.voicexml.test.*;

/**
  * @author Nu Echo Inc.
  */
public final class DialogueChannelIsdoneMatcher extends BaseMatcher<VoiceXmlTestDialogueChannel> {
    @Override
    public boolean matches(Object arg0) {
        if (!(arg0 instanceof VoiceXmlTestDialogueChannel)) { return false; }
        VoiceXmlTestDialogueChannel channel = (VoiceXmlTestDialogueChannel) arg0;
        
        return channel.getLastStep() instanceof LastTurnStep;
    }

    @Override
    public void describeTo(Description arg0) {
        arg0.appendText("channel is done");
    }
}