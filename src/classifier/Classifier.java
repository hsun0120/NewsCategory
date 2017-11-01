package classifier;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.HashMultimap;
import com.hankcs.hanlp.HanLP;
import com.opencsv.CSVReader;

import datastructure.IndexInterval;
import datastructure.IntervalTree;

/**
 * @since 2017/10/26
 * @author Haoran Sun
 * Classifier for news
 */
public class Classifier {
	static final int GEO_MAX_LENGTH = 15;
	static final String DELI = "(?<=[。|,|;])";
	static final String[] IGNORE_LOC = {"市辖区", "城区", "矿区", "郊区", "省直辖县级行政区划"};
	static final String[] REGIONS = {"省", "自治区", "市", "区", "县", "壮族自治区",
			"回族自治区", "维吾尔自治区", "自治县", "自治州"};
	static final int CITY = 2;
	static final int DIST_COUNTY = 3;
	static final int LENGTH_CITY_CODE = 4;

	private HashMultimap<String, GeoInfo> dict;
	private HashMap<String, String> newspapers;
	private TreeSet<String> provinces;
	private TreeSet<String> cities;

	private class GeoInfo {
		private String code;
		private GeoInfo parent;

		public GeoInfo(String code, GeoInfo parent) {
			this.code = code;
			this.parent = parent;
		}

		public String getCode() {
			return this.code;
		}

		public GeoInfo getParent() {
			return this.parent;
		}

		public boolean isBelongTo(String code) {
			if(code == null || code.length() == 0) return false;

			GeoInfo parent = this.parent;
			while(parent != null) {
				if(parent.getCode().equals(code))
					return true;
				parent = parent.parent;
			}
			return false;
		}

		public int getLevel() {
			int level = 1;
			GeoInfo parent = this.parent;
			while(parent != null) {
				parent = parent.parent;
				level++;
			}
			return level;
		}

		@Override
		public String toString() {
			return "\"Code:\"" + this.code;
		}
	}

	public Classifier() {
		this.dict = HashMultimap.create();
		this.newspapers = new HashMap<>();
	}

	public void loadNewspaperList(String list) {
		try(Scanner sc = new Scanner(new InputStreamReader(new
				FileInputStream(list), StandardCharsets.UTF_8))) {
			while(sc.hasNext()) {
				String name = sc.next();
				String code = sc.next();
				this.newspapers.put(name, code);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void loadDict(String dictName) {
		try(Scanner sc = new Scanner(new InputStreamReader(new
				FileInputStream(dictName), StandardCharsets.UTF_8))) {
			JSONArray jsonDict = new JSONArray(sc.nextLine());
			for(int i = 0; i < jsonDict.length(); i++) { // Level 1
				JSONObject level1 = jsonDict.getJSONObject(i);
				GeoInfo level1Unit = new
						GeoInfo(level1.getString("code"), null);
				this.dict.put(level1.getString("name"), level1Unit);

				JSONArray level2Arr = level1.getJSONArray("childs");
				for(int j = 0; j < level2Arr.length(); j++) {
					JSONObject level2 = level2Arr.getJSONObject(j);
					GeoInfo parent = null;
					if(this.exclude(level2.getString("name")))
						parent = level1Unit;
					else {
						parent = new GeoInfo(level2.getString("code"), level1Unit);
						this.dict.put(level2.getString("name"), parent);
					}
					JSONArray level3Arr = level2.getJSONArray("childs");
					for(int k = 0; k < level3Arr.length(); k++) {
						JSONObject level3 = level3Arr.getJSONObject(k);
						if(this.exclude(level3.getString("name"))) continue;
						this.dict.put(level3.getString("name"), new
								GeoInfo(level3.getString("code"), parent));
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void readNews(String filename, String output) {
		try (CSVReader reader = new CSVReader(new InputStreamReader(new
				FileInputStream(filename), StandardCharsets.UTF_8));) {
			String[] line;
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new
					FileOutputStream(output), StandardCharsets.UTF_8));
			while((line = reader.readNext()) != null) {
				String id = line[0];
				String newspaper = line[1];
				String text = line[line.length - 1].replaceAll("<[^>]+>", "").replace
						(" ", "");
				String newsOrigin = this.newspapers.get(newspaper);
				/* Newspaper is from Hongkong or Macau */
				if(newsOrigin.equals("81") || newsOrigin.equals("82"))
					text = HanLP.convertToSimplifiedChinese(text);
				this.provinces = new TreeSet<>();
				this.cities = new TreeSet<>();
				
				writer.write(id + " ");
				this.ngramMatch(text.split(DELI), newsOrigin, writer);
				writer.write("\n");
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean exclude(String word) {
		for(String loc: IGNORE_LOC)
			if(loc.equals(word)) return true;
		return false;
	}

	private String lookup(String term, String newsOrigin) {
		if(this.dict.containsKey(term))
			return this.directLookup(term, newsOrigin);
		else
			return this.prefixLookup(term, newsOrigin);
	}

	private String directLookup(String term, String newsOrigin) {
		Set<GeoInfo> result = this.dict.get(term);
		Iterator<GeoInfo> it = result.iterator();
		if(result.size() == 1) {
			GeoInfo loc = it.next();
			if(loc.getLevel() == 1) this.provinces.add(loc.getCode());
			else this.cities.add(loc.getCode());
			return loc.code;
		} else {
			while(it.hasNext()) {
				GeoInfo loc = it.next();
				if(loc.isBelongTo(newsOrigin)) return loc.code;
				Iterator<String> iter;
				if(loc.getLevel() == CITY)
					iter = this.provinces.iterator();
				else
					iter = this.cities.iterator();
				while(iter.hasNext()) {
					String code = iter.next();
					if(loc.isBelongTo(code)) return loc.code;
				}
			}
		}
		return null;
	}

	private String prefixLookup(String term, String newsOrigin) {
		String ret = null;
		int count = 0;
		for(int i = 0; i < REGIONS.length; i++) {
			String code = this.directLookup(term + REGIONS[i], newsOrigin);
			if(code != null && ret == null && count == 0) {
				ret = code;
				count++;
			}
			if(i > CITY && code != null && code.length() > 2 &&
					!this.provinces.contains(code.substring(0, 2)) &&
					!this.cities.contains(code.substring(0, 4)))
				ret = null;
			if(i <= CITY && code != null && code.length() > LENGTH_CITY_CODE)
				ret = null;
			if(code != null && code.startsWith(newsOrigin))
				ret = code;
		}
		return ret;
	}

	private void ngramMatch(String[] content, String newsOrigin, BufferedWriter
			writer) {
		int n = GEO_MAX_LENGTH;
		int wordLength = n;

		/* Iterate through all sentences */
		for (int i = 0; i < content.length; i++) {
			IntervalTree<IndexInterval> ist = new IntervalTree<>();
			int endIndex = 0;
			wordLength = n;

			/* First round word matching using the word length n */
			for (int j = 0; j < content[i].length();) {
				endIndex = j + wordLength - 1;
				if (endIndex >= content[i].length())
					break;

				/* Word matching */
				String match = null;
				if ((match = this.lookup(content[i].substring(j, endIndex + 1),
						newsOrigin)) != null) {
					/* Mark interval */
					ist.insert(new IndexInterval(j, endIndex));
					try {
						writer.write(content[i].substring(j, endIndex + 1) + 
								",code:" + match + " ");
					} catch (IOException e) {
						e.printStackTrace();
					}
					j = endIndex + 1; // Get next index
				} else {
					j++;
				}
			}
			wordLength--; // Ready to match word with shorter length

			int nextPos;
			/* Try matching until the length 2 */
			for (; wordLength > 1; wordLength--) {
				/* Start from the first available position */
				for (int j = ist.nextAvailable(new IndexInterval(0, wordLength - 1));
						j < content[i].length();) {
					endIndex = j + wordLength - 1;
					if (endIndex >= content[i].length())
						break;

					nextPos = ist.nextAvailable(new IndexInterval(j, endIndex)); // Check
					// overlap
					if (j == nextPos) { // No overlap found
						/* Word matching */
						String match = null;
						if ((match = this.lookup(content[i].substring(j,
								endIndex + 1), newsOrigin)) != null) {
							/* Mark interval */
							ist.insert(new IndexInterval(j, endIndex));
							try {
								writer.write(content[i].substring(j, endIndex +
										1) + ",code:" + match + " ");
							} catch (IOException e) {
								e.printStackTrace();
							}
							j = endIndex + 1; // Get next index
						} else {
							j++;
						}
					} else
						j = nextPos; // Try next available position
				}
			}
		}
	}

	public static void main(String[] args) {
		Classifier csf = new Classifier();
		csf.loadNewspaperList("newspaperList.txt");
		csf.loadDict("pca-code.json");
		csf.readNews("2017-01.csv", "output.txt");
	}

}