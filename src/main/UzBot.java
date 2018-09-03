package main;

import com.google.common.base.Splitter;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.net.URL;
import java.util.*;

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

        long chatId = message.getChatId();

        if (replyText.isEmpty()) {
            String[] args = incomeText.split(" ");

            switch (args[0]) {
                case "/help":
                    replyText = "command list will be here.. /status for example";
                    break;

                case "/status":
                    replyText = getStatus();
                    break;

                case "/start":
                    if (args.length > 1) {
                        int idx = 0;
                        String command = args[1];

                        if (args.length > 2) {
                            idx = Integer.parseInt(command);
                            command = args[2];
                        }

                        Thread newThread = new Thread(
                                () -> {
                                    while (true) {
                                        if (Thread.interrupted()) {
                                            break;
                                        }

                                        try {
                                            URL url = new URL(Thread.currentThread().getName());
                                            Map<String, String> params = Splitter.on("&").withKeyValueSeparator("=").split(url.getQuery());

                                            System.out.println(url.getQuery());

                                            JSONObject data = UzApi.getTrains(
                                                    params.get("from"),
                                                    params.get("to"),
                                                    params.get("date")
                                            );

                                            if (data.isNull("warning")) {
                                                boolean isOnlyPlatskart = Boolean.parseBoolean(params.getOrDefault("only_p", "false"));
                                                boolean isPlatskartAvailable = false;

                                                if (isOnlyPlatskart) {
                                                    JSONArray trains = data.getJSONArray("list");

                                                    outerloop:
                                                    for (int i = 0; i < trains.length(); i++) {
                                                        JSONArray types = trains.getJSONObject(i).getJSONArray("types");

                                                        for (int j = 0; j < types.length(); j++) {
                                                            JSONObject type = types.getJSONObject(j);

                                                            if (type.get("id").equals("П")) {
                                                                isPlatskartAvailable = true;
                                                                break outerloop;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (!(isOnlyPlatskart && !isPlatskartAvailable)) {
                                                    for (int i = 0; i < 10; i++) {
                                                        sendApiMethod(new SendMessage(chatId, "SUCCESS !!!\n" + url).disableWebPagePreview());
                                                        Thread.sleep(10 * 1000);
                                                    }
                                                    Thread.currentThread().interrupt();
                                                }
                                            }

                                            Thread.sleep(5 * 60 * 1000);
                                        } catch (InterruptedException e) {
                                            System.out.println(Thread.currentThread().getId() + ": sleep was interrupted");
                                            Thread.currentThread().interrupt();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            try {
                                                sendApiMethod(new SendMessage(chatId, "Failed :(").disableNotification());
                                                Thread.sleep(30 * 60 * 1000);
                                            } catch (TelegramApiException | InterruptedException e1) {
                                                e1.printStackTrace();
                                                Thread.currentThread().interrupt();
                                            }
                                        }
                                    }
                                },
                                command);
                        newThread.start();

                        if (idx < scanPool.size()) {
                            scanPool.get(idx).interrupt();
                            scanPool.set(idx, newThread);
                        } else {
                            scanPool.add(newThread);
                        }
                    }

                    replyText = getStatus();
                    break;

                case "/stop":
                    if (args.length == 1) {
                        for (Iterator<Thread> iterator = scanPool.iterator(); iterator.hasNext(); ) {
                            Thread thread = iterator.next();
                            thread.interrupt();
                            iterator.remove();
                        }
                        replyText = "all scanPool has been stopped\n";
                    } else {
                        int idx = Integer.parseInt(args[1]);
                        scanPool.get(idx).interrupt();
                        scanPool.remove(idx);
                        replyText = "#" + idx + " scanning has been stopped\n";
                    }

                    replyText += getStatus();
                    break;

                default:
                    replyText = "whaat?";
            }
        }

        System.out.println(message.getDate() + "\t" + message.getChat().getUserName() + ":\t" + incomeText + "\t reply: " + replyText);

        try {
            sendApiMethod(new SendMessage(chatId, replyText).disableWebPagePreview());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getStatus() {
        StringBuilder result = new StringBuilder();

        scanPool.removeIf(Thread::isInterrupted);

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
