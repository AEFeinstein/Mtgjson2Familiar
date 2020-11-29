package com.gelakinetic.mtgJson2Familiar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GitUtils {

    /**
     * Checkout, reset, and pull this repo
     */
    static void gitCleanup() {
        System.out.println(execSysCmd("git checkout master"));
        System.out.println(execSysCmd("git reset --hard"));
        System.out.println(execSysCmd("git pull"));
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
            e.printStackTrace();
        }
        return null;
    }
}
