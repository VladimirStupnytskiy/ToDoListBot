import org.json.JSONArray;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;

import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class ToDoBot extends TelegramLongPollingBot {
    private Message message;
    private boolean createBot = false;
    String[] mainMenu = {"Новий список", "Опції списку"};

    int kamikadzeMessageID;
    String tag = "x";
    String markedSymb = "✅ ";
    String unmarkedSymb = "◽️ ";

    String listTitle = "️✔️️ *TO DO LIST*";
    DBController dbController = DBController.getInstance();
    Mode mode = Mode.SPLIT; // standard mode


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            message = update.getMessage();
            long chatID = message.getChatId();
            kamikadzeMessageID = message.getMessageId();
            String messageText = message.getText();
            String userID = message.getFrom().getId().toString();

            if (messageText.equals("/start")) {
                if (!dbController.isExist(userID)){
                    dbController.createFirst(userID,message.getFrom().getFirstName().
                            concat(" ").concat(message.getFrom().getLastName()),mode,0);
                }
                launch(chatID,userID);
            }else
            if (messageText.equals(mainMenu[0])) {//Новий список
                launch(chatID,userID);
            }else

            if (messageText.equals(mainMenu[1])) {//Опції списку
                deleteMessage(kamikadzeMessageID, chatID);
                MySendMassage(myInlineKeyboard(new LinkedHashMap(){{
                    put("Ставити галки навпроти виконаних пунктів", "SET_MODE_MARK");
                    put("Переміщувати виконані пункти вниз списку", "SET_MODE_SPLIT");
                }}),"Яким чином відображати динаміку виконання?",false,chatID,userID);
            }

            else {
                dbController.addComp(userID,messageText);
                deleteMessage(kamikadzeMessageID, chatID);
                showList(Mode.valueOf(dbController.getMode(userID)),userID,chatID);
            }

        } else if (update.hasCallbackQuery()) {
            String callback = update.getCallbackQuery().getData();
            long chatID = update.getCallbackQuery().getMessage().getChatId();
            String userID = update.getCallbackQuery().getFrom().getId().toString();


            if (callback.contains(tag)){
                dbController.click(userID,Integer.parseInt(callback.split(tag)[1]));
                showList(Mode.valueOf(dbController.getMode(userID)),userID,chatID);
            }

            switch (callback){
                case ("SET_MODE_MARK"):
                    dbController.setMode(userID,Mode.MARK);
                    showList(Mode.valueOf(dbController.getMode(userID)),userID,chatID);
                    deleteMessage(update.getCallbackQuery().getMessage().getMessageId(),update.getCallbackQuery().getMessage().getChatId());
                    break;
                case ("SET_MODE_SPLIT"):
                    dbController.setMode(userID,Mode.SPLIT);
                    showList(Mode.valueOf(dbController.getMode(userID)),userID,chatID);
                    deleteMessage(update.getCallbackQuery().getMessage().getMessageId(),update.getCallbackQuery().getMessage().getChatId());
                    break;}

            if (callback.equals("DONE")){
                dbController.setHide(userID);
                showList(Mode.valueOf(dbController.getMode(userID)),userID,chatID);
            }
        }
    }

    private void showList(Mode mode, String userID, long chatID) {
        LinkedHashMap linked = new LinkedHashMap();
        LinkedHashMap temp = new LinkedHashMap();
        JSONArray list = dbController.getList(userID);

        switch (mode){
            case MARK -> {
                for (int i = 0; i < list.length(); i++) {
                    if (Character.toLowerCase(list.getString(i).charAt(0)) == '$'){
                        linked.put(markedSymb.concat(list.getString(i).substring(1)),tag.concat(String.valueOf(i)));
                    }else
                        linked.put(unmarkedSymb.concat(list.getString(i)),tag.concat(String.valueOf(i)));
                }
                break;
            }

            case SPLIT -> {
                for (int i = 0; i < list.length(); i++) {
                    if (Character.toLowerCase(list.getString(i).charAt(0)) != '$'){
                        linked.put(unmarkedSymb.concat(list.getString(i)),tag.concat(String.valueOf(i)));
                    }else
                        temp.put(markedSymb.concat(list.getString(i).substring(1)),tag.concat(String.valueOf(i)));
                }
                if (temp.size()!=0 && !dbController.getHide(userID))linked.put("〰️〰️〰 \uD83D\uDD3B️ DONE \uD83D\uDD3B〰️〰️〰️","DONE");
                else if (temp.size()!=0)linked.put("〰〰〰\uD83D\uDCC2 DONE \uD83D\uDCC2〰〰〰","DONE");

                if (!dbController.getHide(userID)) linked.putAll(temp);
                break;
            }
        }

        if (Integer.parseInt(dbController.getMainMessageID(userID))!=0){
            editMessage(Integer.parseInt(dbController.getMainMessageID(userID)),chatID,progress(userID,chatID));
            editInlineMenu(Integer.parseInt(dbController.getMainMessageID(userID)),chatID,myInlineKeyboard(linked));
        }else MySendMassage(myInlineKeyboard(linked), progress(userID,chatID), true, chatID, userID);
    }

    public String progress(String userID, long chatID){
       int count = dbController.getMarkedCount(userID);
       int size = dbController.getList(userID).length();

        StringBuilder sb = new StringBuilder();
        sb.append(listTitle).append("  (").
                append(count).
                append("/").
                append(size).
                append(")  `").
                append(100*count/size).
                append("%`");//`
        return sb.toString();
    }

    public void launch(long chatID,String userID){
        deleteMessage(kamikadzeMessageID, chatID);
        dbController.clearList(userID);
        MySendMassage(mainMenuKeyboard(), "Відправ перший пункт списку",chatID);
    }


    public void MySendMassage(InlineKeyboardMarkup inlineKeyboardMarkup, String text, boolean isMain, long chatID, String userID) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(text);
        try {
            if (isMain){
                dbController.setMainMessageID(userID,execute(sendMessage).getMessageId());
            }
            else {execute(sendMessage);}
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public InlineKeyboardMarkup myInlineKeyboard (LinkedHashMap linkedHashMap){
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        Set set = linkedHashMap.entrySet();
        Iterator i = set.iterator();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline;

        while (i.hasNext()) {
            Map.Entry me = (Map.Entry) i.next();
            rowInline = new ArrayList<>();
            rowInline.add(InlineKeyboardButton.builder()
                    .text((String) me.getKey()).
                    callbackData((String) me.getValue())
                    .build());
            rowsInline.add(rowInline);
        }
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    private void deleteMessage(int messageID, long chatID) {
        DeleteMessage deleteMessage = new DeleteMessage(String.valueOf(chatID), messageID);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void editMessage(int messageID, long chatID,String messageText) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setMessageId(messageID);
        editMessageText.setChatId(String.valueOf(chatID));
        editMessageText.setText(messageText);
        editMessageText.enableMarkdown(true);
        try {
            execute(editMessageText);
        } catch (TelegramApiException e) {

        }
    }

    public ReplyKeyboardMarkup mainMenuKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(mainMenu[0]));
        row.add(new KeyboardButton(mainMenu[1]));
        keyboard.add(row);
        replyKeyboardMarkup.setKeyboard(keyboard);
        return replyKeyboardMarkup;
    }


    private void editInlineMenu(int messageID, long chatID, InlineKeyboardMarkup inlineKeyboardMarkup) {
        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setMessageId(messageID);
        editMessageReplyMarkup.setChatId(String.valueOf(chatID));
        editMessageReplyMarkup.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(editMessageReplyMarkup);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void MySendMassage(ReplyKeyboardMarkup replyKeyboardMarkup, String text, long chatID) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(String.valueOf(chatID));
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "toDoL1stBot";
    }

    @Override
    public String getBotToken() {
        return "5163705176:AAEKCfKLOiLz_dUy9epT8yRNdsDTWMibl-c";
    }


}
