package org.apache.cordova.firebase.models;

public class PayloadTalk {
    public String msgid;
    public String jid;
    public String name;
    public String eType;
    public String body;
    public String gt;
    public String nType;
    public String nsound;
    public String[] mention;

    public String getMsgid() {
        return msgid;
    }

    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String geteType() {
        return eType;
    }

    public void seteType(String eType) {
        this.eType = eType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getGt() {
        return gt;
    }

    public void setGt(String gt) {
        this.gt = gt;
    }

    public String getnType() {
        return nType;
    }

    public void setnType(String nType) {
        this.nType = nType;
    }

    public String getNsound() {
        return nsound;
    }

    public void setNsound(String nsound) {
        this.nsound = nsound;
    }

}
