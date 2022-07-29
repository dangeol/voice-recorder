package com.dangeol.voicerecorder.services;

import com.dangeol.voicerecorder.Commands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SchedulerService {
    /*
        Service class providing scheduled tasks after a given period of time or at an interval
        defined by "delay" and "timeUnit"
     */
    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private static final int delayStopMaxTime = 180;
    private static final int delayActivity = 24;
    private static final TimeUnit timeUnitStop = TimeUnit.MINUTES;
    private static final TimeUnit timeUnitActivity = TimeUnit.HOURS;
    private final Commands commands = new Commands();
    private ScheduledFuture<?> scheduledFuture;

    public SchedulerService() {
        // prevent Memory leaks
        executor.setRemoveOnCancelPolicy(true);
    }

    /**
     * Stop the recording when its time limit is reached
     * @param event
     */
    public void scheduleStopEvent(MessageReceivedEvent event) {
        Runnable taskStopAfterMaxTime = () -> {
            if (event.getGuild().getAudioManager().isConnected()) {
                commands.onStopCommand(event);
                logger.info("Scheduled stop down initiated.");
            } else {
                logger.info("Scheduled stop down not initiated, no Audio connection.");
            }
        };
        try {
            scheduledFuture = executor.scheduleWithFixedDelay(taskStopAfterMaxTime,
                    0, delayStopMaxTime, timeUnitStop);
        } catch (RejectedExecutionException ree) {
            logger.error(ree.getMessage());
        }
    }

    public void cancelStopEvent() {
        scheduledFuture.cancel(true);
        logger.info("Scheduled stop command cancelled");
    }

    /**
     * The bot's activity must be reset regularly because it seems to expire automatically
     * after about two or three days
     * @param jda
     */
    public void scheduleSetBotActivityEvent(JDA jda) {
        Runnable task = () -> {
            try {
                jda.awaitReady();
                jda.getPresence().setActivity(Activity.playing("https://gitlab.com/sirimangalo/voice-recorder"));
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
            logger.info("Set scheduled activity.");
        };
        try {
            executor.scheduleAtFixedRate(task,
                    0, delayActivity, timeUnitActivity);
        } catch (RejectedExecutionException ree) {
            logger.error(ree.getMessage());
        }
    }
}
