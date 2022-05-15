import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class DBController {
    private static DBController dbController;
    private static final String MONGO_URI = "";
    private static MongoClient client;
    private static MongoDatabase database;
    private static MongoCollection collection;


    public static synchronized DBController getInstance() {
        if (dbController == null) {
            dbController = new DBController();
            client = new MongoClient(new MongoClientURI(MONGO_URI));
            database = client.getDatabase("toDoListDB");
            collection = database.getCollection("422116466");
        }
        return dbController;
    }

    public void addComp(String userID, String item) {
        MongoCollection mc = database.getCollection(userID);
        mc.updateOne((Document) mc.find().first(), Updates.addToSet("comps", item));

    }

    public void click(String userID, int index) {
        String tag = "$";
        MongoCollection mc = database.getCollection(userID);
        Document document = (Document) mc.find().first();
        JSONArray jsonArray = getList(userID);
        String temp = jsonArray.getString(index);
        if(Character.toLowerCase(temp.charAt(0)) == '$'){
            tag = "";
            temp = temp.substring(1);
        }
        mc.updateOne(document, Updates.set("comps.".concat(String.valueOf(index)), tag.concat(temp)));
    }

    public JSONArray getList(String userID) {
        return new JSONObject((Document) database.getCollection(userID).find().first()).getJSONArray("comps");
    }

    public void createFirst(String userID, String userName, Mode mode, int mainMessageID) {

        MongoCollection mc = database.getCollection(userID);
        Document document = new Document("user", userName).
                append("mode", mode.toString()).
                append("messageID", mainMessageID).
                append("isHide", "false");

        mc.insertOne(document);
    }
    public boolean isExist(String userID){
        return database.listCollectionNames()
                .into(new ArrayList<String>()).contains(userID);
    }

    public void setMode(String userID, Mode mode) {
        MongoCollection mc = database.getCollection(userID);
        mc.updateOne((Document) mc.find().first(), Updates.set("mode", String.valueOf(mode)));
    }

    public String getMode(String userID) {
        MongoCollection mc = database.getCollection(userID);
        Document document = (Document) mc.find().first();
        return (String) document.get("mode");
    }

    public void setMainMessageID(String userID, long value) {
        MongoCollection mc = database.getCollection(userID);
        mc.updateOne((Document) mc.find().first(), Updates.set("messageID", String.valueOf(value)));
    }

    public String getMainMessageID(String userID) {
        MongoCollection mc = database.getCollection(userID);
        Document document = (Document) mc.find().first();
        return String.valueOf(document.get("messageID"));
    }

    public void clearList(String userID) {
        MongoCollection mc = database.getCollection(userID);
        String[] cars = {""};
        mc.updateOne((Document)mc.find().first(),Updates.unset("comps"));
    //    mc.updateOne((Document) mc.find().first(), Updates.set("comps", Arrays.asList(cars)));

        mc.updateOne((Document) mc.find().first(), Updates.set("messageID", "0"));
    }

    public void setHide(String userID) {
        MongoCollection mc = database.getCollection(userID);
        Document document = (Document) mc.find().first();
        boolean temp = Boolean.parseBoolean((String) document.get("isHide"));
        mc.updateOne((Document) mc.find().first(), Updates.set("isHide", String.valueOf(!temp)));
    }

    public boolean getHide(String userID) {
        MongoCollection mc = database.getCollection(userID);
        Document document = (Document) mc.find().first();
        String temp = (String) document.get("isHide");
        return temp.equals("true") ? true : false;
    }

    public int getMarkedCount(String userID) {
        int k = 0;
        JSONArray jsonArray = getList(userID);
        for (int i = 0; i < jsonArray.length(); i++) {
            if (Character.toLowerCase(jsonArray.getString(i).charAt(0)) == '$')k++;
        }
        return k;
    }

}
