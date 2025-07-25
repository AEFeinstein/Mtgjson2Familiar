package com.gelakinetic.mtgJson2Familiar;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class setCodeMapper {

    private final codeMap cm;

    setCodeMapper(Gson gsonWriter) {
        codeMap cm1;
        try (FileReader fr = new FileReader(new File(Filenames.PATCHES_DIR, Filenames.SET_CODE_MAP_FILE), StandardCharsets.UTF_8)) {
            cm1 = gsonWriter.fromJson(fr, setCodeMapper.codeMap.class);
        } catch (IOException e) {
            cm1 = null;
            m2fLogger.log(m2fLogger.LogLevel.ERROR, "Couldn't read code map");
            m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
        }
        cm = cm1;
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
        try (FileReader fr = new FileReader("fam_codes.json", StandardCharsets.UTF_8);
             FileReader fr2 = new FileReader("mj_codes.json", StandardCharsets.UTF_8)) {
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
                        m2fLogger.log(m2fLogger.LogLevel.DEBUG, "Code match, " + mjc.name + " & " + fc.name);
                        map.mCodes.add(new codeMapEntry(mjc, fc));
                        break;
                    }
                }
            }

            m2fLogger.log(m2fLogger.LogLevel.DEBUG, "\nNo Matches\n----------");
            for (code fc : famCodes.codes) {
                boolean found = false;
                for (code mjc : mjCodes.codes) {
                    if (mjc.name.equals(fc.name) || mjc.code.equals(fc.code)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    m2fLogger.log(m2fLogger.LogLevel.DEBUG, fc.name + "\t" + fc.code);
                }
            }

            try (FileWriter fw = new FileWriter(new File(Filenames.PATCHES_DIR, Filenames.SET_CODE_MAP_FILE), StandardCharsets.UTF_8)) {
                gsonWriter.toJson(map, fw);
            }
        } catch (IOException e) {
            m2fLogger.logStackTrace(m2fLogger.LogLevel.ERROR, e);
        }
    }

    public String getFamiliarCode(String code) {
        for (codeMapEntry cme : cm.mCodes) {
            if (cme.mMtgjsonSetCode.equals(code)) {
                return cme.mFamiliarSetCode;
            }
        }
        // Code not found, must be something new and in sync
        return code;
    }

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
}
