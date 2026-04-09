package preprocess;

import generics.Bug;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import utils.ChangeLocator;
import utils.FileToLines;
import utils.GitHelp;
import utils.ReadBugsFromXML;
import utils.WriteLinesToFile;

public class ExtractCommits {
	public static List<Bug> bugs;
	public static HashSet<String> validCommits;
	public static String loc = main.Main.settings.get("workingLoc");
	public static String repo = main.Main.settings.get("repoDir");
	public static HashSet<String> concernedCommits;
	public static HashSet<String> changeMap;
	public static HashMap<String,Long> changeTime;
	public static void indexHunks() throws Exception {
		getCommitsOneLine();
		loadCommits();
		extractHunks();
	}
	
	public static void getCommitsOneLine() throws Exception{
		String logFile = loc + File.separator + "logOneline.txt";
		File file = new File(logFile);
		// Always regenerate if file doesn't exist or is empty
		if (!file.exists() || file.length() == 0) {
			System.out.println("Generating logOneline.txt from repository: " + repo);
			String content = GitHelp.getAllCommitOneLine(repo);
			if (content != null && !content.trim().isEmpty()) {
				System.out.println("Successfully retrieved " + content.split("\n").length + " commits");
				WriteLinesToFile.writeToFiles(content, logFile);
			} else {
				System.err.println("Warning: git log returned empty result for repository: " + repo);
				System.err.println("Repository path: " + repo);
				// Try to verify repository exists and is accessible
				File repoDir = new File(repo);
				if (!repoDir.exists()) {
					System.err.println("ERROR: Repository directory does not exist: " + repo);
				} else if (!new File(repoDir, ".git").exists()) {
					System.err.println("ERROR: Repository is not a valid git repository (no .git directory): " + repo);
				} else {
					System.err.println("Repository exists but git log returned empty. This may indicate a problem with git access.");
				}
				// Write empty file to prevent repeated attempts
				WriteLinesToFile.writeToFiles("", logFile);
			}
		} else {
			System.out.println("logOneline.txt already exists with " + file.length() + " bytes, skipping regeneration");
		}
	}

	public static void loadCommits() throws ParseException {
		String commitFile = "";
		if (main.Main.settings.containsKey("concernedCommit"))
			commitFile = main.Main.settings.get("concernedCommit");
		List<String> lines = null;
		changeMap = ChangeLocator.getShortChangeMap();
		if (commitFile.equals("")) {
			changeTime = ChangeLocator.getChangeTime();
			List<Bug> bugs = ReadBugsFromXML.getFixedBugsFromXML(main.Main.settings.get("bugReport"));
			HashMap<Integer,HashSet<String>> bugConcernedCommits = new HashMap<Integer, HashSet<String>>();
			
			// Initialize HashSet for all bugs first to avoid null pointer exceptions
			for (Bug bug : bugs) {
				bugConcernedCommits.put(bug.id, new HashSet<String>());
			}
			
			System.out.println(changeMap.size());
			for (String change : changeTime.keySet()) {
				for (Bug bug : bugs) {
					if (changeTime.get(change) < bug.reportTime)
						bugConcernedCommits.get(bug.id).add(change);
				}
			}
			concernedCommits = new HashSet<String>();
			lines = new ArrayList<String>();
			for (Bug bug : bugs) {
				HashSet<String> commits = bugConcernedCommits.get(bug.id);
				if (commits == null) {
					commits = new HashSet<String>();
				}
				lines.add(bug.id + "\t" + commits.toString());
				concernedCommits.addAll(commits);
			}
			System.out.println(concernedCommits.size());
			WriteLinesToFile.writeLinesToFile(lines, loc + File.separator + "concernedCommits.txt");
			
		} else {
			lines = FileToLines.fileToLines(commitFile);
			System.out.println(commitFile);
			concernedCommits = new HashSet<String>();
			for (String line : lines) {
	            System.out.println(line);
				concernedCommits.add(line.split("\t")[0].trim());
			}
		}
	}
	
	public static void extractHunks() throws Exception {
		System.out.print("Extracting Commits");
		String revisionLoc = loc + File.separator + "revisions";
		if (main.Main.settings.containsKey("revisionsLoc"))
			revisionLoc = main.Main.settings.get("revisionsLoc");
		File file = new File(revisionLoc);
		if (!file.exists())
			file.mkdirs();
		
		int count = 0;
		int percent = 0;
		int max = concernedCommits.size();
		for (String hash : concernedCommits) {
			count++;
			if (!changeMap.contains(hash)) continue;

			File parentPath = new File(revisionLoc + File.separator + hash.substring(0,2) + File.separator + hash.substring(2,4));
            //file = new File(revisionLoc + File.separator + fullHash);
			if (!parentPath.exists())
				parentPath.mkdirs();
			
			String commitFile = parentPath.getAbsolutePath() + File.separator + hash + ".txt";
			file = new File(commitFile);
			if (!file.exists()) {
				String content = GitHelp.gitShow(hash, repo);
				WriteLinesToFile.writeToFiles(content, commitFile);
			}
			
			int newpercent = (int)((count*100) / (double)max);					
			if (newpercent > percent){
				if (newpercent != 0 && newpercent != 100 && newpercent%10==0)
					System.out.print(",");
				else if(newpercent%2==0)
					System.out.print(".");
				percent = newpercent;
			}
		}
		System.out.println("Done.");
	}

}
