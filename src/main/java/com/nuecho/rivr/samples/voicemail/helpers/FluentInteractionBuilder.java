/*
 * Copyright (c) 2002-2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.helpers;

import java.util.*;

import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;

/**
 * Fluent-style interaction builder for easier creation of interaction turn.
 * 
 * @author Nu Echo Inc.
 */
public final class FluentInteractionBuilder {
    private final InteractionBuilder mBuilder;

    private FluentInteractionBuilder(String name) {
        mBuilder = new InteractionBuilder(name);
    }

    public static FluentInteractionBuilder newInteraction() {
        return newInteraction(UUID.randomUUID().toString());
    }

    public static FluentInteractionBuilder newInteraction(String id) {
        return new FluentInteractionBuilder(id);
    }

    /**
     * Fluent method to add synthesized voice (ie TTS) to the interaction.
     * 
     * @param text The text to be played.
     * @return this instance, for easy chaining.
     */
    public FluentInteractionBuilder synthesis(String text) {
        mBuilder.addPrompt(new SynthesisText(text));
        return this;
    }

    /**
     * Fluent method to add synthesized voice (ie TTS) to the interaction with
     * dtmf barge-in.
     * 
     * @param text The text to be played.
     * @param dtmfLength The number of dtmf digits to expect.
     * @return this instance, for easy chaining.
     */
    public FluentInteractionBuilder synthesisWithDtmf(String text, int dtmfLength) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=" + dtmfLength);
        DtmfRecognitionConfiguration config = new DtmfRecognitionConfiguration(grammarReference);
        mBuilder.setFinalRecognition(config, null, TimeValue.seconds(5));
        mBuilder.addPrompt(Arrays.asList(new SynthesisText(text)), config, null);
        return this;
    }

    /**
     * @return The interaction turn configured via this builder.
     */
    public InteractionTurn build() {
        return mBuilder.build();
    }
}
