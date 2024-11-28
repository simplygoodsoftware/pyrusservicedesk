package com.pyrus.pyrusservicedesk.sdk.sync

import android.util.TimeUtils
import java.io.IOException
import java.util.Calendar

abstract class SyncEvent {
    protected var TAG: String = "SyncEvent"

    var eventId: String? = ""

    var clientTime: Calendar? = null

    /*constructor() {
        this.eventId = null
    }

    constructor(eventId: String?) {
        this.clientTime = TimeUtils.getCalendarUTC()
        this.eventId = eventId
    }

    @Throws(IOException::class)
    abstract fun writeParams(g: JsonGenerator, forServer: Boolean, cc: CacheController?)

    @Throws(IOException::class)
    abstract fun readParams(
        s: String?,
        jp: JsonParser,
        accountKey: AccountKey?,
        cc: CacheController?
    ): Boolean

    abstract fun isReady2send(cc: CacheController?): Boolean

    @Throws(IOException::class)
    override fun writeJson(g: JsonGenerator, cc: CacheController) {
        writeJson(g, false, cc)
    }

    @Throws(IOException::class)
    fun writeJson(g: JsonGenerator, forServer: Boolean, cc: CacheController) {
        g.writeStartObject()
        g.writeStringField("EventId", eventId)
        g.writeStringField("ClientTime", TimeHelper.calendarToStr(clientTime, false))
        writeParams(g, forServer, cc)
        g.writeEndObject()
    }

    @Throws(IOException::class)
    override fun readJson(jp: JsonParser, accountKey: AccountKey?, cc: CacheController?) {
        if (jp.getCurrentToken() !== JsonToken.START_OBJECT) {
            return
        }
        while (jp.nextToken() !== JsonToken.END_OBJECT) {
            val s: String = jp.getCurrentName()
            jp.nextToken()
            if ("EventId" == s) {
                eventId = jp.getText()
            } else if ("ClientTime" == s) {
                clientTime = TimeHelper.strToCalendar(jp.getText(), false)
            } else {
                if (!readParams(s, jp, accountKey, cc)) {
                    JsonHelper.skip(jp)
                }
            }
        }
    }*/

    override fun toString(): String {
        return "SyncEvent@" + hashCode() + "[" + eventId + "]"
    }
}