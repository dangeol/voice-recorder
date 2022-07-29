package com.dangeol.voicerecorder;

import com.dangeol.voicerecorder.services.SchedulerService;
import com.dangeol.voicerecorder.utils.CreateDirFilesUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.EnumSet;

public class VoiceRecorder extends ListenerAdapter {

    private static final CreateDirFilesUtil createDirFilesUtil = new CreateDirFilesUtil();
    private static final Logger logger = LoggerFactory.getLogger(VoiceRecorder.class);
    private static final Commands commands = new Commands();

    public static void main(String[] args) {

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES
        );

        try {
            JDA jda = JDABuilder.createDefault(getEnvItem("bot_token"), intents)
                    .addEventListeners(new VoiceRecorder())
                    .setActivity(Activity.playing("https://gitlab.com/sirimangalo/voice-recorder"))
                    .enableCache(CacheFlag.VOICE_STATE)
                    .build();
            SchedulerService schedulerService = new SchedulerService();
            schedulerService.scheduleSetBotActivityEvent(jda);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        logger.info("Bot is online!");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = message.getAuthor();
        String content = message.getContentDisplay();
        Guild guild = event.getGuild();

        if (author.isBot())
            return;

        if (!event.isFromGuild())
        {
            return;
        }

        if (content.startsWith("!record ")) {
            String arg = content.substring("!record ".length());
            try {
                commands.onRecordCommand(event, guild, arg);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } else if (content.equals("!record")) {
            try {
                commands.onRecordCommand(event);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        } else if (content.equals("!stop")) {
            commands.onStopCommand(event);
        }
    }

    /**
     * Gets from env.json the value of the key which is passed as argument
     * @param key
     * @return: the String value of the requested item in env.json
     * @throws Exception
     */
    public static String getEnvItem(String key) throws Exception {
        InputStream inputStream = VoiceRecorder.class.getResourceAsStream("/env.json");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject)jsonParser.parse(
                new InputStreamReader(inputStream, "UTF-8"));
        return jsonObject.get(key).toString();
    }
}
