/*
 * Copyright (c) 2002-2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.helpers;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import com.nuecho.rivr.core.channel.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.interaction.*;

/**
 * @author Nu Echo Inc.
 */
public final class Interactions {
    private final InteractionTurn mTurn;
    private final Map<String, EventHandler> mHandlers;

    private Interactions(InteractionTurn turn) {
        mTurn = turn;
        mHandlers = new TreeMap<String, EventHandler>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                /*
                 * Compare the two string in reverse order: we want the iteration order 
                 * to be from the most specific event to the least specific. The event handler
                 * for "error.badfetch.http" must be invoked before "error.badfetch" and "error".
                 * The iteration order of two unrelated event is unimportant.
                 */
                return String.CASE_INSENSITIVE_ORDER.compare(o2, o1);
            }
        });
    }

    public static Interactions wrap(InteractionTurn turn) {
        return new Interactions(turn);
    }

    /**
     * Utility method for adding a noMatch event handler. Same as calling
     * <code>handlerFor(VoiceXmlEvent.NO_MATCH, noMatchHandler)</code>, but much
     * more readable.
     * 
     * @param noMatchHandler The event handler.
     * @see #handlerFor(String, EventHandler)
     */
    public Interactions onNoMatch(EventHandler noMatchHandler) {
        return handlerFor(VoiceXmlEvent.NO_MATCH, noMatchHandler);
    }

    /**
     * Utility method for adding a noInput event handler. Same as calling
     * <code>handlerFor(VoiceXmlEvent.NO_INPUT, noInputHandler)</code>, but much
     * more readable.
     * 
     * @param noInputHandler The event handler.
     * @see #handlerFor(String, EventHandler)
     */
    public Interactions onNoInput(EventHandler noInputHandler) {
        return handlerFor(VoiceXmlEvent.NO_INPUT, noInputHandler);
    }

    /**
     * Utility method for adding an hangup event handler. Same as calling
     * <code>handlerFor(VoiceXmlEvent.CONNECTION_DISCONNECT_HANGUP, noInputHandler)</code>
     * , but much more readable.
     * 
     * @param hangupHandler The event handler.
     * @see #handlerFor(String, EventHandler)
     */
    public Interactions onHangup(EventHandler hangupHandler) {
        return handlerFor(VoiceXmlEvent.CONNECTION_DISCONNECT_HANGUP, hangupHandler);
    }

    /**
     * Add an event handler for the specified prefix.
     * 
     * @param prefix The VoiceXml event prefix.
     * @param handler The event handler.
     * @see VoiceXmlEvent
     */
    public Interactions handlerFor(String prefix, EventHandler handler) {
        mHandlers.put(prefix, handler);
        return this;
    }

    public static EventHandler reprompt() {
        return new RepromptEventHandler();
    }

    public static EventHandler throwException(Class<? extends RuntimeException> clazz) throws NoSuchMethodException {
        return new ThrowExceptionEventHandler(clazz);
    }

    //    public static EventHandler reprompt(int max){
    //        return new RepromptEventHandler(max);
    //    }

    /**
     * Execute the interaction on the specified channel and handle events which
     * have an event handler defined. This can cause more turns to be executed
     * on the channel.
     * 
     * @param channel The channel for turn execution
     * @param timeout The execution timeout for any turn executed in the scope
     *            of this method.
     * @return The final answer after events have been handled.
     * @throws Timeout
     * @throws InterruptedException
     */
    public VoiceXmlInputTurn doTurn(DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel, TimeValue timeout)
            throws Timeout, InterruptedException {
        VoiceXmlInputTurn turn = channel.doTurn(mTurn, timeout);
        boolean handleEvent = true;
        while (handleEvent) {
            handleEvent = false;
            for (Entry<String, EventHandler> entry : mHandlers.entrySet()) {
                if (VoiceXmlEvent.hasEvent(entry.getKey(), turn.getEvents())) {
                    VoiceXmlEvent event = getEvent(entry.getKey(), turn.getEvents());
                    turn = entry.getValue().forTurn(mTurn, turn).handle(event, channel, timeout);
                    handleEvent = true;
                    break;
                }
            }
        }
        return turn;
    }

    private VoiceXmlEvent getEvent(String key, List<VoiceXmlEvent> events) {
        for (VoiceXmlEvent event : events) {
            if (event.isSubtypeOf(key)) { return event; }
        }
        return null;
    }

    /**
     * Simple event handler that will re-execute the same
     * {@link InteractionTurn}. Useful for noMatch and noInput
     * 
     * @author Nu Echo Inc.
     */
    private static final class RepromptEventHandler implements EventHandler {
        private InteractionTurn mTurn;

        @Override
        public EventHandler forTurn(InteractionTurn turn, VoiceXmlInputTurn initialAnswer) {
            mTurn = turn;
            return this;
        }

        @Override
        public VoiceXmlInputTurn handle(VoiceXmlEvent event,
                                        DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel,
                                        TimeValue timeout) throws Timeout, InterruptedException {
            return channel.doTurn(mTurn, timeout);
        }
    }

    private static final class ThrowExceptionEventHandler implements EventHandler {
        private Constructor<? extends RuntimeException> mConstructor;

        public ThrowExceptionEventHandler(Class<? extends RuntimeException> clazz) throws NoSuchMethodException {
            mConstructor = clazz.getConstructor(String.class);
        }

        @Override
        public EventHandler forTurn(InteractionTurn turn, VoiceXmlInputTurn initialAnswer) {
            return this;
        }

        @Override
        public VoiceXmlInputTurn handle(VoiceXmlEvent event,
                                        DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel,
                                        TimeValue timeout) throws Timeout, InterruptedException {
            RuntimeException toThrow;
            try {
                toThrow = mConstructor.newInstance(event.getMessage());
                toThrow.fillInStackTrace();
            } catch (InvocationTargetException exception){
                throw new RuntimeException(exception.getCause());
            } catch (Throwable exception) {
                throw new RuntimeException(exception);
            } 
            throw toThrow;
        }
    }

    public interface EventHandler {
        /**
         * Contextualize the event handler for the executed
         * {@link InteractionTurn} and its {@link VoiceXmlInputTurn} answer.
         * <p>
         * Implementation are free to construct a new instance or set fields.
         * 
         * @param turn
         * @param initialAnswer
         * @return
         */
        EventHandler forTurn(InteractionTurn turn, VoiceXmlInputTurn initialAnswer);

        /**
         * Handle the event. The channel and timeout are passed as a convenience
         * to allow implementation to execute more turns.
         * 
         * @param event
         * @param channel
         * @param timeout
         * @return
         * @throws Timeout
         * @throws InterruptedException
         */
        VoiceXmlInputTurn handle(VoiceXmlEvent event,
                                 DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> channel,
                                 TimeValue timeout) throws Timeout, InterruptedException;
    }
}
