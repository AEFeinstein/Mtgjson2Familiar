package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class mtgjson_meta {
    String date;
    public String version;

    public long getTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
            return sdf.parse(this.date).getTime() / 1000;
        } catch (ParseException e) {
            System.err.println("TIMESTAMP NOT PARSED: ~" + this.date + "~");
        }
        return 0;
    }
}
