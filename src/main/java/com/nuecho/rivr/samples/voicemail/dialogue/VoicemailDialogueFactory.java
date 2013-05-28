/*
 * Copyright (c) 2012 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.output.*;

/**
 * @author Nu Echo Inc.
 */
public class VoicemailDialogueFactory implements VoiceXmlDialogueFactory {

    @Override
    public VoicemailDialogue create(DialogueInitializationInfo<VoiceXmlInputTurn, VoiceXmlOutputTurn, VoiceXmlDialogueContext> dialogueInitializationInfo) {
        return new VoicemailDialogue(dialogueInitializationInfo.getContext().getDialogueChannel());
    }

}
