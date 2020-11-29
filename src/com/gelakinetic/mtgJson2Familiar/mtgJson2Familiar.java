package com.gelakinetic.mtgJson2Familiar;

public class mtgJson2Familiar {

    public static void main(String[] args) {

        // Parse the arguments
        boolean scrapeRules = false;
        boolean scrapeJudgeDocs = false;
        boolean buildPatches = false;
        boolean nextArgIsUsername = false;
        boolean nextArgIsPassword = false;
        String gitUsername = null;
        String gitPassword = null;
        for (String arg : args) {
            if (nextArgIsPassword) {
                nextArgIsPassword = false;
                gitPassword = arg;
            } else if (nextArgIsUsername) {
                nextArgIsUsername = false;
                gitUsername = arg;
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
                    case "-u": {
                        nextArgIsUsername = true;
                        break;
                    }
                    case "-w": {
                        nextArgIsPassword = true;
                        break;
                    }
                }
            }
        }

        if ((!scrapeRules && !scrapeJudgeDocs && !buildPatches) ||
                ((gitUsername != null && gitPassword == null) || (gitUsername == null && gitPassword != null))) {
            System.out.print("Usage\n" +
                    "  -p : build patches\n" +
                    "  -r : build comprehensive rules\n" +
                    "  -j : build judge documents\n" +
                    "  -u [username] : A git username to push changes\n" +
                    "  -w [password] : A git password to push changes\n");
            return;
        }

        int status = 0;

        // If a git username and password were supplied, clean the repo first
        if (gitUsername != null) {
            GitUtils.gitCleanup();
        }

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
