/** Original by Komatsuda for extracting word 
  * from HTML files of ClueWeb09 CATB.
  * Modified by Zheng. (2019/05/20)
  */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * クエリ毎のクエリ尤度のスコアを算出し、ファイルに出力する。
 * クエリを構成する各ターム毎にBM25が算出されたファイルが既にあり、それらファイルは文書ID毎に昇順ソートされていることを前提に処理を行っている。
 * 各ターム毎にBM25が算出されたファイルには，BM25を算出する際に用いた統計量も保持されており，その統計量を利用することでクエリ尤度を算出している。
 * アルゴリズムの思考はマージソートと同じ
 */
public class QueryLikelihoodCalculator {
	/**
	 * クエリが保持されているファイル名
	 */
	static String PATH_TO_QUERY_LIST = "";//"queries.101-150.txt.20151225";
	/**
	 * ストップワードが保持されているファイル
	 */
	static String PATH_TO_STOP_WORDS_FILE = "";//"english.stop";
	/**
	 * 出力先ディレクトリ名
	 */
	static String DIR_FOR_OUTPUT = "";//"querylikelihood/";
	/**
	 * 出力ファイル名のプレフィックス
	 */
	static final String PREFIX_TO_OUTPUT_FILE = "ql.";
	/**
	 * ターム毎のBM25を保持するファイルが置いてあるディレクトリ名
	 */
	static String DIR_OF_BM25_FILES = "";//"trec2011_bm25/";
	/**
	 * タームごとのBM25を保持するファイル名の接頭辞
	 */
	static final String PREFIX_OF_BM25_FILE_NAME = "bm25-with-detail.";
	/**
	 * ストップワードを保持する変数
	 */
	static Set<String> stopWords = new HashSet<String>();
	/**
	 * コレクション中の文書数
	 */
	static final int NUM_OF_DOCS_IN_COLLECTION = 42898443;
	/**
	 * スムージングするためのパラメータ
	 */
	static final double PARAM_FOR_SMOOTHING = 110.0;
	
	
	public static void main(String[] args){
		
		if(args.length != 4){
			System.out.println("Usage : java QueryLikelihoodCalculator <file of query list> <file of stop words> <directory for output> <directory of bm25 files>");
			System.exit(1);
		}
		PATH_TO_QUERY_LIST = args[0];
		PATH_TO_STOP_WORDS_FILE = args[1];
		DIR_FOR_OUTPUT = args[2] + "/";
		DIR_OF_BM25_FILES = args[3] + "/";
		
		/**
		 * ストップワードを取得して保持する。
		 */
		BufferedReader br = null;
		try {
			File file = new File(PATH_TO_STOP_WORDS_FILE);
			br = new BufferedReader(new FileReader(file));
			String stopWord;
			while ((stopWord = br.readLine()) != null) {
				stopWord = stopWord.replaceAll(System.getProperty("line.separator"), "");
				stopWords.add(stopWord);
			}
		} catch (FileNotFoundException e) {
				e.printStackTrace();
	  } catch(IOException e) {
	    	e.printStackTrace();
	  } finally{ 
	    	if (br != null) {
	    		try {
					br.close();
					} catch (IOException e) {
					e.printStackTrace();
					}
	    	}
	  }

		/**
		 * ランキングを作成するクエリリストを取得して、クエリ尤度を算出し、結果をファイルに出力する
		 */
		File file = new File(PATH_TO_QUERY_LIST);
		br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			String idAndQuery;
			while ((idAndQuery = br.readLine()) != null) {
				String id = idAndQuery.split(":")[0];
				String query = idAndQuery.split(":")[1];
				writeRankingSortedByQueryLikelihood(id, query);
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
	  } catch(IOException e) {
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
	
	private static void writeRankingSortedByQueryLikelihood(String id, String query){
		System.out.println("Processing the query-" + id + "(" + query + ")");
		List<String> terms = new ArrayList<String>();
		
		String[] words = query.split("[\\s]+");
		for (String word : words) {
			if (stopWords.contains(word)) continue;
			Stemmer stemmer = new Stemmer();
	        char[] chars = word.toCharArray();
	        stemmer.b = chars;
	        stemmer.i = chars.length;
	        stemmer.stem();
	        String term = stemmer.toString().trim();
	        terms.add(term);
		}
		
		Map<String,Double> termAndCollectionModelMap = new HashMap<String,Double>(terms.size() * 4/3);
		//まずコレクションモデルの推定
		for (String term : terms) {
			//文書ID毎にソートされていることを前提としている．
			File file = new File(DIR_OF_BM25_FILES + PREFIX_OF_BM25_FILE_NAME + term + ".csv");
			if (!file.exists()) continue;
			BufferedReader br = null;
			try {
				double res = 0d;
				br = new BufferedReader(new FileReader(file));
				String line;
				while ((line = br.readLine()) != null) {
					// if(line.startsWith("document_id,BM25")) continue;
					double tf = Double.parseDouble(line.split(",")[2]);
					double documentLength = Double.parseDouble(line.split(",")[8]);
					res += tf / documentLength;
				}
				res /= NUM_OF_DOCS_IN_COLLECTION;
				termAndCollectionModelMap.put(term, res);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
		  } catch(IOException e) {
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
	
		for(Map.Entry<String, Double> e : termAndCollectionModelMap.entrySet()){
			System.out.println(e.getKey() + ":" + e.getValue());
		}
		//各文書について，語の発生確率を計算
		Map<String,BM25FileReader> termAndFrMap = new HashMap<String,BM25FileReader>(); 
		for(String term : terms){
			File file = new File(DIR_OF_BM25_FILES+PREFIX_OF_BM25_FILE_NAME + term + ".csv");
			if (!file.exists()) continue;
			BM25FileReader fileReader = new BM25FileReader(file, term);
			termAndFrMap.put(term,fileReader);
		}
		
		PrintWriter pw = null;
		File outputFile = new File(DIR_FOR_OUTPUT + PREFIX_TO_OUTPUT_FILE+id + ".csv");
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
			while (!isAllFileRead(termAndFrMap)) {
				//一番小さい文書IDを保持するFileReaderを取得
				String minDocId = "";
				for (Map.Entry<String, BM25FileReader> e : termAndFrMap.entrySet()) {
					BM25FileReader fr = e.getValue();
					if (minDocId.isEmpty()) {
						minDocId = fr.getCurDocId();
					} else {
						if (minDocId.compareTo(fr.getCurDocId()) > 0) {
							minDocId = fr.getCurDocId();
						}
					}
				}

				//一番小さい文書IDを保持しているFileReaderからデータを取得して尤度を計算
				String valsByTerm = "";
				//ディリクレ・スムージング
				double qlDirichlet = 1d;
				//線形補完（パラメータを0.1間隔で動かして行った時の値を保持する）
				double[] qlArrayForTuning = new double[5];
				Arrays.fill(qlArrayForTuning, 1d);
				for (Map.Entry<String, BM25FileReader> e : termAndFrMap.entrySet()) {
					BM25FileReader fr = e.getValue();
					double tf = 0d;
					double docLen = 0d;
					String term = fr.getTerm();
					double cm = termAndCollectionModelMap.get(term);
					if (fr.getCurDocId().equals(minDocId)) {
						tf = fr.getTermFreqInDocs();
						docLen = fr.getDocLen();
						fr.next();
					}
					//値の更新:ディリクレスムージング
					double qlTerm = (tf + PARAM_FOR_SMOOTHING * cm) / (docLen + PARAM_FOR_SMOOTHING);
					qlDirichlet *= qlTerm;
					valsByTerm += term + ":" + qlTerm + ",";
					
					//パラメータを変更させた際の尤度を計算
					double param = 1.1;
					for (int i = 0; i < 5; i++) {
						double ql = (tf + param * cm) / (docLen + param);
						qlArrayForTuning[i] = qlArrayForTuning[i] * ql;
						param = param * 10;
					}
				}
				
				String lineForOutput = minDocId + "," + qlDirichlet + "," ;
				for(double qlForTuning : qlArrayForTuning){
					lineForOutput += qlForTuning + ",";
				}
				lineForOutput += valsByTerm.substring(0, valsByTerm.length() - 1);
				pw.println(lineForOutput);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (pw != null) pw.close();
		}
	}
	
	private static boolean isAllFileRead(Map<String,BM25FileReader> fileReaders) {
		for (Map.Entry<String, BM25FileReader> e : fileReaders.entrySet()) {
			if (! e.getValue().isFileEnd()) return false;
		}
		return true;
	}
	
	static class BM25FileReader {
		private BufferedReader br = null;
		private String topDocInfo = "";
		private String EOF = "END";
		private String term = "";
		
		BM25FileReader (File file,String term) {
			try {
				br = new BufferedReader(new FileReader(file));
				String line;
				line = br.readLine();
				// if(line.startsWith("document")) line = br.readLine();//ヘッダは読み飛ばし
				if (line == null) topDocInfo = EOF;
				else topDocInfo = line;
				this.term = term;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				if (br != null) {
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (br != null) {
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		
		String getTerm(){
			return this.term;
		}
		/**
		 * ファイルオフセットが指している行の文書IDを返す
		 * @return
		 */
		String getCurDocId(){
			return topDocInfo.split(",")[0];
		}
		/**
		 * ファイルオフセットが指している行の文書について、単語の出現頻度を返す。
		 * @return
		 */
		double getTermFreqInDocs() {
			return Double.parseDouble(topDocInfo.split(",")[2]);
		}
		/**
		 * ファイルオフセットが指している行の文書について、文書長を返す。
		 * @return
		 */
		double getDocLen() {
			return Double.parseDouble(topDocInfo.split(",")[8]);
		}
		/**
		 * ファイルオフセットがファイルの終わりを指しているかどうかを返す
		 * @return
		 */
		boolean isFileEnd() {
			if (topDocInfo.isEmpty() || topDocInfo.equals("END")) {
				return true;
			}
			return false;
		}
		/**
		 * 次の行の先頭にファイルオフセットを進める
		 */
		void next() {
			try {
				String line;
				line = br.readLine();
				if (line == null) topDocInfo = EOF;
				else topDocInfo = line;
			} catch (FileNotFoundException e){
				e.printStackTrace();
				if (br != null) {
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (br != null) { 
					try {
						br.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
}
