package com.dangeol.voicerecorder;

import com.dangeol.voicerecorder.audio.AudioHandler;
import com.dangeol.voicerecorder.services.SchedulerService;
import com.dangeol.voicerecorder.utils.MessageUtil;
import com.dangeol.voicerecorder.utils.UploadUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Commands {

    private static final Logger logger = LoggerFactory.getLogger(com.dangeol.voicerecorder.VoiceRecorder.class);
    private static final MessageUtil messages = new MessageUtil();
    private static final SchedulerService stopSchedulerService = new SchedulerService();

    /**
     * Handle command without arguments.
     * @param event: The event for this command
     */
    public void onRecordCommand(MessageReceivedEvent event) throws Exception {
        Member member = event.getMember();
        GuildVoiceState voiceState = member.getVoiceState();
        AudioChannel channel = voiceState.getChannel();
        if (channel == null) {
            messages.onUnknownChannelMessage(event.getChannel(), "your voice channel");
            return;
        }
        connectTo(channel, event);
    }

    /**
     * Handle command with arguments.
     * @param event: The event for this command
     * @param guild: The guild where its happening
     * @param arg: The input argument
     */
    public void onRecordCommand(MessageReceivedEvent event, Guild guild, String arg) throws Exception {
        boolean isNumber = arg.matches("\\d+"); // This is a regular expression that ensures the input consists of digits
        AudioChannel channel = null;
        if (isNumber) {
            channel = guild.getVoiceChannelById(arg);
        } if (channel == null) {
            List<VoiceChannel> channels = guild.getVoiceChannelsByName(arg, true);
            if (!channels.isEmpty())
                channel = channels.get(0);
        }

        MessageChannel messageChannel = event.getChannel();
        if (channel == null) {
            messages.onUnknownChannelMessage(messageChannel, arg);
            return;
        }
        connectTo(channel, event);
    }

    /**
     * Connect to requested channel and start audio handler
     * @param audioChannel: The audioChannel to connect to
     * @param event: GuildMessageReceivedEvent
     */
    public void connectTo(AudioChannel audioChannel, MessageReceivedEvent event) throws Exception {
        Guild guild = audioChannel.getGuild();
        MessageChannel messageChannel = event.getChannel();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            messages.onAlreadyConnectedMessage(messageChannel, audioChannel.getName());
            return;
        }
        messages.disclaimerConsentMessage(audioChannel, messageChannel);
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.error(ie.getMessage());
        }

        changeBotNickName(event, "[REC]");
        // Set the sending and receiving handler to our audio system
        AudioHandler handler = new AudioHandler();
        audioManager.setSendingHandler(handler);
        audioManager.setReceivingHandler(handler);
        audioManager.openAudioConnection(audioChannel);
        messages.onConnectionMessage(audioChannel, messageChannel);
        stopSchedulerService.scheduleStopEvent(event);
    }

    /**
     * Disconnect from channel and close the audio connection
     * @param event
     */
    public void onStopCommand(MessageReceivedEvent event) {
        AudioChannel connectedChannel = event.getGuild().getSelfMember().getVoiceState().getChannel();
        if(connectedChannel == null) {
            messages.onNotRecordingMessage(event.getChannel());
            return;
        }
        stopSchedulerService.cancelStopEvent();
        UploadUtil uploadutil = new UploadUtil();
        changeBotNickName(event, "");
        event.getGuild().getAudioManager().closeAudioConnection();
        messages.onDisconnectionMessage(event.getChannel());
        try {
            uploadutil.uploadMp3(event.getChannel());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Add a prefix to the bot's nickname during recording
     * @param event
     * @param prefix
     */
    public void changeBotNickName(MessageReceivedEvent event, String prefix) {
        Member bot = event.getGuild().getSelfMember();
        String botName = "VoiceRecorder";
        try {
            event.getGuild().modifyNickname(bot, prefix + botName).queue();
        } catch (InsufficientPermissionException e) {
            logger.error(e.getMessage());
        }
    }
}
