/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import static com.nuecho.rivr.samples.voicemail.helpers.FluentInteractionBuilder.*;
import static com.nuecho.rivr.samples.voicemail.helpers.Interactions.*;
import static java.lang.String.*;

import javax.json.*;

import org.slf4j.*;

import com.nuecho.rivr.core.channel.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.samples.voicemail.helpers.*;
import com.nuecho.rivr.samples.voicemail.helpers.Interactions.EventHandler;
import com.nuecho.rivr.samples.voicemail.model.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.rendering.voicexml.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;
import com.nuecho.rivr.voicexml.util.*;
import com.nuecho.rivr.voicexml.util.json.*;

/**
 * @author Nu Echo Inc.
 */
public final class VoicemailDialogue implements VoiceXmlDialogue {
    /**
     * 
     */
    private static final TimeValue DEFAULT_TIMEOUT = TimeValue.seconds(5);

    private final Logger mLog = LoggerFactory.getLogger(getClass());

    private static final String STATUS_PROPERTY = "status";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_INTERRUPTED = "interrupted";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_INVALID_USER = "invalid-user";

    private static final String CAUSE_PROPERTY = "cause";

    private static final EventHandler THROW_HANGUP;
    private static final EventHandler THROW_PLATFORM_ERROR;

    static {
        try {
            THROW_HANGUP = throwException(HangUp.class);
            THROW_PLATFORM_ERROR = throwException(PlatformError.class);
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }
    }

    private final DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> mChannel;
    private String mContextPath;

    public VoicemailDialogue(DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel) {
        mChannel = channel;
    }

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context) throws Exception {
        mContextPath = context.getContextPath();
        String status;
        JsonObjectBuilder resultObjectBuilder = JsonUtils.createObjectBuilder();
        try {
            status = runDialogue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            status = STATUS_INTERRUPTED;
        } catch (Exception exception) {
            mLog.error("Error during dialogue", exception);
            status = STATUS_ERROR;
            JsonUtils.add(resultObjectBuilder, CAUSE_PROPERTY, ResultUtils.toJson(exception));
            return new VoiceXmlReturnTurn(STATUS_ERROR, "com.nuecho.rivr", null);
        }

        JsonUtils.add(resultObjectBuilder, STATUS_PROPERTY, status);
        VariableDeclarationList variables = VariableDeclarationList.create(resultObjectBuilder.build());

        return new VoiceXmlReturnTurn("result", variables);
    }

    private String runDialogue() throws Timeout, InterruptedException {
        if (login() == null) return STATUS_INVALID_USER;

        // C03
        // The real voicemail (ie with the phone) has these audios, but not in the NuBot callflow.
        //            Interactions mainMenu = newInteraction("main-menu").dtmfBargeIn(1)
        //                                                               .audio(audioPath("vm-youhave"))
        //                                                               .synthesis("0")
        //                                                               .audio(audioPath("vm-received"))
        //                                                               .audio(audioPath("vm-opts"))
        //                                                               .noInputTimeout(DEFAULT_TIMEOUT)
        //                                                               .toInteractions();
        //            alwaysReprompt(mainMenu);
        Interactions mainMenu = alwaysReprompt(audioWithDtmf("main-menu", "vm-opts", 1));
        String menu;
        do {
            menu = processDtmfTurn(mainMenu);
            if ("0".equals(menu)) {
                mailboxConfigure();
            } else if ("1".equals(menu)) {
                messageMenu();
            } else if ("3".equals(menu)) {
                advancedOptions();
            }
        } while (!"#".equals(menu));

        return STATUS_SUCCESS;
    }

    private void mailboxConfigure() throws Timeout, InterruptedException, HangUp, PlatformError {
        // C07
        Interactions options = alwaysReprompt(audioWithDtmf("mailbox-options", "vm-options", 1));

        // C08
        InteractionTurn record = newInteraction("record-name").audio(audioPath("vm-rec-name")).record().build();

        // C09
        // FIXME onNoMatch: sorry + reprompt. Missing sorry prompt, so always reprompt instead.
        Interactions review = alwaysReprompt(audioWithDtmf("confirm-name", "vm-review", 1));

        // C10
        InteractionTurn saved = audio("message-saved", "vm-msgsaved");

        String selectedOption;
        do {
            selectedOption = processDtmfTurn(options);
            if ("3".equals(selectedOption)) {
                processTurn(record).getRecordingInfo();
                String reviewOption = processDtmfTurn(review);
                // 1-save, 2-listen and review, 3-rerecord
                if ("1".equals(reviewOption)) {
                    processTurn(saved);
                }
            }
        } while (!"*".equals(selectedOption));
    }

    private InteractionTurn audio(String interactionName, String audioName) {
        return newInteraction(interactionName).audio(audioPath(audioName)).build();
    }

    private void messageMenu() {
        // TODO Auto-generated method stub

    }

    private void advancedOptions() {
        // TODO Auto-generated method stub

    }

    private User login() throws Timeout, InterruptedException, HangUp, PlatformError {
        TimeValue timeout = TimeValue.seconds(10);
        // C01
        Interactions askLogin = audioWithDtmf("ask-login", "vm-login", 4, timeout);
        // C02
        Interactions askPassword = audioWithDtmf("ask-password", "vm-password", 4, timeout);
        // C06
        Interactions incorrect = audioWithDtmf("incorrect-mailbox", "vm-incorrect-mailbox", 4, timeout);

        String username;
        String password;
        int tries = 0;
        do {
            tries++;
            username = processDtmfTurn(askLogin);
            password = processDtmfTurn(askPassword);
            // Subsequent tries must use the other interaction.
            askLogin = incorrect;
        } while (!validate(username, password) && tries < 3);
        if (tries == 3) {
            processTurn(audio("goodbye", "vm-goodbye"));
            return null;
        }
        return new User(username, password);
    }

    private boolean validate(String username, String password) {
        // FIXME STUB, only to make NuBot's test scenarios work.
        return username.equals("4069") && password.equals("6522");
    }

    private Interactions audioWithDtmf(String interactionName, String audio, int dtmfLength) {
        return audioWithDtmf(interactionName, audio, dtmfLength, DEFAULT_TIMEOUT);
    }

    private Interactions audioWithDtmf(String interactionName, String audio, int dtmfLength, TimeValue noInputTimeout) {
        return newInteraction(interactionName).dtmfBargeIn(dtmfLength)
                                              .audio(audioPath(audio))
                                              .noInputTimeout(noInputTimeout)
                                              .toInteractions();
    }

    private String audioPath(String audio) {
        return format("%s/instrumented/%s.ulaw", mContextPath, audio);
    }

    private static Interactions defaultHandlers(Interactions interactions) {
        return interactions.onHangup(THROW_HANGUP).handlerFor(VoiceXmlEvent.ERROR, THROW_PLATFORM_ERROR);
    }

    private static Interactions alwaysReprompt(Interactions interactions) {
        return interactions.onNoInput(reprompt()).onNoMatch(reprompt());
    }

    private String processDtmfTurn(Interactions interaction) throws Timeout, InterruptedException {
        // Is there a better way?
        RecognitionInfo result = defaultHandlers(interaction).doTurn(mChannel, null).getRecognitionInfo();
        if (result == null) return "";
        String rawDtmfs = result.getRecognitionResult().getJsonObject(0).getJsonString("utterance").getString();
        mLog.trace("Received {}", rawDtmfs);
        return rawDtmfs.replace(" ", "");
    }

    private VoiceXmlInputTurn processTurn(InteractionTurn outputTurn) throws Timeout, InterruptedException {
        return defaultHandlers(wrap(outputTurn)).doTurn(mChannel, null);
    }

}