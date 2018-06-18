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

    private Properties props;
    private List<Thread> scanPool;

    public UzBot() {
        try {
            props = new Properties();
            props.load(new FileInputStream("resources/config.properties"));
            scanPool = new ArrayList<>();
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
        String incomeText = "";
        String replyText = "";

        if (message == null) { //isAnswer = true
            CallbackQuery callbackQuery = update.getCallbackQuery();
            message = callbackQuery.getMessage();
            incomeText = callbackQuery.getData();
        } else {
            incomeText = message.getText();
        }

        switch (incomeText.split(" ")[0]) {
            case "/help":
                replyText = "command list will be here.. /status for example";
                break;

            case "/status":
                if (scanPool.isEmpty()) {
                    replyText = "0: empty";
                } else {
                    for (int i = 0; i < scanPool.size(); i++) {
                        replyText += i + ": " + scanPool.get(i).getName() + "\n";
                    }
                }
                break;

            case "/start":
                replyText = "all scanPool has been started";
                break;

            case "/stop":
                replyText = "all scanPool has been stopped";
                break;

            case "/scan":
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //todo: implement
                    }
                });
                thread.setName(incomeText.split(" ")[1]);

                thread.run();

                replyText = "scan function will be invoked here..";
                break;

            default:
                replyText = "whaat?";
        }

        System.out.println(message.getDate() + "\t" + message.getChat().getUserName() + ":\t" + incomeText);

        try {
            sendApiMethod(new SendMessage(message.getChatId(), replyText));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendKeyboard(Message inputMsg, String text) throws TelegramApiException {
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
