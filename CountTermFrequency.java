import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CountTermFrequency {

	private static final String SEP = File.separator;
	private static final String DIR_OF_TERM_FILE = SEP + "media" + SEP + "zzh" + SEP + "HDD1" + SEP + "clueweb09_term";
//	private static final String DIR_FOR_DEBUG = "J:" + SEP + "mnt" + SEP + "usbhdd" + SEP + "wordsWithListTagsMT" + SEP + "en0000" + SEP + "09" + SEP;
	private static final Set<String> targetTerms = new HashSet<String>();
	private static final String PATH_TO_STOP_WORDS_FILE = "english.stop";
	private static Set<String> stopWords = new HashSet<String>();
	private static final String PATH_TO_QUERY_EXCLUDE = "query_empty.txt";
	private static Set<String> excludeTerms = new HashSet<String>();
	
	public static void main(String[] args){
		if(args.length == 0){
			System.out.println("Usage : java xxx path/to/querylist");
			System.exit(1);
		}
		String pathToQueryList = args[0];
		setQueryKeywords(pathToQueryList);
		setStopWord();
		setExcludeTerms();
		File dir = new File(DIR_OF_TERM_FILE);
//		File dir = new File(DIR_FOR_DEBUG);
		iterCountDF(dir);
	}
	
	/**
	 * すでに計算済みのタームをフィールドに保持させる．
	 */
	private static void setExcludeTerms(){
		File file = new File(PATH_TO_QUERY_EXCLUDE);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String qidAndQuery;
			while((qidAndQuery = br.readLine()) != null){
				qidAndQuery = qidAndQuery.replaceAll(System.getProperty("line.separator"), "");
				String query = qidAndQuery.split(":")[1];
				String[] words = query.split("[\\s]+");
				for(String word : words){
					String term = preProcess(word);
					excludeTerms.add(term);
				}
			}
		}catch(FileNotFoundException e) {
			e.printStackTrace();
	    }catch(IOException e) {
	    	e.printStackTrace();
	    }finally{
	    	if(br != null){
	    		try {
	    			br.close();
	    		} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }
	}
	
	private static void setStopWord(){
		File file = new File(PATH_TO_STOP_WORDS_FILE);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String stopWord;
			while((stopWord = br.readLine()) != null){
				stopWord = stopWord.replaceAll(System.getProperty("line.separator"), "");
				stopWords.add(stopWord);
			}
		}catch(FileNotFoundException e) {
			e.printStackTrace();
	    } catch(IOException e) {
	    	e.printStackTrace();
	    }finally{
	    	if(br != null){
	    		try {
	    			br.close();
	    		} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }
	}
	
	/**
	 * クエリをファイルから読み込んでフィールドに格納する
	 * @param pathToFile 
	 */
	private static void setQueryKeywords(String pathToFile){
		File file = new File(pathToFile);
		BufferedReader br = null;
		try{
			br = new BufferedReader(new FileReader(file));
			String qidAndQuery;
			while((qidAndQuery = br.readLine()) != null){
				qidAndQuery = qidAndQuery.replaceAll(System.getProperty("line.separator"), "");
//				String qid = qidAndQuery.split(":")[0];
				String query = qidAndQuery.split(":")[1];
				String[] terms = query.split("[\\s]+");
				for(String term : terms){
					targetTerms.add(term);
				}
			}
		}catch(FileNotFoundException e) {
			e.printStackTrace();
	    } catch(IOException e) {
	    	e.printStackTrace();
	    }finally{
	    	if(br != null){
	    		try {
	    			br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	  	}
		}
	
	private static void iterCountDF(File file){
		if(file.isDirectory()){
			System.out.println(file.getAbsolutePath());
			File[] files = file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return !(name.equals(".DS_Store") || name.equals(".txt") || name.contains("._"));
				}
			});
			for(File childFile : files){
				iterCountDF(childFile);
			}
		} else {
				System.out.println(file.getAbsolutePath());
				countDF(file);
		}
		
	}
	
	private static void countDF(File file){
		String docId = file.getName().substring("clueweb09-en00".length(), file.getName().length() - ".txt".length());
		Map<String,Integer> dfMap = new HashMap<String,Integer>();
		BufferedReader br = null;
		try {
			for(String targetWord : targetTerms){
				String targetTerm = preProcess(targetWord);
				dfMap.put(targetTerm, 0);
			}
			
			br = new BufferedReader(new FileReader(file));
			String word;
			while((word = br.readLine()) != null){
				String term = preProcess(word);
				for(String targetWord : targetTerms){
					String targetTerm = preProcess(targetWord);
					if(term.equals(targetTerm) && !excludeTerms.contains(targetTerm)){
						dfMap.put(targetTerm, dfMap.get(targetTerm)+1);
					}
				}   
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(br != null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//ファイルに出力
		for(String targetWord : targetTerms){
			String targetTerm = preProcess(targetWord);
			if(targetTerm == "") continue;
			if(excludeTerms.contains(targetTerm)) continue;
			if(dfMap.get(targetTerm).equals(0)) continue;
			
			File outputFile = new File(SEP + "media" + SEP + "zzh" + SEP + "HDD1" + SEP + "clueweb09_term_frequency" + SEP + "tf." + targetTerm + ".csv");
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile,true)));
				pw.println(docId + "," + dfMap.get(targetTerm));
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(pw != null) pw.close();
			}
		}	
	}
	
	private static String preProcess(String word){
		if(stopWords.contains(word)) return "";
		//stem the word and add list
		Stemmer stemmer = new Stemmer();
		char[] chars = word.toCharArray();
		stemmer.b = chars;
		stemmer.i = chars.length;
		stemmer.stem();
		String term = stemmer.toString().trim();
		return term;
	}
}
