 /*
  * Copyright (c) 2002-2013 Nu Echo Inc.  All rights reserved. 
  */

package com.nuecho.rivr.samples.voicemail.helpers;

import org.hamcrest.*;

import com.nuecho.rivr.voicexml.test.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;

/**
 * @author Nu Echo Inc.
 */
public final class DialogueMatchers {
    private DialogueMatchers()  {}
    
    public static Matcher<InteractionTurn> nameIs(String name){
        return new InteractionTurnNameMatcher(name);
    }
    
    public static Matcher<VoiceXmlTestDialogueChannel> lastInteractionNameIs(String name){
        return new DialogueChannelLastInteractionNameMatcher(nameIs(name));
    }
}
