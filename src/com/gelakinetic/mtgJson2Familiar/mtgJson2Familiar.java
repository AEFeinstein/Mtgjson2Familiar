package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.GathererScraper.JsonTypes.Patch;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_allPrintings;
import com.gelakinetic.mtgfam.helpers.tcgp.TcgpHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class mtgJson2Familiar {

    static class code {
        String name;
        String code;

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof code) {
                return ((code) obj).name.equals(this.name);
            }
            return false;
        }
    }

    static class codes {
        ArrayList<code> codes;
    }

    static class codeMapEntry {
        String mMtgjsonSetName;
        String mMtgjsonSetCode;
        String mFamiliarSetName;
        String mFamiliarSetCode;

        public codeMapEntry(code mjc, code fc) {
            this.mMtgjsonSetCode = mjc.code;
            this.mMtgjsonSetName = mjc.name;
            this.mFamiliarSetCode = fc.code;
            this.mFamiliarSetName = fc.name;
        }
    }

    static class codeMap {
        ArrayList<codeMapEntry> mCodes = new ArrayList<>();
    }

    public static void main(String[] args) {

        // Make a gson object
        Gson gsonReader = new Gson();
        Gson gsonWriter = new GsonBuilder()
                .setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")))
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        // Uncomment this to create the mapping between Familiar set codes and mtgjson set codes
        // createFamiliarMtgjsonCodeMap(gsonReader, gsonWriter);

        codeMap cm;
        try (FileReader fr = new FileReader("setCodeMap.json")) {
            cm = gsonWriter.fromJson(fr, codeMap.class);
        } catch (IOException e) {
            System.err.println("Couldn't read code map");
            e.printStackTrace();
            return;
        }

        // Get TCGPlayer.com group IDs
        HashMap<Long, String> ids;
        try {
            ids = new TcgpHelper().getGroupIds();
        } catch (IOException e) {
            System.err.println("Couldn't initialize TCGP group IDs");
            e.printStackTrace();
            return;
        }

        // Read in the AllPrintings.json file
        mtgjson_allPrintings printings;
        try (FileReader fr = new FileReader("AllPrintings.json")) {
            printings = gsonReader.fromJson(fr, mtgjson_allPrintings.class);
        } catch (IOException e) {
            System.err.println("Couldn't read AllPrintings.json");
            e.printStackTrace();
            return;
        }

        // Iterate over all sets
        Manifest manifest = new Manifest();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);

            if (!set.cards.isEmpty()) {
                // Create a new patch object
                Patch p = new Patch();
                p.mExpansion = new Expansion(set, ids);
                p.mCards = new ArrayList<>(set.cards.size());

                // For each card
                boolean validSet = false;
                for (mtgjson_card orig : set.cards) {
                    if (null != orig.identifiers.multiverseId) {
                        validSet = true;
                        break;
                    }
                }
                if (validSet) {
                    boolean found = false;
                    for (codeMapEntry cme : cm.mCodes) {
                        if (set.code.equals(cme.mMtgjsonSetCode)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("No code found for " + set.name);
                    }
                }
                // For each card
//                for (mtgjson_card orig : set.cards) {
//                    // Parse it
//                    Card c = new Card(orig, set);
//                    // If it has a multiverse ID, add it
//                    if (c.mMultiverseId > -1) {
//                        p.mCards.add(c);
//                    }
//                }

                // If any cards are in this set
                if (p.mCards.size() > 0) {
                    p.mExpansion.calculateDigest(gsonReader, p.mCards);
                    // Write the patch file
                    try (FileWriter fw = new FileWriter("patches-v2/" + p.mExpansion.mCode_gatherer + ".json")) {
                        gsonWriter.toJson(p, fw);
                    } catch (IOException e) {
                        System.err.println("Couldn't write patch file");
                        e.printStackTrace();
                        return;
                    }

                    Manifest.ManifestEntry entry = new Manifest.ManifestEntry();
                    entry.mName = p.mExpansion.mName_gatherer;
                    entry.mURL = "https://raw.githubusercontent.com/AEFeinstein/Mtgjson2Familiar/master/patches-v2/" + p.mExpansion.mCode_gatherer + ".json.gzip";
                    entry.mCode = p.mExpansion.mCode_gatherer;
                    entry.mDigest = p.mExpansion.mDigest;
                    entry.mExpansionImageURLs.addAll(p.mExpansion.mExpansionImageURLs);
                    manifest.mPatches.add(entry);
                }
            }
        }

        try (FileWriter fw = new FileWriter("patches.json")) {
            gsonWriter.toJson(manifest, fw);
        } catch (IOException e) {
            System.err.println("Couldn't write manifest file");
            e.printStackTrace();
        }
    }

    /**
     * Create a file mapping Familiar JSON set codes to mtgjson set codes
     * Warning!!! I had to manually edit this file anyway
     * <p>
     * TODO the Promotional Planes set is still a problem, split between PCH (Tazeem) and PCA (Stairs to Infinity)
     *
     * @param gsonReader To read json with
     * @param gsonWriter To write json with
     */
    @SuppressWarnings("unused")
    static void createFamiliarMtgjsonCodeMap(Gson gsonReader, Gson gsonWriter) {
        try (FileReader fr = new FileReader("fam_codes.json");
             FileReader fr2 = new FileReader("mj_codes.json")) {
            codes famCodes = gsonReader.fromJson(fr, codes.class);
            codes mjCodes = gsonReader.fromJson(fr2, codes.class);
            codeMap map = new codeMap();

            for (code fc : famCodes.codes) {
                boolean found = false;
                for (code mjc : mjCodes.codes) {
                    if (mjc.name.equals(fc.name)) {
                        map.mCodes.add(new codeMapEntry(mjc, fc));
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                for (code mjc : mjCodes.codes) {
                    if (mjc.code.equals(fc.code)) {
                        System.out.println("Code match, " + mjc.name + " & " + fc.name);
                        map.mCodes.add(new codeMapEntry(mjc, fc));
                        break;
                    }
                }
            }

            System.out.println("\nNo Matches\n----------");
            for (code fc : famCodes.codes) {
                boolean found = false;
                for (code mjc : mjCodes.codes) {
                    if (mjc.name.equals(fc.name) || mjc.code.equals(fc.code)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println(fc.name + "\t" + fc.code);
                }
            }

            try (FileWriter fw = new FileWriter("setCodeMap.json")) {
                gsonWriter.toJson(map, fw);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
