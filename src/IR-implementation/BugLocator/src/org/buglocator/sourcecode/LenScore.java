package org.buglocator.sourcecode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

import org.buglocator.property.Property;

public class LenScore {
	private String workDir = Property.getInstance().WorkDir + Property.getInstance().Separator;
	private int fileCount = Property.getInstance().FileCount;

	/**
	 * 
	 * @throws IOException
	 */
	public void computeLenScore() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(workDir + "TermInfo.txt"));
		String line = null;
		int max = Integer.MIN_VALUE;
		int[] lens = new int[fileCount];
		int i = 0;
		Hashtable<String, Integer> lensTable = new Hashtable<String, Integer>();
		int count = 0;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(";");
			String name = values[0].substring(0, values[0].indexOf("\t"));
			Integer len = Integer.parseInt(values[0].substring(values[0].indexOf("\t") + 1));
			lensTable.put(name, len);
			lens[i++] = len;
			if (len != 0)
				count++;
			if (len > max) {
				max = len;
			}
		}
		reader.close();

		int sum = 0;
		for (int j = 0; j < lens.length; j++) {
			sum += lens[j];
		}
		double average = sum / (double) count; // Average length of files (total word count)

		// Statistics generated in 10 units. (Digital distribution of lens by file)
		// This is where I use it ...
		Hashtable<Integer, Integer> statTable = new Hashtable<Integer, Integer>();
		for (int j = 0; j < lens.length; j++) {
			if (lens[j] != 0) {
				int index = lens[j] / 10;
				if (statTable.containsKey(index)) {
					int l = statTable.get(index);
					l++;
					statTable.remove(index);
					statTable.put(index, l);
				} else {
					statTable.put(index, 1);
				}
			}
		}

		// Find the standard deviation of the lens
		double squareDevi = 0;
		for (int j = 0; j < lens.length; j++) {
			if (lens[j] != 0) {
				squareDevi += (lens[j] - average) * (lens[j] - average);
			}
		}
		double standardDevi = Math.sqrt(squareDevi / count);

		// Determine min high of lens
		double low = average - 3 * standardDevi;
		double high = average + 3 * standardDevi;

		int min = 0;
		if (low > 0)
			min = (int) low;

		// write Length Score
		FileWriter writer = new FileWriter(workDir + "LengthScore.txt");
		for (String key : lensTable.keySet()) {
			int len = lensTable.get(key);
			double score = 0.0;
			double nor = this.getNormValue(len, high, min);
			if (len != 0) {
				if (len > low && len < high) {

					score = this.getLenScore(nor);
				} else if (len < low) {
					score = 0.5;
				} else {
					score = 1.0;
				}
			} else {
				score = 0.0;
			}
			if (nor > 6)
				nor = 6;
			if (score < 0.5)
				score = 0.5f;
			writer.write(key + "\t" + score + Property.getInstance().LineSeparator);
			writer.flush();
		}
		writer.close();
	}

	private float getNormValue(int x, double max, double min) {
		return 6 * (float) (x - min) / (float) (max - min);
	}

	public double getLenScore(double len) {
		return (Math.exp(len) / (1 + Math.exp(len)));
	}
}
