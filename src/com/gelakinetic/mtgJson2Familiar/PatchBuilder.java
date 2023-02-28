package com.gelakinetic.mtgJson2Familiar;

import com.gelakinetic.GathererScraper.JsonTypes.*;
import com.gelakinetic.mtgJson2Familiar.gsonSerializer.AlphabeticalJsonSerializer;
import com.gelakinetic.mtgJson2Familiar.gsonSerializer.PrefixedFieldNamingStrategy;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_card;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_meta;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_set;
import com.gelakinetic.mtgJson2Familiar.mtgjsonClasses.mtgjson_token;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_allPrintings;
import com.gelakinetic.mtgJson2Familiar.mtgjsonFiles.mtgjson_metafile;
import com.gelakinetic.mtgfam.helpers.tcgp.TcgpHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PatchBuilder {

    private static final List<String> bannedInHistoricJMP = Arrays.asList(
            "Ajani's Chosen",
            "Angelic Arbiter",
            "Path to Exile",
            "Read the Runes",
            "Rhystic Study",
            "Thought Scour",
            "Exhume",
            "Mausoleum Turnkey",
            "Reanimate",
            "Scourge of Nel Toth",
            "Ball Lightning",
            "Chain Lightning",
            "Draconic Roar",
            "Flametongue Kavu",
            "Goblin Lore",
            "Fa'adiyah Seer",
            "Scrounging Bandar",
            "Time to Feed");
    private static final List<String> bannedInHistoricJ21 = Arrays.asList(
            "Fog",
            "Kraken Hatchling",
            "Ponder",
            "Regal Force",
            "Stormfront Pegasus",
            "Force Spike",
            "Assault Strobe",
            "Tropical Island");

    /**
     * Build all the patches with data from mtgjson
     *
     * @return true if the patches were built, false if there was an error
     */
    static boolean buildPatches(File tcgpKeyFile) {

        m2fLogger.log(m2fLogger.LogLevel.INFO, "Building patches");

        // Make a gson object
        EmptyStringToNumberTypeAdapter numberTypeAdapter = new EmptyStringToNumberTypeAdapter();
        Gson gsonReader = new GsonBuilder()
                .registerTypeAdapter(Double.class, numberTypeAdapter)
                .registerTypeAdapter(double.class, numberTypeAdapter)
                .registerTypeAdapter(Float.class, numberTypeAdapter)
                .registerTypeAdapter(float.class, numberTypeAdapter)
                .registerTypeAdapter(Long.class, numberTypeAdapter)
                .registerTypeAdapter(long.class, numberTypeAdapter)
                .registerTypeAdapter(Integer.class, numberTypeAdapter)
                .registerTypeAdapter(int.class, numberTypeAdapter)
                .create();
        // Top level JSON objects
        AlphabeticalJsonSerializer<LegalityData> legalitySerializer = new AlphabeticalJsonSerializer<>(false);
        AlphabeticalJsonSerializer<Manifest> manifestSerializer = new AlphabeticalJsonSerializer<>(false);
        AlphabeticalJsonSerializer<Patch> patchSerializer = new AlphabeticalJsonSerializer<>(false);
        // Nested JSON objects
        AlphabeticalJsonSerializer<Card> cardSerializer = new AlphabeticalJsonSerializer<>(false);
        AlphabeticalJsonSerializer<Expansion> expansionSerializer = new AlphabeticalJsonSerializer<>(false);
        AlphabeticalJsonSerializer<Manifest.ManifestEntry> manifestEntrySerializer = new AlphabeticalJsonSerializer<>(false);
        AlphabeticalJsonSerializer<LegalityData.Format> formatSerializer = new AlphabeticalJsonSerializer<>(false);

        Gson gsonWriter = new GsonBuilder()
                .setFieldNamingStrategy((new PrefixedFieldNamingStrategy("m")))
                .registerTypeHierarchyAdapter(LegalityData.class, legalitySerializer)
                .registerTypeHierarchyAdapter(Manifest.class, manifestSerializer)
                .registerTypeHierarchyAdapter(Patch.class, patchSerializer)
                .registerTypeHierarchyAdapter(Card.class, cardSerializer)
                .registerTypeHierarchyAdapter(Expansion.class, expansionSerializer)
                .registerTypeHierarchyAdapter(Manifest.ManifestEntry.class, manifestEntrySerializer)
                .registerTypeHierarchyAdapter(LegalityData.Format.class, formatSerializer)
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create();

        // Get the metadata for the local "AllPrintings.json" file
        mtgjson_allPrintings printings;
        try (FileReader fr = new FileReader("AllPrintings.json", StandardCharsets.UTF_8)) {
            printings = gsonReader.fromJson(fr, mtgjson_allPrintings.class);
        } catch (FileNotFoundException e) {
            printings = null;
        } catch (IOException e) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't read AllPrintings.json");
            m2fLogger.logStackTrace(e);
            return false;
        }

        // Try to read in the current AllPrintings.json file
        boolean newDownload;
        if (null == printings) {
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "AllPrintings.json not found, downloading");
            newDownload = downloadLatestAllPrintings(gsonReader, null);
        } else {
            // Get the latest data from mtgjson if it's newer than what we last used
            newDownload = downloadLatestAllPrintings(gsonReader, printings.meta);
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

        // Get list of Expansions on Gatherer
        ArrayList<String> gathererExpansions = getGathererExpansionList();

        // Save promo planes to re-add later
        ArrayList<Card> promoPlanes = new ArrayList<>();

        // Set up legality data
        LegalityData legal = new LegalityData();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);
            for (mtgjson_card card : set.cards) {
                for (String format : card.legalities.keySet()) {
                    LegalityData.Format checkFmt = new LegalityData.Format(Card.beautifyFormat(format));
                    if (!legal.mFormats.contains(checkFmt)) {
                        legal.mFormats.add(checkFmt);
                    }
                }
            }
        }

        // Hack in a reserved list to the legalities
        legal.mFormats.add(new LegalityData.Format("Reserved List"));

        // Keep a collection of mtgjson_set objects with merged card lists
        HashMap<String, mtgjson_set> mergedSets = new HashMap<>();

        // Iterate over all sets
        ArrayList<Patch> allPatches = new ArrayList<>();
        for (String key : printings.data.keySet()) {
            mtgjson_set set = printings.data.get(key);
            set.removeDuplicateUuids();

            // YMID used to be Y22, and we can't have that changing on us!
            if ("YMID".equalsIgnoreCase(set.code)) {
                set.code = "Y22";
            }

            boolean isArenaOnly = set.isArenaOnly();

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
                        buildCardLegalities(legal, c);
                        // If it has a multiverse ID, store it for later
                        if (c.mMultiverseId > -1) {
                            promoPlanes.add(c);
                        }
                    }
                } else {
                    // Check if this should be merged with an existing expansion
                    Patch newPatch = new Patch(newExpansion, new ArrayList<>(set.cards.size()));
                    for (Patch existingPatch : allPatches) {
                        if (existingPatch.mExpansion.mCode_gatherer.equals(newExpansion.mCode_gatherer)) {

                            // Multiple mtgjson sets merged into to one, pick the right metadata and name
                            String nameToUse = null;
                            String mdToUse = null;
                            switch (existingPatch.mExpansion.mName_gatherer) {
                                case "Mystery Booster Playtest Cards 2019":
                                case "Mystery Booster": {
                                    nameToUse = "Mystery Booster";
                                    mdToUse = "Mystery Booster Playtest Cards 2019";
                                    break;
                                }
                                case "Archenemy":
                                case "Archenemy Schemes": {
                                    nameToUse = "Archenemy";
                                    mdToUse = "Archenemy";
                                    break;
                                }
                                case "Archenemy: Nicol Bolas":
                                case "Archenemy: Nicol Bolas Schemes": {
                                    nameToUse = "Archenemy: Nicol Bolas";
                                    mdToUse = "Archenemy: Nicol Bolas";
                                    break;
                                }
                                case "Planechase":
                                case "Planechase Planes": {
                                    nameToUse = "Planechase";
                                    mdToUse = "Planechase";
                                    break;
                                }
                                case "Planechase 2012 Planes":
                                case "Planechase 2012": {
                                    nameToUse = "Planechase 2012";
                                    mdToUse = "Planechase 2012";
                                    break;
                                }
                                case "Planechase Anthology Planes":
                                case "Planechase Anthology": {
                                    nameToUse = "Planechase Anthology";
                                    mdToUse = "Planechase Anthology";
                                    break;
                                }
                                case "Dragon Con":
                                case "HarperPrism Book Promos":
                                case "Prerelease Events":
                                case "Promo set for Gatherer": {
                                    nameToUse = "Promo set for Gatherer";
                                    mdToUse = "Promo set for Gatherer";
                                    break;
                                }
                                case "Magazine Inserts":
                                case "Media Inserts":
                                case "Starter 2000": {
                                    nameToUse = "Starter 2000";
                                    mdToUse = "Starter 2000";
                                    break;
                                }
                                case "Magic Online Avatars":
                                case "Vanguard Series": {
                                    nameToUse = "Vanguard Series";
                                    mdToUse = "Vanguard Series";
                                    break;
                                }
                            }

                            // Set the metadata properly
                            existingPatch.setMetaData(newPatch, nameToUse, mdToUse);

                            // Match found, use existing expansion
                            newPatch = existingPatch;

                            break;
                        }
                    }

                    boolean isSecretLair = "SLD".equals(newPatch.mExpansion.mCode_gatherer) ||
                            "SLU".equals(newPatch.mExpansion.mCode_gatherer);

                    boolean setHasMultiverseId = false;
                    for (mtgjson_card orig : set.cards) {
                        if (orig.identifiers.multiverseId != null) {
                            setHasMultiverseId = true;
                            break;
                        }
                    }

                    // For each card
                    for (mtgjson_card orig : set.cards) {
                        // Parse it
                        Card c = new Card(orig, set, newExpansion, scm);
                        // If it has a multiverse ID, or is +2 Mace, add it
                        if (setHasMultiverseId || isArenaOnly || isSecretLair) {
                            newPatch.mCards.add(c);

                            buildCardLegalities(legal, c);
                        }
                    }

                    // For dungeons
                    for (mtgjson_token token : set.tokens) {
                        if (token.type.contains("Dungeon")) {
                            Card c = new Card(token, set, newExpansion, scm);
                            newPatch.mCards.add(c);
                        }
                    }

                    // Ensure that Candlelit Cavalry exists
                    if ("MID".equals(newPatch.mExpansion.mCode_gatherer)) {
                        CandlelitCavalry cc = new CandlelitCavalry();
                        if (!newPatch.mCards.contains(cc)) {
                            newPatch.mCards.add(cc);
                        }
                    }

                    // Check if we're less than two weeks away from a release (one week from a prerelease)
                    boolean isReleaseSoon = false;
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
                        long releaseSinceEpochS = sdf.parse(set.releaseDate).getTime() / 1000;
                        long currentSinceEpochS = java.time.Instant.now().getEpochSecond();
                        long secUntilRelease = releaseSinceEpochS - currentSinceEpochS;
                        if (0 < secUntilRelease && secUntilRelease < (60 * 60 * 24 * 14)) {
                            isReleaseSoon = true;
                        }
                    } catch (ParseException e) {
                        m2fLogger.log(m2fLogger.LogLevel.ERROR, e.getMessage());
                    }

                    // If no cards were added
                    if (newPatch.mCards.isEmpty()) {
                        // but the expansion exists on Gatherer
                        boolean existsOnGatherer = false;
                        for (String gathererExpansion : gathererExpansions) {
                            if (gathererExpansion.toLowerCase().contains(set.name.toLowerCase())) {
                                existsOnGatherer = true;
                                break;
                            }
                        }

                        // If the expansion is on gatherer, or it's of a specific type
                        // and not a partial preview or is releasing soon, add it
                        if (existsOnGatherer ||
                                ((!set.isPartialPreview || isReleaseSoon) &&
                                        ("archenemy".equals(set.type) ||
                                                "arsenal".equals(set.type) ||
                                                "commander".equals(set.type) ||
                                                "duel_deck".equals(set.type) ||
                                                "expansion".equals(set.type) ||
                                                "spellbook".equals(set.type) ||
                                                "draft_innovation".equals(set.type)))) {
                            m2fLogger.log(m2fLogger.LogLevel.INFO, "Adding " + set.name + " (no multiverseID, on Gatherer)");

                            // Add all the cards anyway
                            for (mtgjson_card orig : set.cards) {
                                // Parse it
                                Card c = new Card(orig, set, newExpansion, scm);
                                newPatch.mCards.add(c);
                                buildCardLegalities(legal, c);
                            }
                        }
                    }

                    // If any cards are in this set, and it isn't saved yet
                    if (newPatch.mCards.size() > 0 && ((!set.isPartialPreview || isReleaseSoon))) {
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
            HashMap<String, String> setLegality = mergedSet.checkSetLegality();
            for (LegalityData.Format fmt : legal.mFormats) {
                if (setLegality.containsKey(fmt.mName) && "Legal".equals(setLegality.get(fmt.mName))) {
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
     * @param c     THe mtg familiar card
     */
    private static void buildCardLegalities(LegalityData legal, Card c) {

        // These cards are conjured, not legal for deck construction
        if ((c.mExpansion.equals("JMP") && bannedInHistoricJMP.contains(c.mName)) ||
                (c.mExpansion.equals("J21") && bannedInHistoricJ21.contains(c.mName))) {
            c.mLegalities.put("Historic", "Banned");
        }

        // See if this card is banned or restricted
        for (LegalityData.Format fmt : legal.mFormats) {
            if ("Banned".equals(c.mLegalities.get(fmt.mName))) {
                if (!fmt.mBanlist.contains(c.mName)) {
                    fmt.mBanlist.add((c.mName));
                }
            }
            if ("Restricted".equals(c.mLegalities.get(fmt.mName))) {
                if (!fmt.mRestrictedlist.contains(c.mName)) {
                    fmt.mRestrictedlist.add((c.mName));
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
            buildCardLegalities(legal, c);
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
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    /**
     * Get a list of expansions listed on Gatherer
     *
     * @return The list of expansions on Gatherer, all lowercase
     */
    static ArrayList<String> getGathererExpansionList() {
        ArrayList<String> expansions = new ArrayList<>();
        Document gathererMain = NetUtils.ConnectWithRetries("https://gatherer.wizards.com/");
        if (null != gathererMain) {
            Elements expansionElements = gathererMain.getElementsByAttributeValueContaining("name", "setAddText");

            for (Element expansionElement : expansionElements) {
                for (Element e : expansionElement.getAllElements()) {
                    if (e.ownText().length() > 0) {
                        expansions.add(e.ownText().toLowerCase());
                    }
                }
            }
        }
        return expansions;
    }

    public static class EmptyStringToNumberTypeAdapter extends TypeAdapter<Number> {
        @Override
        public void write(JsonWriter jsonWriter, Number number) throws IOException {
            if (number == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(number);
        }

        @Override
        public Number read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            try {
                String value = jsonReader.nextString();
                if ("".equals(value)) {
                    return 0;
                }
                return NumberUtils.createNumber(value);
            } catch (NumberFormatException e) {
                throw new JsonSyntaxException(e);
            }
        }
    }
}
