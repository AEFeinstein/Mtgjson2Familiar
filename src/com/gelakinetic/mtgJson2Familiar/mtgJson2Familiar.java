package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.Card;
import com.gelakinetic.GathererScraper.JsonTypes.Expansion;
import com.gelakinetic.GathererScraper.JsonTypes.LegalityData;
import com.gelakinetic.GathererScraper.JsonTypes.Manifest;
import com.gelakinetic.GathererScraper.JsonTypes.Patch;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_legalities;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_allPrintings;
import com.gelakinetic.mtgfam.helpers.tcgp.TcgpHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

        // Save promo planes to readd later
        ArrayList<Card> promoPlanes = new ArrayList<>();

        // Set up legality data
        LegalityData legal = new LegalityData();
        legal.mFormats.add(new LegalityData.Format("Vintage"));
        legal.mFormats.add(new LegalityData.Format("Standard"));
        legal.mFormats.add(new LegalityData.Format("Pioneer"));
        legal.mFormats.add(new LegalityData.Format("Brawl"));
        legal.mFormats.add(new LegalityData.Format("Modern"));
        legal.mFormats.add(new LegalityData.Format("Legacy"));
        legal.mFormats.add(new LegalityData.Format("Commander"));
        legal.mFormats.add(new LegalityData.Format("Pauper"));

        // Iterate over all sets
        ArrayList<Patch> allPatches = new ArrayList<>();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);

            // If the set has cards
            if (!set.cards.isEmpty()) {
                // Create the expansion object
                Expansion newExpansion = new Expansion(set, ids, scm);

                // If this is Mythic Edition
                if (set.code.equals("MED")) {
                    // Split it first, then add it
                    allPatches.addAll(splitMythicEdition(newExpansion, set, scm));
                } else if (set.code.equals("PHOP")) {
                    for (mtgjson_card orig : set.cards) {
                        // Parse it
                        Card c = new Card(orig, set, scm);
                        // If it has a multiverse ID, store it for later
                        if (c.mMultiverseId > -1) {
                            promoPlanes.add(c);
                        }
                    }
                } else {
                    // Check if this should be merged with an existing expansion
                    Patch newPatch = null;
                    for (Patch existingPatch : allPatches) {
                        if (existingPatch.mExpansion.mCode_gatherer.equals(newExpansion.mCode_gatherer)) {
                            // Match found, use existing expansion
                            newPatch = existingPatch;

                            // Multiple mtgjson sets merged into to one, pick the right name
                            switch (existingPatch.mExpansion.mName_gatherer) {
                                case "Mystery Booster Playtest Cards":
                                case "Mystery Booster": {
                                    existingPatch.mExpansion.mName_gatherer = "Mystery Booster";
                                    break;
                                }
                                case "Archenemy":
                                case "Archenemy Schemes": {
                                    existingPatch.mExpansion.mName_gatherer = "Archenemy";
                                    break;
                                }
                                case "Archenemy: Nicol Bolas":
                                case "Archenemy: Nicol Bolas Schemes": {
                                    existingPatch.mExpansion.mName_gatherer = "Archenemy: Nicol Bolas";
                                    break;
                                }
                                case "Planechase":
                                case "Planechase Planes": {
                                    existingPatch.mExpansion.mName_gatherer = "Planechase";
                                    break;
                                }
                                case "Planechase 2012 Planes":
                                case "Planechase 2012": {
                                    existingPatch.mExpansion.mName_gatherer = "Planechase 2012";
                                    break;
                                }
                                case "Planechase Anthology Planes":
                                case "Planechase Anthology": {
                                    existingPatch.mExpansion.mName_gatherer = "Planechase Anthology";
                                    break;
                                }
                                case "Dragon Con":
                                case "HarperPrism Book Promos":
                                case "Prerelease Events":
                                case "Magazine Inserts":
                                case "Starter 2000": {
                                    existingPatch.mExpansion.mName_gatherer = "Promo set for Gatherer";
                                    break;
                                }
                                case "Magic Online Avatars":
                                case "Vanguard Series": {
                                    existingPatch.mExpansion.mName_gatherer = "Vanguard";
                                    break;
                                }
                            }
                            break;
                        }
                    }

                    // If this isn't being merged with an existing patch, create a new patch
                    if (null == newPatch) {
                        newPatch = new Patch(newExpansion, new ArrayList<>(set.cards.size()));
                    }

                    // For each card
                    for (mtgjson_card orig : set.cards) {
                        // Parse it
                        Card c = new Card(orig, set, scm);
                        // If it has a multiverse ID, add it
                        if (c.mMultiverseId > -1) {
                            newPatch.mCards.add(c);

                            // See if this card is banned or restricted
                            for (LegalityData.Format fmt : legal.mFormats) {
                                // Check for bans
                                if (("Vintage".equals(fmt.mName) && "Banned".equals(orig.legalities.vintage)) ||
                                        ("Standard".equals(fmt.mName) && "Banned".equals(orig.legalities.standard)) ||
                                        ("Pioneer".equals(fmt.mName) && "Banned".equals(orig.legalities.pioneer)) ||
                                        ("Brawl".equals(fmt.mName) && "Banned".equals(orig.legalities.brawl)) ||
                                        ("Modern".equals(fmt.mName) && "Banned".equals(orig.legalities.modern)) ||
                                        ("Legacy".equals(fmt.mName) && "Banned".equals(orig.legalities.legacy)) ||
                                        ("Commander".equals(fmt.mName) && "Banned".equals(orig.legalities.commander)) ||
                                        ("Pauper".equals(fmt.mName) && "Banned".equals(orig.legalities.pauper))) {
                                    if (!fmt.mBanlist.contains(c.mName)) {
                                        fmt.mBanlist.add(c.mName);
                                    }
                                }

                                // Check for restricted
                                if (("Vintage".equals(fmt.mName) && "Restricted".equals(orig.legalities.vintage)) ||
                                        ("Standard".equals(fmt.mName) && "Restricted".equals(orig.legalities.standard)) ||
                                        ("Pioneer".equals(fmt.mName) && "Restricted".equals(orig.legalities.pioneer)) ||
                                        ("Brawl".equals(fmt.mName) && "Restricted".equals(orig.legalities.brawl)) ||
                                        ("Modern".equals(fmt.mName) && "Restricted".equals(orig.legalities.modern)) ||
                                        ("Legacy".equals(fmt.mName) && "Restricted".equals(orig.legalities.legacy)) ||
                                        ("Commander".equals(fmt.mName) && "Restricted".equals(orig.legalities.commander)) ||
                                        ("Pauper".equals(fmt.mName) && "Restricted".equals(orig.legalities.pauper))) {
                                    if (!fmt.mRestrictedlist.contains(c.mName)) {
                                        fmt.mRestrictedlist.add(c.mName);
                                    }
                                }

                                // Exclude these cards from eternal sets
                                if (("Vintage".equals(fmt.mName)) ||
                                        ("Legacy".equals(fmt.mName)) ||
                                        ("Commander".equals(fmt.mName)) ||
                                        ("Pauper".equals(fmt.mName) && c.mRarity == 'C')) {
                                    if (c.mType.startsWith("Scheme") ||
                                            c.mType.startsWith("Ongoing Scheme") ||
                                            c.mType.startsWith("Plane ") ||
                                            c.mType.startsWith("Phenomenon") ||
                                            c.mType.startsWith("Vanguard") ||
                                            c.mType.startsWith("Conspiracy")) {
                                        if (!fmt.mBanlist.contains(c.mName)) {
                                            fmt.mBanlist.add(c.mName);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // If any cards are in this set and it isn't saved yet
                    if (newPatch.mCards.size() > 0 && !allPatches.contains(newPatch)) {
                        // See what formats this set is legal in
                        mtgjson_legalities setLegality = set.checkSetLegality();
                        for (LegalityData.Format fmt : legal.mFormats) {
                            if (("Standard".equals(fmt.mName) && null != setLegality.standard) ||
                                    ("Pioneer".equals(fmt.mName) && null != setLegality.pioneer) ||
                                    ("Brawl".equals(fmt.mName) && null != setLegality.brawl) ||
                                    ("Modern".equals(fmt.mName) && null != setLegality.modern)
                                // ("Pauper".equals(fmt.mName) && null != setLegality.pauper) ||
                                // ("Vintage".equals(fmt.mName) && null != setLegality.vintage) ||
                                // ("Legacy".equals(fmt.mName) && null != setLegality.legacy) ||
                                // ("Commander".equals(fmt.mName) && null != setLegality.commander) ||
                            ) {
                                fmt.mSets.add(newPatch.mExpansion.mCode_gatherer);
                            }
                        }

                        // Update the rarities
                        newPatch.mExpansion.fetchRaritySymbols(newPatch.mCards);

                        // Save this patch
                        allPatches.add(newPatch);
                        System.out.println("Added " + newPatch.mExpansion.mName_gatherer);
                    }
                }
            }
        }

        // Readd the promo planes
        for (Card plane : promoPlanes) {
            if (plane.mName.contains("Tazeem")) {
                for (Patch p : allPatches) {
                    if (p.mExpansion.mCode_gatherer.equals("PCH")) {
                        plane.mExpansion = p.mExpansion.mCode_gatherer;
                        p.mCards.add(plane);
                        break;
                    }
                }
            } else if (plane.mName.contains("Stairs to Infinity")) {
                for (Patch p : allPatches) {
                    if (p.mExpansion.mCode_gatherer.equals("PC2")) {
                        plane.mExpansion = p.mExpansion.mCode_gatherer;
                        p.mCards.add(plane);
                        break;
                    }
                }
            }
        }

        // Now write the patches
        Manifest manifest = new Manifest();
        for (Patch p : allPatches) {
            // Now that the patch is fully created, calculate the digest
            p.mExpansion.calculateDigest(gsonReader, p.mCards);
            // Write the patch file
            try (FileWriter fw = new FileWriter("patches-v2/" + p.mExpansion.mCode_gatherer + ".json")) {
                gsonWriter.toJson(p, fw);
            } catch (IOException e) {
                System.err.println("Couldn't write patch file");
                e.printStackTrace();
                return;
            }

            // Add this patch to the manifest
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

        // Sort the legal data
        for (LegalityData.Format fmt : legal.mFormats) {
            Collections.sort(fmt.mBanlist);
            Collections.sort(fmt.mRestrictedlist);
            Collections.sort(fmt.mSets);
        }
        // Add a timestamp
        legal.mTimestamp = printings.meta.getTimestamp();

        // Write the legal data
        try (FileWriter fw = new FileWriter("legality.json")) {
            gsonWriter.toJson(legal, fw);
        } catch (IOException e) {
            System.err.println("Couldn't write legalities");
            e.printStackTrace();
        }
    }

    /**
     * Mythic edition was three sets for Familiar but one set in MTGJSON. This function splits the one set into three
     *
     * @param newExpansion The original MTGJSON MED expansion
     * @param set          The original MTGJSON set
     * @param scm          A set code mapper
     * @return Three patches for the Mythic Editions
     */
    private static ArrayList<Patch> splitMythicEdition(Expansion newExpansion, mtgjson_set set, setCodeMapper scm) {

        // Split it into
        Patch grn = new Patch(new Expansion(newExpansion, "Guilds of Ravnica Mythic Edition", "GRNMED", "Mythic Edition: Guilds of Ravnica"), new ArrayList<>(8));
        Patch rna = new Patch(new Expansion(newExpansion, "Ravnica Allegiance Mythic Edition", "RNAMED", "Mythic Edition: Ravnica Allegiance"), new ArrayList<>(8));
        Patch war = new Patch(new Expansion(newExpansion, "War of the Spark Mythic Edition", "WARMED", "Mythic Edition: War of the Spark"), new ArrayList<>(8));

        // For each card
        for (mtgjson_card orig : set.cards) {
            // Parse it
            Card c = new Card(orig, set, scm);
            // If it has a multiverse ID, add it
            if (c.mMultiverseId > -1) {
                switch (c.mNumber.toUpperCase().charAt(0)) {
                    case 'G': {
                        c.mExpansion = "GRNMED";
                        grn.mCards.add(c);
                    }
                    case 'R': {
                        c.mExpansion = "RNAMED";
                        rna.mCards.add(c);
                    }
                    case 'W': {
                        c.mExpansion = "WARMED";
                        war.mCards.add(c);
                    }
                }
            }
        }

        // Update the rarities
        grn.mExpansion.fetchRaritySymbols(grn.mCards);
        rna.mExpansion.fetchRaritySymbols(rna.mCards);
        war.mExpansion.fetchRaritySymbols(war.mCards);

        // Return the patches
        ArrayList<Patch> patches = new ArrayList<>(3);
        patches.add(grn);
        patches.add(rna);
        patches.add(war);

        System.out.println("Added " + grn.mExpansion.mName_gatherer);
        System.out.println("Added " + rna.mExpansion.mName_gatherer);
        System.out.println("Added " + war.mExpansion.mName_gatherer);

        return patches;
    }
}
