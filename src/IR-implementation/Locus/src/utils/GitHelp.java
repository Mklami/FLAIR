package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import generics.Commit;

public class GitHelp {
	public static String getAllCommitOneLine(String repo) 
		throws Exception{
		ExecCommand executor = new ExecCommand();
		
		// First, verify repository exists
		java.io.File repoFile = new java.io.File(repo);
		if (!repoFile.exists()) {
			System.err.println("ERROR: Repository path does not exist: " + repo);
			return null;
		}
		
		// Use -c safe.directory='*' flag to bypass ownership check (works in Docker)
		// This is more reliable than setting config globally
		String[] command = {"git", "-c", "safe.directory=*", "log", "--all", "--pretty=format:%h%x09%an%x09%ad%x09%s"};
		String result = executor.execOneThread(command, repo);
		if (result == null || result.trim().isEmpty()) {
			// Fallback: try with simpler format if the complex one fails
			String[] simpleCommand = {"git", "-c", "safe.directory=*", "log", "--all", "--oneline"};
			result = executor.execOneThread(simpleCommand, repo);
		}
		return result;
	} 
	
	public static String getAllCommitWithFullDescription(String repo) 
		throws Exception {
		ExecCommand executor = new ExecCommand();
		String result = executor.exec("git log", repo);
		return result;
	}
	
	public static String getAllCommitWithChangedFiles(String repo) 
		throws Exception {
		ExecCommand executor = new ExecCommand();
		String result = executor.exec("git log --name-only --oneline", repo);
		return result;
	}
	
	public static String gitShow(String hash, String repo) 
			throws Exception{
		ExecCommand executor = new ExecCommand();
		String[] command = {"git", "-c", "safe.directory=*", "show", hash};
		String result = executor.execOneThread(command, repo);
		return result;
	}
	
	public static String gitAnnotate(String file, String rev, String repo) 
			throws Exception {
		ExecCommand executor = new ExecCommand();
//		System.out.println("git annotate " + file + " " + rev + "^");
//		file = file.replace(" ", "\\ ");
		String[] annotate = {"git", "-c", "safe.directory=*", "annotate", file, rev + "^"};
		
//		System.out.println("git annotate " + file + " " + rev + "^");
//		String result = executor.execOneThread("git annotate " + file + " " + rev + "^", repo);
		String result = executor.execOneThread(annotate, repo);
		return result;
	}
	
	public static String gitAnnotateDeletedFile(String file, String rev, String repo) 
			throws Exception {
		
		ExecCommand executor = new ExecCommand();
		ExecCommand executor1 = new ExecCommand();
		ExecCommand executor2 = new ExecCommand();
		// create the file
		String path = file.substring(0, file.lastIndexOf("/"));
		String[] touches = {"touch",file};
		String[] annotate = {"git", "-c", "safe.directory=*", "annotate", file, rev + "^"};
		String[] removes = {"rm",file};
		String[] mkdirs = {"mkdir","-p",path};
//		System.out.println("touch " + file);
//		System.out.println("git annotate " + file + " " + rev + "^");
//		System.out.println("rm " + file);
		
//		file = file.replace(" ", "\\ ");
		
		ExecCommand mkdir = new ExecCommand();
		mkdir.execOneThread(mkdirs, repo);
//		mkdir.execOneThread("mkdir -p " + path, repo);
//		String status = executor1.execOneThread("touch " + file, repo);
		String status = executor1.execOneThread(touches, repo);
		if (status == null) {
			System.err.println("The specified path is not exits!");
			return null;
		}
		// annotate the source file
//		String result = executor.execOneThread("git annotate " + file + " " + rev + "^", repo);
		String result = executor.execOneThread(annotate, repo);
		// delete the file
//		executor2.execOneThread("rm " + file, repo);
		executor2.execOneThread(removes, repo);
		return result;
	}
	
	public static List<Commit> readFromTextGIT(String filename) {
		String hashId;
		String authorName;
		String authorEmail;
		Date date;
		String description;
		String line;
		List<Commit> commits = new ArrayList<Commit>();
		try {
			BufferedReader bw = new BufferedReader(new FileReader(new File(filename)));
			line = bw.readLine();
			while (line != null) {
				line = line.trim();
				if (line.isEmpty()) {
					line = bw.readLine();
					continue;
				}
				
				// Skip any banner/output that doesn't start with "commit "
				if (!line.startsWith("commit ")) {
					line = bw.readLine();
					continue;
				}
				
				// Format: "commit <hash>"
				if (line.length() < 7) {
					line = bw.readLine();
					continue;
				}
				hashId = line.substring(7).trim();
				// Some git versions append extra info after the hash; keep only the first token
				int firstSpace = hashId.indexOf(' ');
				if (firstSpace > 0) {
					hashId = hashId.substring(0, firstSpace);
				}
				
				// Read author line, skipping any blank/intermediate lines
				line = bw.readLine();
				while (line != null && line.trim().isEmpty()) {
					line = bw.readLine();
				}
				if (line == null || !line.startsWith("Author")) {
					// Unexpected format, skip this commit safely
					line = bw.readLine();
					continue;
				}
				
				int nameStart = line.indexOf(":") + 1;
				int nameEnd = line.indexOf("<");
				int emailEnd = line.indexOf(">");
				if (nameStart <= 0 || nameEnd <= nameStart || emailEnd <= nameEnd) {
					line = bw.readLine();
					continue;
				}
				authorName = line.substring(nameStart, nameEnd).trim();
				authorEmail = line.substring(nameEnd + 1,emailEnd);
				
				// Read date line
				line = bw.readLine();
				while (line != null && line.trim().isEmpty()) {
					line = bw.readLine();
				}
				if (line == null || !line.startsWith("Date")) {
					line = bw.readLine();
					continue;
				}
				
				String dateStr = line.substring(line.indexOf(":") + 1).trim();
				date = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH).parse(dateStr);
				
				// Read description lines until next commit
				StringBuilder descBuilder = new StringBuilder();
				line = bw.readLine();
				while (line != null && !line.startsWith("commit ")) {
					descBuilder.append(line);
					descBuilder.append("\n");
					line = bw.readLine();
				}
				description = descBuilder.toString().trim();
				
				Commit commit = new Commit(hashId,authorName,authorEmail,date,description);
				commits.add(commit);
			}
			bw.close();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return commits;
		
	}
}
