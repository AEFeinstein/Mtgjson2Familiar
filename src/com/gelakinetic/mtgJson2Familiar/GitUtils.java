package com.gelakinetic.mtgJson2Familiar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GitUtils {

    /**
     * Checkout, reset, and pull this repo
     *
     * @return true if the repo was cleaned, false if there was an error
     */
    static boolean gitCleanup() {
        String ret;

        ret = execSysCmd("git checkout master");
        if (null != ret) {
            System.out.println(ret);
        } else {
            return false;
        }

        ret = execSysCmd("git reset --hard");
        if (null != ret) {
            System.out.println(ret);
        } else {
            return false;
        }

        ret = execSysCmd("git pull");
        if (null != ret) {
            System.out.println(ret);
        } else {
            return false;
        }

        return true;
    }

    /**
     * Execute a system command and return the output
     *
     * @param command The command to execute
     * @return The string output for this command
     */
    private static String execSysCmd(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                output.append("# ").append(command).append('\n');
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
                return output.toString();
            }
        } catch (IOException e) {
            System.err.println("Error executing \"" + command + "\"");
            e.printStackTrace();
        }
        return null;
    }
}
