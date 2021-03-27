package com.magi1053;

import com.cloudmersive.client.ImageOcrApi;
import com.cloudmersive.client.invoker.ApiClient;
import com.cloudmersive.client.invoker.Configuration;
import com.cloudmersive.client.invoker.auth.ApiKeyAuth;
import com.cloudmersive.client.model.ImageToTextResponse;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageEmbedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Impact extends ListenerAdapter {
    static JDA jda;
    static String bot_token = "DISCORD_TOKEN";
    static String guild_id = "613087331142074389";
    static Guild guild;
    static String member_id = "620457035019976736";
    static Role member;
    //     static String census_id = "604514647269179392"; // Test channel
    static String census_id = "648377141234237463";
    static String leavelog_id = "631235472080896048";
    static Sheets sheets = new Sheets("SHEET_ID", "API_KEY");
    static Pattern ancestralPattern = Pattern.compile("^\\D*(\\d+)\\D*Faction$", Pattern.MULTILINE);
    static Pattern namePattern = Pattern.compile("^\\d*\\s*([a-zA-Z]+)\\n(?:Ancestral\\n)?Faction$", Pattern.MULTILINE);
    static Pattern equipmentPattern = Pattern.compile("^Equipment Points\\n([\\d ]+)", Pattern.MULTILINE);
    // static Pattern ancestralPattern = Pattern.compile("^[a-zA-Z]*\\s?(\\d+)\\s([a-zA-Z]+)\\s([a-zA-Z]+)\\s?$", Pattern.MULTILINE);
    // static Pattern equipmentPattern = Pattern.compile("^Eq\\w+t Points\\s(\\d+)\\s?$", Pattern.MULTILINE);

    public static void main(String[] args) throws Exception {
        jda = new JDABuilder(bot_token)
                .addEventListeners(new Impact())
                .build();
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api-na-premium.cloudmersive.com");
        ApiKeyAuth Apikey = (ApiKeyAuth) defaultClient.getAuthentication("Apikey");
        Apikey.setApiKey("API_KEY");
        jda.awaitReady();
        guild = jda.getGuildById(guild_id);
        assert guild != null;
        member = guild.getRoleById(member_id);
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        Member member = event.getMember();
        System.out.printf("Triggering user join for \"%s\"%n", member.getEffectiveName());
        sheets.sendPost(member.getId());
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        Member member = event.getMember();
        System.out.printf("Triggering user leave for \"%s\"%n", member.getEffectiveName());
        Sheets.Response sheetsResponse = sheets.sendGet(member.getId());
        Character character = sheetsResponse.user;
        if (character != null) {
            TextChannel channel = jda.getTextChannelById(leavelog_id);
            assert channel != null;
            channel.sendMessage(character.embed()).queue();
        }
    }

    @SneakyThrows
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User author = event.getAuthor();
        if (author.isBot()) return;
        Member guildMember = guild.getMember(author);
        if (guildMember == null || !guildMember.getRoles().contains(member)) return;
        MessageChannel channel = event.getChannel();
        Message message = event.getMessage();
        if (message.getContentDisplay().startsWith(".gs")) {
            List<User> users = new ArrayList<>(message.getMentionedUsers());
            if (users.isEmpty()) users.add(author);
            MessageAction messageAction = channel.sendMessage(author.getAsMention());
            for (User user : users) {
                Member userMember = guild.getMember(user);
                if (userMember == null || !userMember.getRoles().contains(member)) return;
                channel.sendTyping().queue();
                Character character = sheets.sendGet(user.getId()).user;
                if (character != null && character.active) messageAction.embed(character.embed()).queue();
            }
        } else if (event.isFromGuild() && channel.getId().equals(census_id)) {
            String content = message.getContentStripped();
            List<MessageEmbed> embeds = message.getEmbeds();
            List<Character.Submission> images = new ArrayList<>();
            for (MessageEmbed embed : embeds) {
                String image = embed.getThumbnail().getUrl();
                assert image != null;
                content = content.replace(image, "");
                String url = embed.getUrl();
                if (url != null) content = content.replace(url, "");
                images.add(new Character.Submission(author, image, embed.getTimestamp()));
            }
            String finalContent = content;
            List<Message.Attachment> attachments = message.getAttachments();
            for (Message.Attachment attachment : attachments) CompletableFuture.runAsync(() -> processCharacterInfo(channel, finalContent, new Character.Submission(author, attachment.getUrl(), attachment.getTimeCreated())));
            for (Character.Submission submission : images) CompletableFuture.runAsync(() -> processCharacterInfo(channel, finalContent, submission));
        }
    }

    @SneakyThrows
    @Override
    public void onGuildMessageEmbed(@Nonnull GuildMessageEmbedEvent event) {
        TextChannel channel = event.getChannel();
        if (!channel.getId().equals(census_id)) return;
        Message message = channel.retrieveMessageById(event.getMessageId()).complete();
        User author = message.getAuthor();
        if (author.isBot()) return;
        String content = message.getContentStripped();
        List<MessageEmbed> embeds = event.getMessageEmbeds();
        List<Character.Submission> images = new ArrayList<>();
        for (MessageEmbed embed : embeds) {
            String image = embed.getThumbnail().getUrl();
            assert image != null;
            content = content.replace(image, "");
            String url = embed.getUrl();
            if (url != null) content = content.replace(url, "");
            images.add(new Character.Submission(author, image, embed.getTimestamp()));
        }
        String finalContent = content;
        for (Character.Submission submission : images) CompletableFuture.runAsync(() -> processCharacterInfo(channel, finalContent, submission));
    }

    @SneakyThrows
    private static void processCharacterInfo(MessageChannel channel, String content, Character.Submission submission) {
        channel.sendTyping().queue();
        Character character = new Character();
        List<String> errors = new ArrayList<>();

        System.out.printf("Triggering image processing from \"%s\"%n", submission.getAuthor().getName());
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(submission.getImage()).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31");
            final BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
            File imageFile = new File(submission.getAuthor().getId() + ".png");
            ImageIO.write(bufferedImage, "png", imageFile);
            ImageToTextResponse result;
            result = new ImageOcrApi().imageOcrPost(imageFile, "Advanced", "ENG", "Auto");
            String textResult = result.getTextResult();
            System.out.println(textResult);

            // OCRResponse ocrResponse = ocr.sendPost(image);
            // LinkedTreeMap parsedResults = (LinkedTreeMap) ocrResponse.ParsedResults[0];
            // String parsedText = parsedResults.get("ParsedText").toString();
            // System.out.println(parsedText);

            Matcher nameMatcher = namePattern.matcher(textResult);
            Matcher ancestralMatcher = ancestralPattern.matcher(textResult);
            Matcher equipmentMatcher = equipmentPattern.matcher(textResult);
            if (nameMatcher.find()) character.name = nameMatcher.group(1);
            if (ancestralMatcher.find()) character.ancestry = Integer.parseInt(ancestralMatcher.group(1));
            if (equipmentMatcher.find()) character.equipment = Integer.parseInt(equipmentMatcher.group(1).replaceAll("\\s+",""));
            character.discord = submission.getAuthor().getId();
            character.verification = submission.getImage();
            character.notes = content.trim();
            character.updated = submission.getDateTime();

            if (character.name == null || character.name.isEmpty()) {
                errors.add("Unable to parse character name");
            } else if (character.ancestry == null) {
                errors.add("Unable to parse ancestry level");
            }
            // if (character.occupation == null || character.occupation.isEmpty()) {
            //     errors.add("Unable to parse occupation");
            // }
            if (character.equipment == null) {
                errors.add("Unable to parse equipment points");
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        if (errors.isEmpty()) {
            List<NameValuePair> params = new ArrayList<>();
            if (character.ancestry != null) params.add(new BasicNameValuePair("ancestry", character.ancestry.toString()));
            if (character.equipment != null) params.add(new BasicNameValuePair("equipment", character.equipment.toString()));
            if (character.discord != null) params.add(new BasicNameValuePair("discord", character.discord));
            if (character.verification != null) params.add(new BasicNameValuePair("verification", character.verification));
            if (character.notes != null) params.add(new BasicNameValuePair("notes", character.notes));
            if (character.updated != null) params.add(new BasicNameValuePair("updated", Long.toString(character.updated.getTime())));
            Sheets.Response sheetsResponse = sheets.sendPost(character.name, new UrlEncodedFormEntity(params));
            if (sheetsResponse.error != null) errors.add(sheetsResponse.error);
            else character = sheetsResponse.user;
        }

        if (errors.isEmpty()) {
            channel.sendMessage(submission.getAuthor().getAsMention() + " Thank you for your submission.").embed(character.embed()).queue();
        } else {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("Errors occurred processing your submission:\n```");
            for (String error : errors) errorMessage.append(error).append("\n");
            errorMessage.append("```Please try retaking your screenshot and submit it again.");
            channel.sendMessage(submission.getAuthor().getAsMention() + " " + errorMessage.toString()).queue();
        }
    }
}

class Character {
    String name;
    Boolean active;
    String guild;
    Integer ancestry;
    Integer equipment;
    String occupation;
    Date updated;
    String discord;
    String verification;
    String notes;

    MessageEmbed embed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(name);
        if (notes != null) eb.setDescription(notes);
        if (verification != null) eb.setThumbnail(verification);
        eb.addField("Ancestry", ancestry != null ? ancestry.toString() : "N/A", true);
        eb.addField("Equipment", equipment != null ? equipment.toString() : "N/A", true);
        eb.addField("Class", occupation != null ? occupation : "None", true);
        eb.addField("Guild", guild != null ? guild : "None", true);
        User user = discord != null ? Impact.jda.retrieveUserById(discord).complete() : null;
        eb.addField("Discord", user != null ? user.getAsMention() : "N/A", true);
        if (updated != null) eb.setFooter("Last Updated " + updated.toString());
        return eb.build();
    }

    static class Submission {
        @Getter
        private User author;
        @Getter
        private String image;
        @Getter
        private Date dateTime;

        Submission(User author, String image, Date dateTime) {
            this.author = author;
            this.image = image;
            this.dateTime = dateTime;
        }

        Submission(User author, String url, OffsetDateTime dateTime) {
            this(author, url, dateTime != null ? dateTime.toInstant() : Instant.now());
        }

        Submission(User author, String url, Instant dateTime) {
            this(author, url, Date.from(dateTime));
        }
    }

}
