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
    private static final String RECORDING_LOCATION = "application.recording";
    private final InteractionBuilder mBuilder;
    private DtmfRecognitionConfiguration mDtmfConfig;
    private TimeValue mNoInputTimeout = TimeValue.seconds(10);
    private List<AudioItem> mPromptItems = new ArrayList<AudioItem>();

    private FluentInteractionBuilder(String name) {
        mBuilder = InteractionBuilder.newBuilder(name);
    }

    public static FluentInteractionBuilder newInteraction() {
        return newInteraction(UUID.randomUUID().toString());
    }

    public static FluentInteractionBuilder newInteraction(String id) {
        return new FluentInteractionBuilder(id);
    }

    /**
     * Enable barge-in for subsequent prompts.
     * 
     * @param dtmfLength The number of digits expected.
     */
    public FluentInteractionBuilder dtmfBargeIn(int dtmfLength) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=" + dtmfLength);
        mDtmfConfig = new DtmfRecognitionConfiguration(grammarReference);
        mDtmfConfig.setTermChar("A");
        return this;
    }
    
    /**
     * Enable barge-in for subsequent prompts.
     * 
     * @param termChar The dtmf that ends the recognized input.
     */
    public FluentInteractionBuilder dtmfBargeIn(String termChar) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits");
        mDtmfConfig = new DtmfRecognitionConfiguration(grammarReference);
        mDtmfConfig.setTermChar(termChar);
        return this;
    }

    /**
     * Configure the no-input timeout used for speech or dtmf recognition after
     * prompts have been played.
     * 
     * @param timeout The no-input timeout.
     */
    public FluentInteractionBuilder noInputTimeout(TimeValue timeout) {
        mNoInputTimeout = timeout;
        return this;
    }

    /**
     * Fluent method to add synthesized voice (ie TTS) to the interaction.
     * 
     * @param text The text to be played.
     * @return this instance, for easy chaining.
     */
    public FluentInteractionBuilder synthesis(String text) {
        mPromptItems.add(new SynthesisText(text));
        return this;
    }

    /**
     * Fluent method to add a server-side recording to the interaction.
     * <p>
     * Note that the path should be absolute (ie starts with '/') to be properly
     * fetched by the voicexml platform.
     * 
     * @param path The audio path on the server
     */
    public FluentInteractionBuilder audio(String path) {
        mPromptItems.add(new Recording(path));
        return this;
    }

    /**
     * Replay a previously recored message.
     */
    public FluentInteractionBuilder replayRecording() {
        mPromptItems.add(new ClientSideRecording(RECORDING_LOCATION));
        return this;
    }

    /**
     * Build the currently configured interaction. If barge-in was enabled (
     * {@link #dtmfBargeIn(int)}, the same configuration will be used to do
     * recognition at the end with the timeout specified in
     * {@link #noInputTimeout(TimeValue)} (defaults to 10s).
     * 
     * @return The interaction turn configured via this builder.
     */
    public InteractionTurn build() {
        if (mDtmfConfig != null) {
            mBuilder.addPrompt(mDtmfConfig, null, mPromptItems);
            mBuilder.setFinalRecognition(mDtmfConfig, null, mNoInputTimeout);
        } else {
            mBuilder.addPrompt(mPromptItems);
        }
        return mBuilder.build();
    }

    /**
     * Utility method to wrap the otherwise built {@link InteractionTurn} via
     * {@link #build()} in an {@link Interactions}.
     */
    public Interactions toInteractions() {
        return Interactions.wrap(build());
    }

    /**
     * Work in progress. Perhaps provide a fluent builder for recording
     * configuration too.
     * 
     * @return
     */
    public FluentInteractionBuilder record() {
        RecordingConfiguration recordingConfiguration = new RecordingConfiguration();
        recordingConfiguration.setBeep(true);
        recordingConfiguration.setDtmfTerm(true);
        recordingConfiguration.setType("audio/x-wav");
        recordingConfiguration.setClientSideAssignationDestination(RECORDING_LOCATION);
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=1");
        DtmfRecognitionConfiguration config = new DtmfRecognitionConfiguration(grammarReference);
        recordingConfiguration.setDtmfTermRecognitionConfiguration(config);
        recordingConfiguration.setPostAudioToServer(true);
        mBuilder.setFinalRecording(recordingConfiguration, TimeValue.seconds(10));
        return this;
    }
}
