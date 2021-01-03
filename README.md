# Mtgjson2Familiar

It's one stop shop for generating data for MTG Familiar!

- Card expansion patches from https://mtgjson.com/
- Comprehensive rules from https://magic.wizards.com/en/game-info/gameplay/rules-and-formats/rules
- Judge documents from https://blogs.magicjudges.org/o/rules-policy-documents/

This is an IntelliJ IDEA project, which can also be opened in Android Studio. To build a JAR, follow these instructions: https://www.jetbrains.com/help/idea/compiling-applications.html#build_artifact

# Setup

1. Install the Github CLI from https://github.com/cli/cli#installation
1. Install at least Java 12, or equivalent JRE
1. Clone this repository from https://github.com/AEFeinstein/Mtgjson2Familiar.git
1. Create or obtain a `gh-token.txt` file containing a github personal access token and put it in this project's root directory. It should look like this:
    ```
   XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    ```
1. Create or obtain a `tcgp_keys.json` file with TCGPlayer.com API keys from http://developer.tcgplayer.com/ and put it in this project's root directory. It should look like this:
    ```
    {
      "ACCESS_TOKEN": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
      "PRIVATE_KEY": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX",
      "PUBLIC_KEY": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
    }
    ```
1. Use `cron` to run `update_data.sh -p -r -k tcgp_keys.json` daily, **except Tuesdays** at 2am EST. This checks for new cards and comprehensive rules, which is fast because version can be checked before parsing.
1. Use `cron` to run `update_data.sh -p -r -k tcgp_keys.json -j` weekly, on Tuesdays at 2am EST. This also checks for new judge documents, which is slow because the judge documents aren't versioned and must be parsed each time.
