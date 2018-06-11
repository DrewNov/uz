package main;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UzBot extends TelegramLongPollingBot {

    Properties props;

    public UzBot() {
        try {
            props = new Properties();
            props.load(new FileInputStream("resources/config.properties"));
        } catch (IOException e) {
            System.out.println("Error: config.properties file not found!");
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        String userName = message.getChat().getUserName();

        System.out.println(message.getDate() + "\t" + userName + ":\t" + message.getText());

        try {
            SendMessage msg = new SendMessage(message.getChatId(), userName);
            sendApiMethod(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return props.getProperty("name");
    }

    @Override
    public String getBotToken() {
        return props.getProperty("token");
    }
}
