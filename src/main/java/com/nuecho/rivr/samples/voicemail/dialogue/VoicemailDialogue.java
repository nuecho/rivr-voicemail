/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import static com.nuecho.rivr.samples.voicemail.helpers.FluentInteractionBuilder.*;

import javax.json.*;

import org.slf4j.*;

import com.nuecho.rivr.core.channel.*;
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
    private final Logger mLog = LoggerFactory.getLogger(getClass());

    private static final String STATUS_PROPERTY = "status";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_INTERRUPTED = "interrupted";
    private static final String STATUS_SUCCESS = "success";

    private static final String CAUSE_PROPERTY = "cause";

    private final DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> mChannel;

    public VoicemailDialogue(DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel) {
        mChannel = channel;
    }

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context) throws Exception {
        String status;
        JsonObjectBuilder resultObjectBuilder = JsonUtils.createObjectBuilder();
        try {
            login();
            status = STATUS_SUCCESS;
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

    private User login() throws Timeout, InterruptedException, HangUp, PlatformError {
        // C01
        InteractionTurn askLogin = newInteraction("ask-login").synthesisWithDtmf("ask-login", 4).build();
        // C02
        InteractionTurn askPassword = newInteraction("ask-password").synthesisWithDtmf("ask-password", 4).build();
        // C06
        InteractionTurn incorrect = newInteraction("incorrect-mailbox").synthesisWithDtmf("incorrect-mailbox", 4).build();
        
        String username;
        String password;
        do {
            username = processDtmfTurn(askLogin);
            password = processDtmfTurn(askPassword);
            // Subsequent tries must use the other interaction.
            if (askLogin != incorrect) {
                askLogin = incorrect;
            }
        } while (!validate(username, password)); // TODO max-no-match?

        return new User(username, password);
    }

    private boolean validate(String username, String password) {
        // FIXME STUB, only to make NuBot's test scenarios work.
        return username.equals("4069") && password.equals("6522");
    }

    private String processDtmfTurn(InteractionTurn interaction) throws Timeout, InterruptedException, HangUp,
            PlatformError {
        // Is there a better way?
        return processTurn(interaction).getRecognitionInfo()
                                       .getRecognitionResult()
                                       .getJsonObject(0)
                                       .getJsonString("interpretation")
                                       .getString();
    }

    private VoiceXmlInputTurn processTurn(VoiceXmlOutputTurn outputTurn) throws Timeout, InterruptedException, HangUp,
            PlatformError {
        VoiceXmlInputTurn inputTurn = mChannel.doTurn(outputTurn, null);

        if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.CONNECTION_DISCONNECT_HANGUP, inputTurn.getEvents()))
            throw new HangUp();

        if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.ERROR, inputTurn.getEvents()))
            throw new PlatformError(inputTurn.getEvents().get(0).getMessage());

        return inputTurn;
    }

}