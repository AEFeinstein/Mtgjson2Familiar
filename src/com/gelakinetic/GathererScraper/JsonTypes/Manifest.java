package com.gelakinetic.GathererScraper.JsonTypes;

import java.util.ArrayList;

public class Manifest {

    public final ArrayList<ManifestEntry> mPatches = new ArrayList<>();
    public long mTimestamp;

    public static class ManifestEntry implements Comparable<ManifestEntry> {
        public String mName;
        public String mURL;
        public String mCode;
        public String mDigest;

        // List of image URLs
        public ArrayList<String> mExpansionImageURLs = new ArrayList<>();

        @Override
        public int compareTo(ManifestEntry o) {
            return mName.compareTo(o.mName);
        }
    }

}
