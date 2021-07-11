package com.gelakinetic.mtgJson2Familiar;

import java.io.File;

public class mtgJson2Familiar {

    public static void main(String[] args) {

        // Parse the arguments
        boolean scrapeRules = false;
        boolean scrapeJudgeDocs = false;
        boolean buildPatches = false;
        boolean printUsage = false;
        boolean keyFileNext = false;
        File tcgpKeyFile = null;

        for (String arg : args) {
            if (keyFileNext) {
                tcgpKeyFile = new File(arg);
                keyFileNext = false;
            } else {
                switch (arg) {
                    case "-p": {
                        buildPatches = true;
                        break;
                    }
                    case "-r": {
                        scrapeRules = true;
                        break;
                    }
                    case "-j": {
                        scrapeJudgeDocs = true;
                        break;
                    }
                    case "-k": {
                        keyFileNext = true;
                        break;
                    }
                    case "-v": {
                        m2fLogger.setLogLevel(m2fLogger.LogLevel.DEBUG);
                        break;
                    }
                    default: {
                        printUsage = true;
                        break;
                    }
                }
            }
        }

        if (printUsage || (!scrapeRules && !scrapeJudgeDocs && !(buildPatches && null != tcgpKeyFile))) {
            System.out.print("Usage\n" +
                    "  -p       : build patches\n" +
                    "  -k [file]: tcgp key file\n" +
                    "  -r       : build comprehensive rules\n" +
                    "  -j       : build judge documents\n" +
                    "  -v       : enable verbose debug\n");
            return;
        }

        int status = 0;

        if (scrapeJudgeDocs) {
            if (!JudgeDocConverter.GetLatestDocs()) {
                status -= 2;
            }
        }

        if (scrapeRules) {
            if (!new CompRulesScraper().GetLatestRules()) {
                status -= 4;
            }
        }

        if (buildPatches && null != tcgpKeyFile) {
            if (!PatchBuilder.buildPatches(tcgpKeyFile)) {
                status -= 8;
            }
        }

        System.exit(status);
    }
}
