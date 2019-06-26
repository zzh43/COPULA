/** Original by Komatsuda for extracting word 
  * from HTML files of ClueWeb09 CATB.
  * Modified by Zheng. (2019/05/06)
  */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * クエリを構成する各語のBM25を計算するモジュール
 */
public class BM25Calculator {
	/**
	 * セパレータ
	 */
	private static final String SEP = File.separator;
	/**
	 * ストップワードファイル
	 */
	private static final String PATH_TO_STOP_WORDS_FILE = "english.stop";
	/**
	 * クエリを保持する変数
	 */
	private static List<String> queryKeyWords = new ArrayList<String>();
	/**
	 * ストップワードを保持する変数
	 */
	private static Set<String> stopWords = new HashSet<String>();
	/**
	 * 語の文書内出現頻度ファイルが置いてあるディレクトリ
	 */
	private static final String DIR_OF_TF_FILES = "/media/zzh/HDD1/clueweb09_term_frequency/";
	/**
	 * タームファイルが置いてあるディレクトリ
	 */
	private static final String DIR_OF_TERM_FILES = "/media/zzh/HDD1/clueweb09_term/";
	/**
	 * ファイルの出力先ディレクトリ
	 */
	private static final String DIR_OF_OUTPUT_FILES = "/media/zzh/HDD1/clueweb09_term_bm25/";
	/**
	 * 出力ファイルの接頭辞
	 */
	private static final String PREFIX_OF_BM25_FILE = "bm25-with-detail.";
	/**
	 * BM25を算出するための統計量 : Clueweb09の総文書数
	 */
	private static final double DOCUMENT_NUMBER_IN_COLLECTION = 42898443d;
	/**
	 * BM25を算出するための統計量 : Clueweb09の平均文書長
	 */
	private static final double AVE_OF_DOCUMENT_LENGTH = 716.78;
	/**
	 * BM25を算出するための統計量 : BM25のパラメータk及びb
	 */
	private static final double K = 1.2, B = 0.75;

	/**
	 * ストップワードをファイルから読み込んでフィールドに格納する
	 */
	private static void setStopWords() {
		File file = new File(PATH_TO_STOP_WORDS_FILE);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String stopWord;
			while ((stopWord = br.readLine()) != null) {
				stopWord = stopWord.replaceAll(System.getProperty("line.separator"), "");
				stopWords.add(stopWord);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } finally {
	    	if (br != null) {
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
	 * @param pathToFile BM25を算出するクエリが書かれたファイル
	 */
	private static void setQueryKeywords(String pathToFile) {
		File file = new File(pathToFile);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String keyword;
			while ((keyword = br.readLine()) != null) {
				keyword = keyword.replaceAll(System.getProperty("line.separator"), "");
				queryKeyWords.add(keyword);
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
	    } catch(IOException e) {
	    	e.printStackTrace();
	    } finally{
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
	 * ステミングを行う
	 * @param word ステミングする語
	 * @return ステミングされた語
	 */
	private static String stemWord(String word){
		Stemmer stemmer = new Stemmer();
        char[] chars = word.toCharArray();
        stemmer.b = chars;
        stemmer.i = chars.length;
        stemmer.stem();
        String term = stemmer.toString().trim();
        return term;
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Usage : java BM25Calculator path/to/querylist");
			System.exit(1);
		}
		String pathToQueryList = args[0];
		
		setStopWords();
		setQueryKeywords(pathToQueryList);
		
		for (String idAndKeywords : queryKeyWords) {
			//the format of the variable isAndKeywords is "id:keyword1 keyword2 keyword3..."
			String id = idAndKeywords.split(":")[0];
			String keywords = idAndKeywords.split(":")[1];
			String[] keywordArray = keywords.split("[\\s]+");
			for (String keyword : keywordArray) {
				calcBM25(id, keyword);
			}
			System.out.println("calculating BM25 has just done. QUERY ID:" + id);
		}
	}
	
	private static void calcBM25(String id,String word){
		if (stopWords.contains(word)) return;
		String term = stemWord(word);
		
		File bm25File = new File(DIR_OF_OUTPUT_FILES + PREFIX_OF_BM25_FILE + term + ".csv");
		if (bm25File.exists()) {
			//既にファイルがある場合は計算しない
			System.out.println("the BM25-file already existed. (TERM:" + term + ")");
			return;
		} else {
			System.out.println("Calculating the BM25 : KEY -> " + term + " ...");
			
			//大域的な統計量の計算
			GlobalStatistics gs = new GlobalStatistics(term);
			final int termFrequencyInCollection = gs.getTermFrequencyInCollection();
			final int documentFrequencyOfTerm = gs.getDocumentFrequencyOfTerm();
			
			File tfFile = new File(DIR_OF_TF_FILES + "tf." + term + ".csv");
			if (!tfFile.exists()) {
				//tfファイルがない場合は計算しない
				System.out.println("the TF-file not existed. (TERM:" + term + ")");
				return;
			}
			BufferedReader br = null;
			PrintWriter pw = null;
			try{
				pw = new PrintWriter(new BufferedWriter(new FileWriter(bm25File)));
				// pw.println("document_id,BM25,tf,idf,term_frequency_in_document,term_frequency_in_collection,document_frequency_where_term_appears,document_length");
				
				br = new BufferedReader(new FileReader(tfFile));
				String docIdAndTf;
				while((docIdAndTf = br.readLine()) != null){
					docIdAndTf = docIdAndTf.replaceAll(System.getProperty("line.separator"), "");
					
					//局所的な統計量及びBM25の計算
					String docId = docIdAndTf.split(",")[0];
					final int termFrequencyInDocument = Integer.parseInt(docIdAndTf.split(",")[1]);
					final int documentLength = getDocumentLength(docId);
					double tf = 0d;
					double idf = 0d;
					double bm25 = 0d;
					if(termFrequencyInCollection > 0){
						tf = Double.valueOf(termFrequencyInDocument);
						idf = Math.log10(((DOCUMENT_NUMBER_IN_COLLECTION - (double)documentFrequencyOfTerm + 0.5) / ((double)documentFrequencyOfTerm + 0.5)) + 1.0d);
						bm25 = idf * tf * (K + 1.0) / (tf + K * (1.0 - B + B * (Double.valueOf(documentLength) / AVE_OF_DOCUMENT_LENGTH)));
					}
					//ファイルへ出力
					String value = 
							bm25 + "," +
							tf + "," +
							idf + "," +
							termFrequencyInDocument + "," +
							termFrequencyInCollection + "," +
							DOCUMENT_NUMBER_IN_COLLECTION + "," +
							documentFrequencyOfTerm + "," +
							documentLength;
					pw.println(docId + "," + value);
				}
			} catch(FileNotFoundException e) {
				e.printStackTrace();
		    } catch(IOException e) {
		    	e.printStackTrace();
		    } finally {
		    	if (pw != null) pw.close();
		    	if (br != null) {
		    		try {
		    			br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }
		}
	}
	
	/**
	 * 大域的な（コレクション全体をなめないといけない）統計量の算出クラス
	 */
	private static class GlobalStatistics {
		//BM25を算出するための統計量を保持する変数
		/**
		 * 語のコレクション内出現頻度
		 * (コレクション中に語が現れた回数)
		 */
		private int termFrequencyInCollection = 0;
		/**
		 * 語が出現する文書数
		 */
		private int documentFrequencyOfTerm = 0;
		
		GlobalStatistics (String term) {
			setStatistics(term);
		}
		
		int getTermFrequencyInCollection() {
			return this.termFrequencyInCollection;
		}
		int getDocumentFrequencyOfTerm() {
			return this.documentFrequencyOfTerm;
		}
		
		/**
		 * 語のコレクション内出現頻度を算出する。
		 * @param term　コレクション内出現頻度を算出する語
		 * @return コレクション内出現頻度
		 */
		private void setStatistics(String term){
			int documentFrequencyOfTerm = 0;
			int termFrequencyInCollection = 0;
			File file = new File(DIR_OF_TF_FILES + "tf." + term + ".csv");
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
				String docIdAndTf;
				while ((docIdAndTf = br.readLine()) != null) {
					docIdAndTf = docIdAndTf.replaceAll(System.getProperty("line.separator"), "");
					int tf = Integer.parseInt(docIdAndTf.split(",")[1]);
					// Fixed by Zheng (2019/05/06)
					documentFrequencyOfTerm ++;
					termFrequencyInCollection += tf;
				}
			} catch(FileNotFoundException e) {
				e.printStackTrace();
		    } catch(IOException e) {
		    	e.printStackTrace();
		    } finally{
		    	if(br != null){
		    		try {
		    			br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }
			this.documentFrequencyOfTerm = documentFrequencyOfTerm;
			this.termFrequencyInCollection = termFrequencyInCollection;
		}
	}
	
	/**
	 * 文書長を算出する
	 * @param docId 文書長を算出したい文書のID
	 * @return 文書長　
	 */
	private static int getDocumentLength(String docId){
		String dirId = "en00" + docId.split("-")[0] + "/";
		String subDirId = docId.split("-")[1] + ".warc" + "/";
		int documentLength = 0;
		File file = null;
		LineNumberReader reader = null;
		try {
			file = new File(DIR_OF_TERM_FILES + dirId + subDirId + "clueweb09-en00" + docId + ".txt");
			if (!file.exists()) System.out.println("File does not exist!");
			reader = new LineNumberReader(new FileReader(file));
		    while ((reader.readLine()) != null);
		    documentLength = reader.getLineNumber();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
	    } catch(IOException e) {
	    	e.printStackTrace();
	    } finally { 
	        if (reader != null) {
	        	try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
	    }	
		return documentLength;
	}
}
