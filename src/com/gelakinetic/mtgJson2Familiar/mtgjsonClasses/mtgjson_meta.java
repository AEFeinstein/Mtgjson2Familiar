package com.gelakinetic.mtgJson2Familiar.mtgjsonClasses;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class mtgjson_meta {
    String date;
    public String version;

    public long getTimestamp() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(this.date).getTime() / 1000;
        } catch (ParseException e) {
            System.err.println("TIMESTAMP NOT PARSED: ~" + this.date + "~");
        }
        return 0;
    }
}
