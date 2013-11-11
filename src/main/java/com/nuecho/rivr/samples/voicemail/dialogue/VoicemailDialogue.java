/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.samples.voicemail.dialogue;

import static com.nuecho.rivr.voicexml.turn.OutputTurns.*;
import static java.lang.String.*;

import java.util.regex.*;

import javax.json.*;

import org.slf4j.*;

import com.nuecho.rivr.core.channel.*;
import com.nuecho.rivr.core.util.*;
import com.nuecho.rivr.samples.voicemail.model.*;
import com.nuecho.rivr.voicexml.dialogue.*;
import com.nuecho.rivr.voicexml.turn.*;
import com.nuecho.rivr.voicexml.turn.first.*;
import com.nuecho.rivr.voicexml.turn.input.*;
import com.nuecho.rivr.voicexml.turn.last.*;
import com.nuecho.rivr.voicexml.turn.output.*;
import com.nuecho.rivr.voicexml.turn.output.audio.*;
import com.nuecho.rivr.voicexml.turn.output.grammar.*;
import com.nuecho.rivr.voicexml.util.*;
import com.nuecho.rivr.voicexml.util.json.*;

/**
 * @author Nu Echo Inc.
 */
public final class VoicemailDialogue implements VoiceXmlDialogue {
    private static final Duration DEFAULT_TIMEOUT = Duration.seconds(5);

    private final Logger mLog = LoggerFactory.getLogger(getClass());

    private static final String STATUS_PROPERTY = "status";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_INTERRUPTED = "interrupted";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_INVALID_USER = "invalid-user";

    private static final String CAUSE_PROPERTY = "cause";

    private static final Pattern DNIS = Pattern.compile(".*:([0-9]*)@.*:.*");

    private static final String RECORDING_LOCATION = "application.recording";

    private DialogueChannel<VoiceXmlInputTurn, VoiceXmlOutputTurn> mChannel;
    private String mContextPath;

    private boolean mNuBotMode;

    @Override
    public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context) throws Exception {
        mContextPath = context.getContextPath();
        mChannel = context.getDialogueChannel();
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
        }

        JsonUtils.add(resultObjectBuilder, STATUS_PROPERTY, status);
        VariableList variables = VariableList.create(resultObjectBuilder.build());

        return new Exit("result", variables);
    }

    private String runDialogue() throws Timeout, InterruptedException {
        detectNuBotInstrumentation();

        if (login() == null) return STATUS_INVALID_USER;

        // C03
        DtmfRecognition dtmfConfig = dtmfBargeIn(1);
        Interaction mainMenu = interaction("main-menu").addPrompt(dtmfConfig,
                                                                  audio("vm-youhave"),
                                                                  synthesis("1"),
                                                                  audio("vm-Old"),
                                                                  audio("vm-message"),
                                                                  audio("vm-onefor"),
                                                                  audio("vm-Old"),
                                                                  audio("vm-messages"),
                                                                  audio("vm-opts")).build(dtmfConfig, DEFAULT_TIMEOUT);

        String selection;
        do {
            selection = processDtmfTurn(mainMenu);
            if ("0".equals(selection)) {
                mailboxConfigure();
            } else if ("1".equals(selection)) {
                messageMenu();
            } else if ("3".equals(selection)) {
                advancedOptions();
            }
        } while (!"#".equals(selection));

        // C50
        processTurn(audio("good-bye", "vm-goodbye"));

        return STATUS_SUCCESS;
    }

    private void detectNuBotInstrumentation() throws Timeout, InterruptedException {
        Script clidAndDnisTurn = new Script("clidAndDnis");
        VariableList dnisVariables = new VariableList();
        dnisVariables.addWithExpression("dnis", "session.connection.local.uri");
        clidAndDnisTurn.setVariables(dnisVariables);

        VoiceXmlInputTurn inputTurn = processTurn(clidAndDnisTurn);
        JsonObject result = (JsonObject) inputTurn.getJsonValue();

        if (result != null) {
            String dnis = result.getString("dnis");
            Matcher matcher = DNIS.matcher(dnis);
            if (!matcher.matches()) throw new IllegalArgumentException(format("Received invalid dnis [%s]", dnis));
            String extension = matcher.group(1);
            mNuBotMode = extension.startsWith("495");
            if (mNuBotMode) {
                mLog.info("Running dialogue in NuBot mode (instrumented prompts)");
            }
        }
    }

    private void mailboxConfigure() throws Timeout, InterruptedException, HangUp, PlatformError {
        // C07
        Interaction options = audioWithDtmf("mailbox-options", "vm-options", 1);

        // C08
        Interaction record = record("record-name", "vm-rec-name");

        // C09
        // FIXME onNoMatch: sorry + reprompt. Missing sorry prompt, so always reprompt instead.
        Interaction review = audioWithDtmf("confirm-name", "vm-review", 1);

        // C10
        Message saved = audio("message-saved", "vm-msgsaved");

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

    private Message audio(String interactionName, String audioName) {
        return new Message(interactionName, audio(audioName));
    }

    private void messageMenu() throws Timeout, InterruptedException {
        // C04 first message received "date" from "phone number" recording
        Interaction playMessage = interaction("play-message").addPrompt(audio("vm-first"),
                                                                        audio("vm-message"),
                                                                        audio("vm-received")).build();
        // C05
        Interaction callMenu = audioWithDtmf("call-menu", "vm-advopts", 1);
        // C17 which folder loop press dtmf for foldername messages
        Interaction whichFolder = audioWithDtmf("ask-folder-to-save", "vm-savefolder", 1);
        // C18 message 1 savedto old messages
        Message messageSaved = audio("message-saved", "vm-savedto");
        String menu;
        processTurn(playMessage);
        do {
            menu = processDtmfTurn(callMenu);
            if ("9".equals(menu)) {
                String folderNum = processDtmfTurn(whichFolder);
                if ("1".equals(folderNum)) {// TODO do something with the folder
                    processTurn(messageSaved);
                }
                // TODO usually, 4 means previous, 5 replay and 6 next. just replay same for now.
            } else if ("4".equals(menu) || "5".equals(menu) || "6".equals(menu)) {
                processTurn(playMessage);
            }
        } while (!"*".equals(menu)); // TODO '#' should exit voicemail.
    }

    private void advancedOptions() throws Timeout, InterruptedException {
        // C11
        Interaction advancedMenu = audioWithDtmf("advanced-options", "vm-leavemsg", 1);
        // C12
        Interaction extension = audioWithDtmf("ask-extension", "vm-extension", 4);
        // C14

        Interaction message = record("ask-message", "vm-intro");
        // C15
        DtmfRecognition dtmfConfig = dtmfBargeIn("#");
        Interaction toCall = interaction("ask-number-to-call").addPrompt(dtmfConfig, audio("vm-enter-num-to-call"))
                                                              .build(dtmfConfig, DEFAULT_TIMEOUT);

        // C16
        Message dialOut = audio("dial-out", "vm-dialout");

        String subMenu = processDtmfTurn(advancedMenu);
        if ("4".equals(subMenu)) {
            String numberToCall = processDtmfTurn(toCall);
            if ("1234".equals(numberToCall)) {
                processTurn(dialOut);
                // TODO Transfer
                //            defaultHandlers(wrap(new BlindTransferTurn("dial-out", numberToCall))).doTurn(mChannel, null);
            }
        } else if ("5".equals(subMenu)) {
            String extensionToCall;
            do {
                extensionToCall = processDtmfTurn(extension);
            } while (!validateExtension(extensionToCall));

            mChannel.doTurn(message, null); // what to do with the recording?
        }
    }

    private boolean validateExtension(String extensionToCall) throws Timeout, InterruptedException {
        if (!"1234".equals(extensionToCall)) {
            // C13
            processTurn(audio("invalid-extension", "pbx-invalid"));
            return false;
        }
        return true;
    }

    private User login() throws Timeout, InterruptedException, HangUp, PlatformError {
        Duration timeout = Duration.seconds(10);
        // C01
        Interaction askLogin = audioWithDtmf("ask-login", "vm-login", 4, timeout);
        // C02
        Interaction askPassword = audioWithDtmf("ask-password", "vm-password", 4, timeout);
        // C06
        Interaction incorrect = audioWithDtmf("incorrect-mailbox", "vm-incorrect-mailbox", 4, timeout);

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

    private AudioItem synthesis(String text) {
        return new SpeechSynthesis(text);
    }

    private AudioItem audio(String audioName) {
        return AudioFile.fromLocation(audioPath(audioName));
    }

    private DtmfRecognition dtmfBargeIn(int dtmfLength) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=" + dtmfLength);
        DtmfRecognition dtmfConfig = new DtmfRecognition(grammarReference);
        dtmfConfig.setTermChar("A");
        return dtmfConfig;
    }

    private DtmfRecognition dtmfBargeIn(String termChar) {
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits");
        DtmfRecognition dtmfConfig = new DtmfRecognition(grammarReference);
        dtmfConfig.setTermChar(termChar);
        return dtmfConfig;
    }

    private Interaction audioWithDtmf(String interactionName, String audio, int dtmfLength) {
        return audioWithDtmf(interactionName, audio, dtmfLength, DEFAULT_TIMEOUT);
    }

    private Interaction audioWithDtmf(String interactionName, String audio, int dtmfLength, Duration noInputTimeout) {
        DtmfRecognition dtmfconfig = dtmfBargeIn(dtmfLength);
        return interaction(interactionName).addPrompt(dtmfconfig, audio(audio)).build(dtmfconfig, noInputTimeout);
    }

    private String audioPath(String audio) {
        String promptType = mNuBotMode ? "instrumented" : "original";
        return format("%s/%s/%s.ulaw", mContextPath, promptType, audio);
    }

    private Interaction record(String interactionName, String audio) {
        Recording recordingConfiguration = new Recording();
        recordingConfiguration.setBeep(true);
        recordingConfiguration.setDtmfTerm(true);
        recordingConfiguration.setType("audio/x-wav");
        recordingConfiguration.setClientSideAssignationDestination(RECORDING_LOCATION);
        GrammarReference grammarReference = new GrammarReference("builtin:dtmf/digits?length=1");
        DtmfRecognition config = new DtmfRecognition(grammarReference);
        recordingConfiguration.setDtmfTermRecognition(config);
        recordingConfiguration.setPostAudioToServer(true);

        return interaction(interactionName).addPrompt(audio(audio)).build(recordingConfiguration, Duration.seconds(10));
    }

    private String processDtmfTurn(Interaction interaction) throws Timeout, InterruptedException {
        // Is there a better way?
        VoiceXmlInputTurn resultTurn = processTurn(interaction);
        RecognitionInfo result = resultTurn.getRecognitionInfo();
        if (result == null) return "";
        String rawDtmfs = result.getRecognitionResult().getJsonObject(0).getJsonString("utterance").getString();
        mLog.trace("Received {}", rawDtmfs);
        return rawDtmfs.replace(" ", "");
    }

    private VoiceXmlInputTurn processTurn(VoiceXmlOutputTurn outputTurn) throws Timeout, InterruptedException {
        VoiceXmlInputTurn inputTurn = mChannel.doTurn(outputTurn, null);
        while (VoiceXmlEvent.hasEvent(VoiceXmlEvent.NO_INPUT, inputTurn.getEvents())
               || VoiceXmlEvent.hasEvent(VoiceXmlEvent.NO_MATCH, inputTurn.getEvents())) {
            inputTurn = mChannel.doTurn(outputTurn, null);
        }
        if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.CONNECTION_DISCONNECT_HANGUP, inputTurn.getEvents()))
            throw new HangUp();
        if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.ERROR, inputTurn.getEvents())) throw new PlatformError();
        return inputTurn;
    }

}