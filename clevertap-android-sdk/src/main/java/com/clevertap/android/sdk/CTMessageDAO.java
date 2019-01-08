package com.clevertap.android.sdk;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Message Data Access Object class interfacing with Database
 */
class CTMessageDAO {
    private String id;
    private JSONObject jsonData;
    private boolean read;
    private long date;
    private long expires;
    private String userId;
    private List<String> tags = new ArrayList<>();
    private String campaignId;
    private JSONObject wzrkParams;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    JSONObject getJsonData() {
        return jsonData;
    }

    void setJsonData(JSONObject jsonData) {
        this.jsonData = jsonData;
    }

    int isRead() {
        if(read){
            return 1;
        }else{
            return 0;
        }
    }

    void setRead(int read) {
        this.read = read == 1;
    }

    long getDate() {
        return date;
    }

    void setDate(long date) {
        this.date = date;
    }

    long getExpires() {
        return expires;
    }

    void setExpires(long expires) {
        this.expires = expires;
    }

    String getUserId() {
        return userId;
    }

    void setUserId(String userId) {
        this.userId = userId;
    }

    String getTags() {
        return TextUtils.join(",",tags);
    }

    void setTags(String tags) {
        String[] tagsArray = tags.split(",");
        this.tags.addAll(Arrays.asList(tagsArray));

    }

    String getCampaignId() {
        return campaignId;
    }

    void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    JSONObject getWzrkParams() {
        return wzrkParams;
    }

    void setWzrkParams(JSONObject wzrk_params) {
        this.wzrkParams = wzrk_params;
    }

     CTMessageDAO(){}  // TODO bad practice to allow unparameterized constructor; very error prone

    private CTMessageDAO(String id, JSONObject jsonData, boolean read, long date, long expires, String userId, String tags, String campaignId, JSONObject wzrkParams){
        this.id = id;
        this.jsonData = jsonData;
        this.read = read;
        this.date = date;
        this.expires = expires;
        this.userId = userId;
        if(tags!=null)
            this.tags = Arrays.asList(tags.split(","));
        this.campaignId = campaignId;
        this.wzrkParams = wzrkParams;
    }

    static CTMessageDAO initWithJSON(JSONObject inboxMessage, String userId){
        try {
            String id = inboxMessage.has("_id") ? inboxMessage.getString("_id") : "00";
            long date = inboxMessage.has("date") ? inboxMessage.getInt("date") : System.currentTimeMillis()/1000;
            long expires = inboxMessage.has("wzrk_ttl") ? inboxMessage.getInt("wzrk_ttl") : (System.currentTimeMillis() + 24*60*Constants.INTERVAL_MINUTES)/1000;
            JSONObject cellObject = inboxMessage.has("msg") ? inboxMessage.getJSONObject("msg") : null;
            String tags = "";
            if(cellObject != null) {
                tags = cellObject.has("tags") ? cellObject.getString("tags") : null;
            }
            String campaignId = inboxMessage.has("wzrk_id") ? inboxMessage.getString("wzrk_id") : "0_0";
            JSONObject wzrkParams = getWzrkFields(inboxMessage);
            return new CTMessageDAO(id, cellObject, false,date,expires,userId, tags,campaignId,wzrkParams);
        }catch (JSONException e){
            Logger.d("Unable to parse Notification inbox message to CTMessageDao - "+e.getLocalizedMessage());
            return null;
        }
    }

    JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id",this.id);
            jsonObject.put("msg",this.jsonData);
            jsonObject.put("isRead",this.read);
            jsonObject.put("date",this.date);
            jsonObject.put("wzrk_ttl",this.expires);
            JSONArray jsonArray = new JSONArray();
            for(int i=0; i<this.tags.size(); i++){
                jsonArray.put(tags.get(i));
            }
            jsonObject.put("tags",jsonArray);
            jsonObject.put("wzrk_id",campaignId);
            jsonObject.put("wzrkParams",wzrkParams);
            return jsonObject;
        } catch (JSONException e) {
            Logger.v("Unable to convert CTMessageDao to JSON - "+e.getLocalizedMessage());
            return jsonObject;
        }
    }

    private static JSONObject getWzrkFields(JSONObject root) throws JSONException {
        final JSONObject fields = new JSONObject();
        Iterator<String> iterator = root.keys();

        while(iterator.hasNext()){
            String keyName = iterator.next();
            if(keyName.startsWith(Constants.WZRK_PREFIX))
                fields.put(keyName,root.get(keyName));
        }

        return fields;
    }
}
