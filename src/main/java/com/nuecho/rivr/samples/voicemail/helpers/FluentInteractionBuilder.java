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
     * Enable barge-in for subsequent prompts.
     * 
     * @param dtmfLength The number of digits expected.
     */
    public FluentInteractionBuilder dtmfBargeIn(int dtmfLength) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=" + dtmfLength);
        mDtmfConfig = new DtmfRecognitionConfiguration(grammarReference);
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
        addPrompt(new SynthesisText(text));
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
        addPrompt(new Recording(path));
        return this;
    }

    /**
     * Replay a previously recored message.
     */
    public FluentInteractionBuilder replayRecording() {
        mBuilder.addPrompt(new ClientSideRecording(RECORDING_LOCATION));
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
            mBuilder.setFinalRecognition(mDtmfConfig, null, mNoInputTimeout);
        }
        return mBuilder.build();
    }

    /**
     * Work in progress. Perhaps provide a fluent builder for recording configuration too.
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

    private void addPrompt(AudioItem... items) {
        if (mDtmfConfig != null) {
            mBuilder.addPrompt(Arrays.asList(items), mDtmfConfig, null);
        } else {
            mBuilder.addPrompt(items);
        }
    }
}
