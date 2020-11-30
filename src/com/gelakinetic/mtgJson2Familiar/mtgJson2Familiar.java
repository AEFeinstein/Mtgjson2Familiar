package com.gelakinetic.mtgJson2Familiar;

public class mtgJson2Familiar {

    public static void main(String[] args) {

        // Parse the arguments
        boolean scrapeRules = false;
        boolean scrapeJudgeDocs = false;
        boolean buildPatches = false;
        boolean printUsage = false;

        for (String arg : args) {
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
                default: {
                    printUsage = true;
                    break;
                }
            }
        }

        if (printUsage || (!scrapeRules && !scrapeJudgeDocs && !buildPatches)) {
            System.out.print("Usage\n" +
                    "  -p : build patches\n" +
                    "  -r : build comprehensive rules\n" +
                    "  -j : build judge documents\n");
            return;
        }

        int status = 0;

        if (scrapeJudgeDocs) {
            if (!new JudgeDocScraper().ScrapeAll()) {
                status -= 2;
            }
        }

        if (scrapeRules) {
            if (!new CompRulesScraper().GetLatestRules()) {
                status -= 4;
            }
        }

        if (buildPatches) {
            if (!new PatchBuilder().buildPatches()) {
                status -= 8;
            }
        }

        System.exit(status);
    }
}
