package main;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.CallbackQuery;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public String getBotUsername() {
        return props.getProperty("name");
    }

    @Override
    public String getBotToken() {
        return props.getProperty("token");
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        boolean isAnswer = false;
        String incomeText = "";
        String replyText = "";

        if (message == null) {
            isAnswer = true;
            CallbackQuery callbackQuery = update.getCallbackQuery();
            message = callbackQuery.getMessage();
            incomeText = callbackQuery.getData();
        } else {
            incomeText = message.getText();
        }

        switch (incomeText) {
            case "/help":
                replyText = "ooh";
                break;

            default:
                replyText = "whaat?";
        }

        System.out.println(message.getDate() + "\t" + message.getChat().getUserName() + ":\t" + incomeText);

        try {
            sendHideKeyboard(message, replyText);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendHideKeyboard(Message inputMsg, String text) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(inputMsg.getChatId(), text);
        List<InlineKeyboardButton> inlineKeyboardButtons = new ArrayList<>();

        InlineKeyboardButton btn1 = new InlineKeyboardButton("btn1");
        btn1.setCallbackData("/btn1");

        InlineKeyboardButton btn2 = new InlineKeyboardButton("btn2");
        btn2.setCallbackData("/help");

        inlineKeyboardButtons.add(btn1);
        inlineKeyboardButtons.add(btn2);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(inlineKeyboardButtons);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(rows);

        //sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(keyboardMarkup);

        sendApiMethod(sendMessage);
    }
}
