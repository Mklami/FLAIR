package launcher;

import java.util.*;

public class Launcher {
    public static void main(String[] args) {
        Map<String, String> arguments = parseArguments(args);
        try {
            Thread.sleep(1000);
            new BoostNSiftLauncher(
                    arguments.get("project"), //LANG
                    arguments.get("workingDir"), // /app/data/output
                    arguments.get("report"), // /app/data/bug_report/LANG_2_0.xml
                    arguments.get("code"), // /app/data/gitrepo
                    arguments.get("granularity") // methodLevelGranularity
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("granularity", "fileLevelGranularity"); // default value

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--project":
                    arguments.put("project", args[++i]);
                    break;
                case "--workingDir":
                    arguments.put("workingDir", args[++i]);
                    break;
                case "--report":
                    arguments.put("report", args[++i]);
                    break;
                case "--code":
                    arguments.put("code", args[++i]);
                    break;
                case "--granularity":
                    arguments.put("granularity", args[++i]);
                    break;
            }
        }

        return arguments;
    }
}
