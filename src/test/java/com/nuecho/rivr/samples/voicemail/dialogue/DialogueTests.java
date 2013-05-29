/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import static com.nuecho.rivr.samples.voicemail.helpers.DialogueMatchers.*;
import static org.junit.Assert.*;

import org.junit.*;
import org.slf4j.*;

import com.nuecho.rivr.core.channel.synchronous.step.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.test.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.util.json.*;

/**
 * @author Nu Echo Inc.
 */
public class DialogueTests {

    private VoiceXmlTestDialogueChannel mDialogueChannel;

    @Before
    public void init() {
        mDialogueChannel = new VoiceXmlTestDialogueChannel("Dialog Tests", TimeValue.minutes(5));
        //        mDialogueChannel.dumpLogs();
        startDialogue(new VoiceXmlFirstTurn());
    }

    @Test
    public void loginSuccess() {
        assertThat(mDialogueChannel, lastInteractionNameIs("ask-login"));
        sendDtmfAnswer("4069");
        assertThat(mDialogueChannel, lastInteractionNameIs("ask-password"));
        sendDtmfAnswer("6522");
        assertThat(mDialogueChannel, lastInteractionNameIs("main-menu"));
        mDialogueChannel.processHangup();
    }

    @Test
    public void loginIncorrect() throws Exception {
        assertThat(mDialogueChannel, lastInteractionNameIs("ask-login"));
        sendDtmfAnswer("4069");
        assertThat(mDialogueChannel, lastInteractionNameIs("ask-password"));
        sendDtmfAnswer("4243");
        assertThat(mDialogueChannel, lastInteractionNameIs("incorrect-mailbox"));
        sendDtmfAnswer("4069");
        assertThat(mDialogueChannel, lastInteractionNameIs("ask-password"));
        sendDtmfAnswer("6522");
        assertThat(mDialogueChannel, lastInteractionNameIs("main-menu"));
        mDialogueChannel.processHangup();
    }

    @Test
    public void changeMailboxName() throws Exception {
        sendDtmfAnswer("4069");
        sendDtmfAnswer("6522");
        sendDtmfAnswer("0");
        assertThat(mDialogueChannel, lastInteractionNameIs("mailbox-options"));
        sendDtmfAnswer("3");
        assertThat(mDialogueChannel, lastInteractionNameIs("record-name"));
        RecordingData recordingData = new RecordingData(new byte[0], "audio/x-wav", "name");
        RecordingInfo recordingInfo = new RecordingInfo(recordingData, TimeValue.seconds(5), false, "#");
        mDialogueChannel.processRecording(recordingInfo);
        // TODO: Assert something about the recording
        assertThat(mDialogueChannel, lastInteractionNameIs("confirm-name"));
        sendDtmfAnswer("1");
        assertThat(mDialogueChannel, lastInteractionNameIs("message-saved"));
        mDialogueChannel.processNoInput();
        assertThat(mDialogueChannel, lastInteractionNameIs("mailbox-options"));
        sendDtmfAnswer("*");
        assertThat(mDialogueChannel, lastInteractionNameIs("main-menu"));
        mDialogueChannel.processHangup();
    }

    private Step<VoiceXmlOutputTurn, VoiceXmlLastTurn> startDialogue(VoiceXmlFirstTurn firstTurn) {
        VoicemailDialogue dialogue = new VoicemailDialogue(mDialogueChannel);
        VoiceXmlDialogueContext context = new VoiceXmlDialogueContext(mDialogueChannel,
                                                                      LoggerFactory.getLogger(getClass()),
                                                                      "x",
                                                                      "contextPath",
                                                                      "servletPath");
        return mDialogueChannel.startDialogue(dialogue, firstTurn, context);
    }

    private void sendDtmfAnswer(String dtmfs) {
        StringBuilder builder = new StringBuilder();
        StringUtils.join(builder, dtmfs.toCharArray(), " ");

        // The usual answer received from a voicexml plateform will have the dtmf digits seperated by spaces in
        // the utterance property and have them joined in the interpretation property.
        mDialogueChannel.processDtmfRecognition(builder.toString(), JsonUtils.wrap(dtmfs), null);
    }

    @After
    public void terminate() {
        mDialogueChannel.dispose();
    }
}
