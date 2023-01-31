package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.ArrayList;

public class LegalityData {

    public ArrayList<Format> mFormats = new ArrayList<>();
    public long mTimestamp;

    public static class Format {
        public final ArrayList<String> mSets = new ArrayList<>();
        public final ArrayList<String> mRestrictedlist = new ArrayList<>();
        public final ArrayList<String> mBanlist = new ArrayList<>();
        public String mName;

        public Format(String name) {
            mName = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Format) {
                return this.mName.equals(((Format) obj).mName);
            }
            return false;
        }
    }
}

