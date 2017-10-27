package classifier;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.HashMultimap;
import com.opencsv.CSVReader;

/**
 * @since 2017/10/26
 * @author Haoran Sun
 * Classifier for news
 */
public class Classifier {
  private HashMultimap<String, GeoInfo> dict;
  private LinkedList<String> provinces;
  private LinkedList<String> cities;
  
  private class GeoInfo {
    private String location;
    private String code;
    private GeoInfo parent;
    
    public GeoInfo(String location, String code, GeoInfo parent) {
      this.location = location;
      this.code = code;
      this.parent = parent;
    }
    
    public String getLocation() {
      return this.location;
    }
    
    public String getCode() {
      return this.code;
    }
    
    public GeoInfo getParent() {
      return this.parent;
    }
    
    @Override
    public String toString() {
      return "{\"Location:\"" + this.location + ", \"Code:\"" + this.code + 
          "}";
    }
  }
  
  public Classifier() {
    this.dict = HashMultimap.create();
    this.provinces = new LinkedList<>();
    this.cities = new LinkedList<>();
  }
  
  public void loadDict(String dictName) {
    try(Scanner sc = new Scanner(new InputStreamReader(new
        FileInputStream(dictName), StandardCharsets.UTF_8))) {
      JSONArray jsonDict = new JSONArray(sc.nextLine());
      for(int i = 0; i < jsonDict.length(); i++) { // Level 1
        JSONObject level1 = jsonDict.getJSONObject(i);
        GeoInfo level1Unit = new
            GeoInfo(level1.getString("name"), level1.getString("code"), null);
        this.dict.put(level1.getString("name"), level1Unit);
        
        JSONArray level2Arr = level1.getJSONArray("childs");
        for(int j = 0; j < level2Arr.length(); j++) {
          JSONObject level2 = level2Arr.getJSONObject(j);
          GeoInfo parent = null;
          if(level2.getString("name").equals("ÊÐÏ½Çø"))
            parent = level1Unit;
          else {
            parent = new GeoInfo(level2.getString("name"),
                level2.getString("code"), level1Unit);
            this.dict.put(level2.getString("name"), parent);
          }
          JSONArray level3Arr = level2.getJSONArray("childs");
          for(int k = 0; k < level3Arr.length(); k++) {
            JSONObject level3 = level3Arr.getJSONObject(k);
            this.dict.put(level3.getString("name"), new
                GeoInfo(level3.getString("name"), level3.getString("code"),
                    parent));
          }
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  public void readNews(String filename) {
    try (CSVReader reader = new CSVReader(new InputStreamReader(new
        FileInputStream(filename), StandardCharsets.UTF_8));) {
      
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void main(String[] args) {
    Classifier csf = new Classifier();
    csf.loadDict("pca-code.json");
    System.out.println("dict fin");
  }
  
}