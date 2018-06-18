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

        if (message == null) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            if (callbackQuery != null) {//isAnswer = true
                message = callbackQuery.getMessage();
                incomeText = callbackQuery.getData();
            } else {
                message = update.getEditedMessage();
                incomeText = message.getText();
                replyText = "editable not supported!";
            }
        } else {
            incomeText = message.getText();
        }

        if (replyText.isEmpty()) {
            switch (incomeText.split(" ")[0]) {
                case "/help":
                    replyText = "command list will be here.. /status for example";
                    break;

                case "/status":
                    replyText = getStatus();
                    break;

                case "/start":
                    replyText = "all scanPool has been started";
                    break;

                case "/stop":
                    for (Thread thread : scanPool) {
                        thread.interrupt();
                    }
                    replyText = "all scanPool has been stopped";
                    break;

                case "/scan":
                    String commandParam = incomeText.split(" ")[1];

                    Thread thread = new Thread(
                            () -> {
                                //todo: implement
                                while (true) {
                                    System.out.println(Thread.currentThread().getName());
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },
                            commandParam);
                    thread.start();

                    if (scanPool.isEmpty()) {
                        scanPool.add(thread);
                    } else {
                        scanPool.get(0).interrupt();
                        scanPool.set(0, thread);
                    }

                    replyText = getStatus();
                    break;

                default:
                    replyText = "whaat?";
            }
        }

        System.out.println(message.getDate() + "\t" + message.getChat().getUserName() + ":\t" + incomeText + "\t reply: " + replyText);

        try {
            sendApiMethod(new SendMessage(message.getChatId(), replyText));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getStatus() {
        StringBuilder result = new StringBuilder();

        if (scanPool.isEmpty()) {
            result.append("0: empty");
        } else {
            for (int i = 0; i < scanPool.size(); i++) {
                result.append(i).append(": ").append(scanPool.get(i).getName()).append("\n");
            }
        }

        return result.toString();
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
