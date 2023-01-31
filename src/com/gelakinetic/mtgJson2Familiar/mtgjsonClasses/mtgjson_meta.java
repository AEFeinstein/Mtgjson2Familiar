package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import com.gelakinetic.mtgJson2Familiar.m2fLogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@SuppressWarnings("unused")
public class mtgjson_meta {
    public String version;
    String date;

    public long getTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
            return sdf.parse(this.date).getTime() / 1000;
        } catch (ParseException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "TIMESTAMP NOT PARSED: ~" + this.date + "~");
        }
        return 0;
    }
}
