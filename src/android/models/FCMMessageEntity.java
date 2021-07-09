package org.apache.cordova.firebase.models;

import java.util.List;

public class FCMMessageEntity {
    String msgid;
    String target;
    String initiatorJid;
    String name;
    String groupName;
    String message;
    List<String> mention;
    String eventType;
    String replaceId;
    long timeStamp;

    public FCMMessageEntity(
        String msgid,
        String target,
        String initiatorJid,
        String name,
        String groupName,
        String message,
        List<String> mention,
        String eventType,
        String replaceId,
        long timeStamp) {

        this.msgid = msgid;
        this.target = target;
        this.initiatorJid = initiatorJid;
        this.name = name;
        this.groupName = groupName;
        this.message = message;
        this.mention = mention;
        this.eventType = eventType;
        this.replaceId = replaceId;
        this.timeStamp = timeStamp;
    }
}
