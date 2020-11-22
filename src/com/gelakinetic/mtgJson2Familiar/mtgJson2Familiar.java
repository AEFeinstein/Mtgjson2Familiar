package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
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

        setCodeMapper scm = new setCodeMapper(gsonWriter);

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
        ArrayList<Patch> allPatches = new ArrayList<>();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);

            if (!set.cards.isEmpty()) {
                Patch newPatch = null;
                Expansion newExpansion = new Expansion(set, ids, scm);
                for (Patch existingPatch : allPatches) {
                    if (existingPatch.mExpansion.mCode_gatherer.equals(newExpansion.mCode_gatherer)) {
                        newPatch = existingPatch;
                        break;
                    }
                }

                if (null == newPatch) {
                    newPatch = new Patch(newExpansion, new ArrayList<>(set.cards.size()));
                }

//                // For each card
//                boolean validSet = false;
//                for (mtgjson_card orig : set.cards) {
//                    if (null != orig.identifiers.multiverseId) {
//                        validSet = true;
//                        break;
//                    }
//                }
//                if (validSet) {
//                    boolean found = false;
//                    for (setCodeMapper.codeMapEntry cme : cm.mCodes) {
//                        if (set.code.equals(cme.mMtgjsonSetCode)) {
//                            found = true;
//                            break;
//                        }
//                    }
//                    if (!found) {
//                        System.out.println("No code found for " + set.name + " // " + set.code);
//                    }
//                }
                // For each card
                for (mtgjson_card orig : set.cards) {
                    // Parse it
                    Card c = new Card(orig, set, scm);
                    // If it has a multiverse ID, add it
                    if (c.mMultiverseId > -1) {
                        newPatch.mCards.add(c);
                    }
                }

                // If any cards are in this set
                if (newPatch.mCards.size() > 0) {
                    allPatches.add(newPatch);
                    System.out.println("Added " + newPatch.mExpansion.mName_gatherer);
                }
            }
        }

        // Now write the patches
        Manifest manifest = new Manifest();
        for (Patch p : allPatches) {
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
            if (null != p.mExpansion.mExpansionImageURLs) {
                entry.mExpansionImageURLs.addAll(p.mExpansion.mExpansionImageURLs);
            }
            manifest.mPatches.add(entry);

            System.out.println("Wrote " + p.mExpansion.mName_gatherer);
        }

        // And write all the metadata
        try (FileWriter fw = new FileWriter("patches.json")) {
            gsonWriter.toJson(manifest, fw);
        } catch (IOException e) {
            System.err.println("Couldn't write manifest file");
            e.printStackTrace();
        }
    }
}
