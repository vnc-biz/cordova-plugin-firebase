package org.apache.cordova.firebase.models;

import java.util.List;

public class FCMMessageEntity {
    String msgid;
    String target;
    String name;
    String groupName;
    String message;
    List<String> mention;
    String eventType;
    String replaceId;

    public FCMMessageEntity(
        String msgid,
        String target,
        String name,
        String groupName,
        String message,
        List<String> mention,
        String eventType,
        String replaceId) {

        this.msgid = msgid;
        this.target = target;
        this.name = name;
        this.groupName = groupNames;
        this.message = message;
        this.mention = mention;
        this.eventType = eventType;
        this.replaceId = replaceId;
    }
}
