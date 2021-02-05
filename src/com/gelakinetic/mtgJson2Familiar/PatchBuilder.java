package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.*;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_legalities;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_meta;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_allPrintings;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_metafile;
import com.gelakinetic.mtgfam.helpers.tcgp.TcgpHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PatchBuilder {
    /**
     * Build all the patches with data from mtgjson
     *
     * @return true if the patches were built, false if there was an error
     */
    static boolean buildPatches(File tcgpKeyFile) {

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Building patches");

        // Make a gson object
        Gson gsonReader = new Gson();
        Gson gsonWriter = new GsonBuilder()
                .setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")))
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        // Try to read in the current AllPrintings.json file
        boolean newDownload;
        mtgjson_allPrintings printings = null;
        try (FileReader fr = new FileReader("AllPrintings.json", StandardCharsets.UTF_8)) {
            printings = gsonReader.fromJson(fr, mtgjson_allPrintings.class);
            // Get the latest data from mtgjson if it's newer than what we last used
            newDownload = downloadLatestAllPrintings(gsonReader, printings.meta);
        } catch (FileNotFoundException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "AllPrintings.json not found, downloading");
            newDownload = downloadLatestAllPrintings(gsonReader, null);
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't read AllPrintings.json");
            m2fLogger.logStackTrace(e);
            return false;
        }

        // If a new AllPrintings.json was downloaded, read it
        if (newDownload) {
            // Read in the current AllPrintings.json file
            try (FileReader fr = new FileReader("AllPrintings.json", StandardCharsets.UTF_8)) {
                printings = gsonReader.fromJson(fr, mtgjson_allPrintings.class);
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't read AllPrintings.json");
                m2fLogger.logStackTrace(e);
                return false;
            }
        }

        if (null == printings) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Printings were null!");
            return false;
        }

        // Uncomment this to create the mapping between Familiar set codes and mtgjson set codes
        // createFamiliarMtgjsonCodeMap(gsonReader, gsonWriter);

        setCodeMapper scm = new setCodeMapper(gsonWriter);

        // Get TCGPlayer.com group IDs
        HashMap<Long, String> ids;
        try {
            ids = new TcgpHelper(tcgpKeyFile).getGroupIds();
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't initialize TCGP group IDs");
            m2fLogger.logStackTrace(e);
            return false;
        }

        // Save promo planes to re-add later
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
        legal.mFormats.add(new LegalityData.Format("Historic"));

        // Hack in a reserved list to the legalities
        LegalityData.Format rl = new LegalityData.Format("Reserved List");
        rl.mBanlist.addAll(Arrays.asList(ReservedList.rl));
        legal.mFormats.add(rl);

        // Keep a collection of mtgjson_set objects with merged card lists
        HashMap<String, mtgjson_set> mergedSets = new HashMap<>();

        // Iterate over all sets
        ArrayList<Patch> allPatches = new ArrayList<>();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);

            boolean isArenaOnly = (null != set.checkSetLegality().historic) && set.isOnlineOnly;

            // If the set has cards
            if (!set.cards.isEmpty()) {

                // Create the expansion object
                Expansion newExpansion = new Expansion(set, ids, scm);

                // If this is Mythic Edition
                if (set.code.equals("MED")) {
                    // Split it first, then add it
                    allPatches.addAll(splitMythicEdition(newExpansion, set, scm, legal));
                } else if (set.code.equals("PHOP")) {
                    // If these are promo planes, save them to be re-added to PCH or PC2 later
                    for (mtgjson_card orig : set.cards) {
                        // Parse it
                        Card c = new Card(orig, set, newExpansion, scm);
                        buildCardLegalities(legal, orig, c);
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
                                case "Prerelease Events": {
                                    existingPatch.mExpansion.mName_gatherer = "Promo set for Gatherer";
                                    existingPatch.mExpansion.mName_tcgp = "Media Promos";
                                    existingPatch.mExpansion.mName_mkm = "Media Promos";
                                    break;
                                }
                                case "Magazine Inserts":
                                case "Starter 2000": {
                                    existingPatch.mExpansion.mName_gatherer = "Starter 2000";
                                    break;
                                }
                                case "Magic Online Avatars":
                                case "Vanguard Series": {
                                    existingPatch.mExpansion.mName_gatherer = "Vanguard";
                                    break;
                                }
                            }

                            // If the existing patch was missing information
                            if (null == existingPatch.mExpansion.mName_tcgp) {
                                // Add it
                                existingPatch.mExpansion = newExpansion;
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
                        Card c = new Card(orig, set, newExpansion, scm);
                        // If it has a multiverse ID, add it
                        if (c.mMultiverseId > -1 || isArenaOnly) {
                            newPatch.mCards.add(c);

                            buildCardLegalities(legal, orig, c);
                        }
                    }

                    // If any cards are in this set and it isn't saved yet
                    if (newPatch.mCards.size() > 0) {
                        if (!allPatches.contains(newPatch)) {
                            // Save this patch
                            allPatches.add(newPatch);
                            m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Added " + newPatch.mExpansion.mName_gatherer);
                        }

                        // Merge the mtgjson cards too for a legality check later
                        mtgjson_set mergedSet = mergedSets.get(newPatch.mExpansion.mCode_gatherer);
                        if (null == mergedSet) {
                            mergedSets.put(newPatch.mExpansion.mCode_gatherer, set);
                        } else {
                            mergedSet.cards.addAll(set.cards);
                        }
                    }

                    // Update the rarities
                    newPatch.mExpansion.fetchRaritySymbols(newPatch.mCards);
                }
            }
        }

        // Re-add the promo planes
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

        // Calculate legalities on merged sets
        for (String mCode_gatherer : mergedSets.keySet()) {
            mtgjson_set mergedSet = mergedSets.get(mCode_gatherer);

            // Update the legalities
            mtgjson_legalities setLegality = mergedSet.checkSetLegality();
            for (LegalityData.Format fmt : legal.mFormats) {
                if (("Standard".equals(fmt.mName) && null != setLegality.standard) ||
                        ("Pioneer".equals(fmt.mName) && null != setLegality.pioneer) ||
                        ("Brawl".equals(fmt.mName) && null != setLegality.brawl) ||
                        ("Historic".equals(fmt.mName) && null != setLegality.historic) ||
                        ("Modern".equals(fmt.mName) && null != setLegality.modern)
                    // ("Pauper".equals(fmt.mName) && null != setLegality.pauper) ||
                    // ("Vintage".equals(fmt.mName) && null != setLegality.vintage) ||
                    // ("Legacy".equals(fmt.mName) && null != setLegality.legacy) ||
                    // ("Commander".equals(fmt.mName) && null != setLegality.commander) ||
                ) {
                    fmt.mSets.add(mCode_gatherer);
                }
            }
        }

        // Now write the patches
        Manifest manifest = new Manifest();
        for (Patch p : allPatches) {
            // Sort the cards
            Collections.sort(p.mCards);
            // Now that the patch is fully created, calculate the digest
            p.mExpansion.calculateDigest(p.mCards);
            // Write the patch file
            String patchName = p.mExpansion.mCode_gatherer + ".json.gzip";
            if (!writeFile(p, gsonWriter, new File(Filenames.PATCHES_DIR, patchName), true)) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't write " + patchName);
                return false;
            }
            // Write an uncompressed patch file, for git diffing
            if (!writeFile(p, gsonWriter, new File(Filenames.PATCHES_DIR_UNZIP, p.mExpansion.mCode_gatherer + ".json"), false)) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't write " + p.mExpansion.mCode_gatherer + ".json");
                return false;
            }

            // Add this patch to the manifest
            Manifest.ManifestEntry entry = new Manifest.ManifestEntry();
            entry.mName = p.mExpansion.mName_gatherer;
            entry.mURL = "https://github.com/AEFeinstein/Mtgjson2Familiar/raw/main/patches-v2/" + patchName;
            entry.mCode = p.mExpansion.mCode_gatherer;
            entry.mDigest = p.mExpansion.mDigest;
            if (null != p.mExpansion.mExpansionImageURLs) {
                entry.mExpansionImageURLs.addAll(p.mExpansion.mExpansionImageURLs);
            }
            manifest.mPatches.add(entry);

            m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Wrote " + p.mExpansion.mName_gatherer);
        }

        // Sort the entries
        Collections.sort(manifest.mPatches);
        // Add a timestamp
        manifest.mTimestamp = printings.meta.getTimestamp();
        // And write all the metadata
        if (!writeFile(manifest, gsonWriter, new File(Filenames.PATCHES_DIR, Filenames.PATCHES_FILE), false)) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't write " + Filenames.PATCHES_FILE);
            return false;
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
        if (!writeFile(legal, gsonWriter, new File(Filenames.PATCHES_DIR, Filenames.LEGALITY_FILE), false)) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't write " + Filenames.LEGALITY_FILE);
            return false;
        }

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Done building patches");
        return true;
    }

    /**
     * Compare the saved last mtgjson version with the current version
     * Download and unzip AllPrintings.json if there is a newer version
     *
     * @param gsonReader A Gson to read metadata with
     * @param meta       The old metadata. May be null
     * @return the new metadata
     */
    private static boolean downloadLatestAllPrintings(Gson gsonReader, mtgjson_meta meta) {
        boolean downloaded = false;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL("https://mtgjson.com/api/v5/Meta.json").openStream(), StandardCharsets.UTF_8))) {
            // Read current metadata from the web
            mtgjson_metafile newMeta = gsonReader.fromJson(in, mtgjson_metafile.class);

            // If there's a mismatch
            if (null == meta ||
                    !meta.version.equals(newMeta.data.version) ||
                    !meta.version.equals(newMeta.meta.version)) {
                // Download and unzip new AllPrintings.json.zip
                try (ZipInputStream allPrintings = new ZipInputStream(new URL("https://mtgjson.com/api/v5/AllPrintings.json.zip").openStream())) {
                    ZipEntry zipEntry = allPrintings.getNextEntry();
                    while (zipEntry != null) {
                        File newFile = new File(zipEntry.getName());
                        if (zipEntry.isDirectory()) {
                            if (!newFile.isDirectory() && !newFile.mkdirs()) {
                                throw new IOException("Failed to create directory " + newFile);
                            }
                        } else {
                            Files.copy(allPrintings, Paths.get(zipEntry.getName()), StandardCopyOption.REPLACE_EXISTING);
                            m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Downloaded new AllPrintings.json " + newMeta.data.version);
                        }
                        zipEntry = allPrintings.getNextEntry();
                    }
                    downloaded = true;
                }
            }
        } catch (IOException e) {
            m2fLogger.logStackTrace(e);
            downloaded = false;
        }
        return downloaded;
    }

    /**
     * If this card is banned or restricted in a format, add it to the list
     *
     * @param legal The legality list to add to
     * @param orig  The mtgjson card
     * @param c     THe mtg familiar card
     */
    private static void buildCardLegalities(LegalityData legal, mtgjson_card orig, Card c) {
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
                    ("Historic".equals(fmt.mName) && "Banned".equals(orig.legalities.historic)) ||
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
                    ("Historic".equals(fmt.mName) && "Restricted".equals(orig.legalities.historic)) ||
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

    /**
     * Mythic edition was three sets for Familiar but one set in MTGJSON. This function splits the one set into three
     *
     * @param newExpansion The original MTGJSON MED expansion
     * @param set          The original MTGJSON set
     * @param scm          A set code mapper
     * @param legal        A list of legalities to add cards to
     * @return Three patches for the Mythic Editions
     */
    private static ArrayList<Patch> splitMythicEdition(Expansion newExpansion, mtgjson_set set, setCodeMapper scm, LegalityData legal) {

        // Split it into
        Patch grn = new Patch(new Expansion(newExpansion, "Guilds of Ravnica Mythic Edition", "GRNMED", "Mythic Edition: Guilds of Ravnica"), new ArrayList<>(8));
        Patch rna = new Patch(new Expansion(newExpansion, "Ravnica Allegiance Mythic Edition", "RNAMED", "Mythic Edition: Ravnica Allegiance"), new ArrayList<>(8));
        Patch war = new Patch(new Expansion(newExpansion, "War of the Spark Mythic Edition", "WARMED", "Mythic Edition: War of the Spark"), new ArrayList<>(8));

        // For each card
        for (mtgjson_card orig : set.cards) {
            // Parse it
            Card c = new Card(orig, set, newExpansion, scm);
            // If it has a multiverse ID, add it
            if (c.mMultiverseId > -1) {
                switch (c.mNumber.toUpperCase().charAt(0)) {
                    case 'G': {
                        c.mExpansion = grn.mExpansion.mCode_gatherer;
                        grn.mCards.add(c);
                        break;
                    }
                    case 'R': {
                        c.mExpansion = rna.mExpansion.mCode_gatherer;
                        rna.mCards.add(c);
                        break;
                    }
                    case 'W': {
                        c.mExpansion = war.mExpansion.mCode_gatherer;
                        war.mCards.add(c);
                        break;
                    }
                    default: {
                        m2fLogger.log(m2fLogger.LogLevel.ERROR, "MED card not parsed ~" + c.mName + "~");
                        break;
                    }
                }
            }
            buildCardLegalities(legal, orig, c);
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

        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Added " + grn.mExpansion.mName_gatherer);
        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Added " + rna.mExpansion.mName_gatherer);
        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Added " + war.mExpansion.mName_gatherer);

        return patches;
    }

    /**
     * Helper function to write a Java object to a JSON file
     *
     * @param object     The object to write
     * @param serializer The serializer to convert the object to JSON
     * @param outFile    The file to write to
     */
    static boolean writeFile(Object object, Gson serializer, File outFile, boolean shouldZip) {
        System.setProperty("line.separator", "\n");

        if (shouldZip) {
            try (OutputStreamWriter osw = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outFile)), StandardCharsets.UTF_8)) {
                osw.write(serializer.toJson(object).replace("\r", ""));
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Failed to write " + object.toString() + " to " + outFile);
                m2fLogger.logStackTrace(e);
                return false;
            }
        } else {
            try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
                osw.write(serializer.toJson(object).replace("\r", ""));
            } catch (IOException e) {
                m2fLogger.log(m2fLogger.LogLevel.ERROR, "Failed to write " + object.toString() + " to " + outFile);
                m2fLogger.logStackTrace(e);
                return false;
            }
        }

        return true;
    }
}
