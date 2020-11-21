package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
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
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);

            if (!set.cards.isEmpty()) {
                // Create a new patch object
                Patch p = new Patch();
                p.mExpansion = new Expansion(set, ids);
                p.mCards = new ArrayList<>(set.cards.size());
                System.out.println(key + " " + p.mExpansion.mName_gatherer);

                // For each card
                for (mtgjson_card orig : set.cards) {
                    // Parse it
                    Card c = new Card(orig, set);
                    // If it has a multiverse ID, add it
                    if (c.mMultiverseId > -1) {
                        p.mCards.add(c);
                    }
                }

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
                }
            } else {
                System.out.println("!!! Empty set " + set.name);
            }
        }
    }
}
