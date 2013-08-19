/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import static com.nuecho.rivr.samples.voicemail.helpers.DialogueMatchers.*;

import org.junit.*;

import com.nuecho.rivr.core.dialogue.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.util.json.*;

/**
 * @author Nu Echo Inc.
 */
public class DialogueTests {

    @Rule
    private TestDialogueChannel mChannel;

    public DialogueTests() throws DialogueFactoryException {
        mChannel = new TestDialogueChannel(new SimpleVoiceXmlDialogueFactory(VoicemailDialogue.class));
    }
    
    @Test
    public void loginSuccess() {
        assertLastInteractionName("ask-login");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("6522");
        assertLastInteractionName("main-menu");
        mChannel.processHangup();
    }

    @Test
    public void loginIncorrect() {
        assertLastInteractionName("ask-login");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("4243");
        assertLastInteractionName("incorrect-mailbox");
        sendDtmfAnswer("4069");
        assertLastInteractionName("ask-password");
        sendDtmfAnswer("6522");
        assertLastInteractionName("main-menu");
        mChannel.processHangup();
    }

    @Test
    public void exitOnPound() {
        login();
        sendDtmfAnswer("#");
        assertLastInteractionName("good-bye");
        mChannel.processNoInput();
        mChannel.checkThat(isDone());
    }

    @Test
    public void changeMailboxName() {
        login();
        sendDtmfAnswer("0");
        assertLastInteractionName("mailbox-options");
        sendDtmfAnswer("3");
        assertLastInteractionName("record-name");
        sendRecording();
        assertLastInteractionName("confirm-name");
        sendDtmfAnswer("1");
        assertLastInteractionName("message-saved");
        mChannel.processNoInput();
        assertLastInteractionName("mailbox-options");
        sendDtmfAnswer("*");
        assertLastInteractionName("main-menu");
        mChannel.processHangup();
    }

    @Test
    public void dialOut() {
        login();
        sendDtmfAnswer("3");
        assertLastInteractionName("advanced-options");
        sendDtmfAnswer("4");
        assertLastInteractionName("ask-number-to-call");
        sendDtmfAnswer("1234"); // don't need to include '#' term char.
        assertLastInteractionName("dial-out");
        mChannel.processHangup();
    }

    @Test
    public void leaveMessage() {
        login();
        sendDtmfAnswer("3");
        assertLastInteractionName("advanced-options");
        sendDtmfAnswer("5");
        assertLastInteractionName("ask-extension");
        sendDtmfAnswer("1234");
        assertLastInteractionName("ask-message");
        sendRecording();
        assertLastInteractionName("main-menu");
        mChannel.processHangup();
    }

    @Test
    public void leaveMessageInvalidExtension() {
        login();
        sendDtmfAnswer("3");
        sendDtmfAnswer("5");
        sendDtmfAnswer("9999");
        assertLastInteractionName("invalid-extension");
        mChannel.processNoInput();
        assertLastInteractionName("ask-extension");
        mChannel.processHangup();
    }

    @Test
    public void listenMessage() {
        login();
        sendDtmfAnswer("1");
        assertLastInteractionName("play-message");
        mChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mChannel.processHangup();
    }

    @Test
    public void listenMessageExit() {
        login();
        sendDtmfAnswer("1");
        mChannel.processNoInput();
        sendDtmfAnswer("*");
        assertLastInteractionName("main-menu");
        mChannel.processHangup();
    }

    @Test
    public void navigateCallPrevious() {
        login();
        sendDtmfAnswer("1");
        mChannel.processNoInput();
        sendDtmfAnswer("4");
        assertLastInteractionName("play-message");
        mChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mChannel.processHangup();
    }

    @Test
    public void navigateCallReplay() {
        login();
        sendDtmfAnswer("1");
        mChannel.processNoInput();
        sendDtmfAnswer("5");
        assertLastInteractionName("play-message");
        mChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mChannel.processHangup();
    }

    @Test
    public void navigateCallNext() {
        login();
        sendDtmfAnswer("1");
        mChannel.processNoInput();
        sendDtmfAnswer("6");
        assertLastInteractionName("play-message");
        mChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mChannel.processHangup();
    }

    @Test
    public void saveMessage() {
        login();
        sendDtmfAnswer("1");
        mChannel.processNoInput();
        sendDtmfAnswer("9");
        assertLastInteractionName("ask-folder-to-save");
        sendDtmfAnswer("1");
        assertLastInteractionName("message-saved");
        mChannel.processNoInput();
        assertLastInteractionName("call-menu");
        mChannel.processHangup();
    }

    private void login() {
        sendDtmfAnswer("4069");
        sendDtmfAnswer("6522");
    }

    private void sendDtmfAnswer(String dtmfs) {
        StringBuilder builder = new StringBuilder();
        StringUtils.join(builder, dtmfs.toCharArray(), " ");

        // The answer received from a voicexml plateform will have the dtmf digits seperated by spaces in
        // the utterance property and have them joined in the interpretation property.
        mChannel.processDtmfRecognition(builder.toString(), JsonUtils.wrap(dtmfs), null);
    }

    private void sendRecording() {
        RecordingData recordingData = new RecordingData(new byte[0], "audio/x-wav", "name");
        RecordingInfo recordingInfo = new RecordingInfo(recordingData, TimeValue.seconds(5), false, "#");
        mChannel.processRecording(recordingInfo);
    }

    private void assertLastInteractionName(String interactionName) {
        mChannel.checkThat(lastInteractionNameIs(interactionName));
    }
}
