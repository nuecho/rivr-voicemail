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
        //                mDialogueChannel.dumpLogs();
        startDialogue(new VoiceXmlFirstTurn());
    }

    @Test
    public void loginSuccess() {
        assertLastInteractionName("ask-login");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("6522");
        assertLastInteractionName("main-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void loginIncorrect() throws Exception {
        assertLastInteractionName("ask-login");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("4243");
        assertLastInteractionName("incorrect-mailbox");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("6522");
        assertLastInteractionName("main-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void exitOnPound() throws Exception {
        login();
        sendDtmfAnswer("#");
        assertLastInteractionName("good-bye");
        mDialogueChannel.processNoInput();
        assertThat(mDialogueChannel, isDone());
    }

    @Test
    public void changeMailboxName() throws Exception {
        login();
        sendDtmfAnswer("0");
        assertLastInteractionName("mailbox-options");
        sendDtmfAnswer("3");
        assertLastInteractionName("record-name");
        sendRecording();
        assertLastInteractionName("confirm-name");
        sendDtmfAnswer("1");
        assertLastInteractionName("message-saved");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("mailbox-options");
        sendDtmfAnswer("*");
        assertLastInteractionName("main-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void dialOut() throws Exception {
        login();
        sendDtmfAnswer("3");
        assertLastInteractionName("advanced-options");
        sendDtmfAnswer("4");
        assertLastInteractionName("ask-number-to-call");
        sendDtmfAnswer("1234"); // don't need to include '#' term char.
        assertLastInteractionName("dial-out");
        mDialogueChannel.processHangup();
    }

    @Test
    public void leaveMessage() throws Exception {
        login();
        sendDtmfAnswer("3");
        assertLastInteractionName("advanced-options");
        sendDtmfAnswer("5");
        assertLastInteractionName("ask-extension");
        sendDtmfAnswer("1234");
        assertLastInteractionName("ask-message");
        sendRecording();
        assertLastInteractionName("main-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void leaveMessageInvalidExtension() throws Exception {
        login();
        sendDtmfAnswer("3");
        sendDtmfAnswer("5");
        sendDtmfAnswer("9999");
        assertLastInteractionName("invalid-extension");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("ask-extension");
        mDialogueChannel.processHangup();
    }

    @Test
    public void listenMessage() throws Exception {
        login();
        sendDtmfAnswer("1");
        assertLastInteractionName("play-message");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void listenMessageExit() throws Exception {
        login();
        sendDtmfAnswer("1");
        mDialogueChannel.processNoInput();
        sendDtmfAnswer("*");
        assertLastInteractionName("main-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void navigateCallPrevious() throws Exception {
        login();
        sendDtmfAnswer("1");
        mDialogueChannel.processNoInput();
        sendDtmfAnswer("4");
        assertLastInteractionName("play-message");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void navigateCallReplay() throws Exception {
        login();
        sendDtmfAnswer("1");
        mDialogueChannel.processNoInput();
        sendDtmfAnswer("5");
        assertLastInteractionName("play-message");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void navigateCallNext() throws Exception {
        login();
        sendDtmfAnswer("1");
        mDialogueChannel.processNoInput();
        sendDtmfAnswer("6");
        assertLastInteractionName("play-message");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mDialogueChannel.processHangup();
    }

    @Test
    public void saveMessage() throws Exception {
        login();
        sendDtmfAnswer("1");
        mDialogueChannel.processNoInput();
        sendDtmfAnswer("9");
        assertLastInteractionName("ask-folder-to-save");
        sendDtmfAnswer("1");
        assertLastInteractionName("message-saved");
        mDialogueChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mDialogueChannel.processHangup();
    }

    private void login() {
        sendDtmfAnswer("4069");
        sendDtmfAnswer("6522");
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

    private void sendRecording() {
        RecordingData recordingData = new RecordingData(new byte[0], "audio/x-wav", "name");
        RecordingInfo recordingInfo = new RecordingInfo(recordingData, TimeValue.seconds(5), false, "#");
        mDialogueChannel.processRecording(recordingInfo);
    }

    private void assertLastInteractionName(String interactionName) {
        assertThat(mDialogueChannel, lastInteractionNameIs(interactionName));
    }

    @After
    public void terminate() {
        mDialogueChannel.dispose();
    }
}
