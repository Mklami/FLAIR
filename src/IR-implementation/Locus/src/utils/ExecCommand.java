package utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecCommand {
	public String exec(String command) throws IOException {
		return exec(command, null, null);
	}

	public String exec(String command, String workpath) throws IOException {
		return exec(command, null, workpath);
	}
	
	public String execOneThread(String command, String workingpath) {
		final StringBuffer result = new StringBuffer("");
		try {
			File dir = new File(workingpath);
			Process process = Runtime.getRuntime().exec(command, null, dir);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;
			while ((line = stdInput.readLine()) != null) {
				result.append(line + "\n");
			}
			
			while ((line = stdError.readLine()) != null) {
				System.out.println(line);
			}
			
			stdInput.close();
			stdError.close();
		} catch (Exception e) {
			System.err.println("Error:" + command);
			return null;
		}
		return result.toString();
	}
	
	public String execOneThread(String[] commands, String workingpath) {
		final StringBuffer result = new StringBuffer("");
		final StringBuffer errorOutput = new StringBuffer("");
		try {
			File dir = new File(workingpath);
			if (!dir.exists()) {
				System.err.println("ERROR: Working directory does not exist: " + workingpath);
				return null;
			}
			if (!dir.isDirectory()) {
				System.err.println("ERROR: Working path is not a directory: " + workingpath);
				return null;
			}
			
			System.out.println("Executing: " + java.util.Arrays.toString(commands) + " in " + workingpath);
			Process process = Runtime.getRuntime().exec(commands, null, dir);
			
			// Read stderr first to avoid blocking
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			Thread errorThread = new Thread() {
				public void run() {
					try {
						String line = null;
						while ((line = stdError.readLine()) != null) {
							errorOutput.append(line + "\n");
							System.err.println("Git stderr: " + line);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							stdError.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			};
			errorThread.start();
			
			// Read stdout
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int lineCount = 0;
			while ((line = stdInput.readLine()) != null) {
				result.append(line + "\n");
				lineCount++;
			}
			stdInput.close();
			
			// Wait for process and error thread to complete
			int exitCode = process.waitFor();
			errorThread.join(5000); // Wait up to 5 seconds for error thread
			
			if (exitCode != 0) {
				System.err.println("Git command failed with exit code: " + exitCode);
				if (errorOutput.length() > 0) {
					System.err.println("Error output: " + errorOutput.toString());
				}
			} else {
				System.out.println("Git command succeeded, retrieved " + lineCount + " lines");
			}
			
			process.destroy();
		} catch (Exception e) {
			System.err.println("Error executing command: " + java.util.Arrays.toString(commands));
			System.err.println("Working path: " + workingpath);
			e.printStackTrace();
			return null;
		}
		return result.toString();
	}
	
	public String exec(String command, String[] envp, String workpath)
			throws IOException {

		final StringBuffer result = new StringBuffer("");
		final String commandStr = command;
		try {
			File dir = null;
			if (null != workpath)
				dir = new File(workpath);
			Process process = Runtime.getRuntime().exec(command, envp, dir);
			final InputStream is1 = process.getInputStream();
			final InputStream is2 = process.getErrorStream();

			new Thread() {
				public void run() {
					BufferedReader br1 = new BufferedReader(
							new InputStreamReader(is1));
					try {
						String line1 = null;
						while ((line1 = br1.readLine()) != null) {
							result.append(line1 + "\n");
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							br1.close();
							is1.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
			
			new Thread() {
				public void run() {
					BufferedReader br2 = new BufferedReader(
							new InputStreamReader(is2));
					try {
						String line1 = null;
						while ((line1 = br2.readLine()) != null) {
							if (line1 != null) {
								System.err.println(commandStr);
								System.err.println(line1);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							br2.close();
							is2.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
			
			process.waitFor();
			process.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {

		}
		return result.toString();
	}


	public void exec(String[] commands) throws IOException {
		exec(commands, null, null);
	}

	public String exec(String[] commands, String workpath) throws IOException {

		return exec(commands, null, workpath);
	}

	public String exec(String[] commands, String[] envp, String workpath)
			throws IOException {
		
		final StringBuffer result = new StringBuffer("");
		try {
			File dir = null;
			if (null != workpath)
				dir = new File(workpath);
			Process process = Runtime.getRuntime().exec(commands, envp, dir);
			final InputStream is1 = process.getInputStream();
			final InputStream is2 = process.getErrorStream();

			new Thread() {
				public void run() {
					BufferedReader br1 = new BufferedReader(
							new InputStreamReader(is1));
					try {
						String line = null;
						while ((line = br1.readLine()) != null) {
							if (line != null) {
								result.append(line + "\n");
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is1.close();
							br1.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
			
			new Thread() {
				public void run() {
					BufferedReader br2 = new BufferedReader(
							new InputStreamReader(is2));
					try {
						String line = null;
						while ((line = br2.readLine()) != null) {
							if (line != null) {
								System.out.println(line);
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							is2.close();
							br2.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
			process.waitFor();
			process.destroy();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return result.toString();
	}
}