/** Original by Komatsuda for extracting word 
  * from HTML files of ClueWeb09 CATB.
  * Modified by Zheng. (2019/06/06)
  */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class EmitBm25QlPairOfHigherRankedDocs {

	/**
	 * 処理対象となるクエリが保持されているファイル名
	 */
	private static String PATH_TO_QUERY_LIST = "";//"queries.101-150.txt.20151225";
	/**
	 * クエリ尤度及びBM25の正解文書が保持されたファイル
	 */
	private static String PATH_TO_REL_FILE = "";//"relevant.docs.bm25-ql.score";
	/**
	 * クエリ尤度モデルによって算出したスコアが記録されているファイルが置いてあるディレクトリ
	 */
	private static String DIR_OF_QL_FILES = "";//"querylikelihood/";
	/**
	 * BM25によって算出したスコアが記録されているファイルが置いているディレクトリ
	 */
	private static String DIR_OF_BM25_FILES = "";//"bm25.by.query/";
	/**
	 * 出力先のファイル名
	 */
	private static String PATH_TO_OUTPUT_FILE = "";//"pair.bm25-ql.score";
	/**
	 * クエリ尤度モデルによって算出したスコアが記録されているファイルの接頭辞
	 */
	private static final String PREFIX_OF_QL_FILE = "sort.ql.";
	/**
	 * BM25によって算出したスコアが記録されているファイルの接頭辞
	 */
	private static final String PREFIX_OF_BM25_FILE = "sort.bm25.";
	/**
	 * 文書スコアを保持するマップ
	 */
	private static Map<String, Scores> idAndScoresMap = new HashMap<String, Scores>();
	/**
	 * 処理対象となるクエリのIDを保持する変数
	 */
	private static Set<String> targetQueryIds = new HashSet<String>();
	/**
	 * 正解文書の<クエリID,文書IDの集合>を保持するテーブル
	 */
	private static Map<String, Set<String>> relQidDocIdsMapper = new HashMap<String, Set<String>>();
	/**
	 * 
	 */
	private final static int TOP_K = 5000;
	
	private static void setQueryId (String path) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(path);
			br = new BufferedReader(new FileReader(file));
			String idAndQuery;
			while ((idAndQuery = br.readLine()) != null) {
				idAndQuery = idAndQuery.replaceAll(System.getProperty("line.separator"), "");
				String id = idAndQuery.split(":")[0];
				targetQueryIds.add(id);
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
	 * 正解文書の<クエリID,文書IDの集合>を作成する
	 */
	private static void setRelDocMapper() {
		System.out.println("Scannning the labeled Docs file..");
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(PATH_TO_REL_FILE);
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll(System.getProperty("line.separator"), "");
				String queryId = line.split("[\\s]+")[0];
				String docId = line.split("[\\s]+")[1];
				
				if (relQidDocIdsMapper.containsKey(queryId)) {
					Set<String> docIds = relQidDocIdsMapper.get(queryId);
					docIds.add(docId);
				} else {
					Set<String> docIds = new HashSet<String>();
					docIds.add(docId);
					relQidDocIdsMapper.put(queryId, docIds);
				}
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
	
	private static void readBM25TopKScore(String queryId) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_BM25_FILES + PREFIX_OF_BM25_FILE + queryId + ".csv");
			br = new BufferedReader(new FileReader(file));
			String line;
			int docCounter = 0;
			while ((line = br.readLine()) != null && docCounter < TOP_K) {
				String docId = line.split(",")[0];
				docCounter++;
				//正解文書の追加はここでは行わない
				if (relQidDocIdsMapper.containsKey(queryId)) {
					Set<String> docIds = relQidDocIdsMapper.get(queryId);
					if (docIds.contains(docId)) continue;
				}
				
				String id = queryId + "_" + docId;
				Double bm25 = Double.parseDouble(line.split(",")[1]);
				Scores scores = new Scores();
				scores.id = id;
				scores.bm25 = bm25;
				idAndScoresMap.put(id, scores);
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
	
	private static void readQlTopKScore(String queryId) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_QL_FILES + PREFIX_OF_QL_FILE + queryId + ".csv");
			br = new BufferedReader(new FileReader(file));
			String line;
			int docCounter = 0;
			while ((line = br.readLine()) != null && docCounter < TOP_K) {
				String docId = line.split(",")[0];
				docCounter++;
				//正解文書の追加はここでは行わない
				if (relQidDocIdsMapper.containsKey(queryId)) {
					Set<String> docIds = relQidDocIdsMapper.get(queryId);
					if (docIds.contains(docId)) continue;
				}
				
				String id = queryId + "_" + docId;
				Scores scores = null;
				if (idAndScoresMap.containsKey(id)) {
					scores = idAndScoresMap.get(id);
				} else {
					scores = new Scores();
					scores.id = id;
				}
				Double ql = Double.parseDouble(line.split(",")[1]);
				scores.ql = ql;
				idAndScoresMap.put(id,scores);
			}
		} catch (FileNotFoundException e) {
				e.printStackTrace();
	  } catch (IOException e) {
	    	e.printStackTrace();
	  } finally {
	    	if(br != null){
	    		try {
						br.close();
					} catch (IOException e) {
							e.printStackTrace();
					}
	    	}
	  }
	}
	
	public static void main(String[] args) {
		
		if (args.length != 5 ) {
			System.out.println("Usage : java QueryLikelihoodCalculator <file of query list> <file with judgement> <directory for querylikelihood files> <directory for bm25 files> <output file name>");
			System.exit(1);
		}

		PATH_TO_QUERY_LIST = args[0];
		PATH_TO_REL_FILE = args[1];
		DIR_OF_QL_FILES = args[2] + "/";
		DIR_OF_BM25_FILES = args[3] + "/";
		PATH_TO_OUTPUT_FILE = args[4];
		
		setQueryId(PATH_TO_QUERY_LIST);
		setRelDocMapper();
		
		//BM25が上位の文書を取得
		System.out.println("Scanning the bm25 ranking file..");
		
		for (String queryId : targetQueryIds) {
			readBM25TopKScore(queryId);
		}
		
		//クエリ尤度が上位の文書を取得
		System.out.println("Scanning the query likelihood model ranking file..");
		for (String queryId : targetQueryIds) {
			readQlTopKScore(queryId);
		}
		
		Set<String> missingBm25Ids = new HashSet<String>();
		Set<String> missingQlmIds = new HashSet<String>();
		
		for (Map.Entry<String, Scores> entry : idAndScoresMap.entrySet()) {
			Scores scores = entry.getValue();
			if (scores.bm25 == -1d) {
				//クエリ尤度のスコアでは上位だが，BM25のスコアでは上位ではないID(="queryId"+"docId")を追加
				missingBm25Ids.add(scores.id);
			}
			if (scores.ql == -1d) {
				//BM25のスコアでは上位だが，クエリ尤度のスコアでは上位ではないID("queryId"+"docId")を追加
				missingQlmIds.add(scores.id);
			}
		}
		
		//クエリ尤度のスコアでは上位だが，BM25のスコアでは上位ではない文書のBM25のスコアを取得する
		System.out.println("Filling the score that cannot be got from the BM25-ranking file..");
		for (String queryId : targetQueryIds) {
			Set<String> missingBm25DocIds = new HashSet<String>();
			for (String missingBm25Id : missingBm25Ids) {
				if (missingBm25Id.startsWith(queryId)) {
					missingBm25DocIds.add(missingBm25Id.split("_")[1]);
				}
			}
			if (!missingBm25DocIds.isEmpty()) {
				setBM25Score(queryId, missingBm25DocIds);
			}
		}
		
		//BM25のスコアでは上位だが，クエリ尤度のスコアでは上位ではない文書のクエリ尤度のスコアを取得する
		System.out.println("Filling the score that cannot be got from the queryLikelihoodModel-ranking file..");
		for (String queryId : targetQueryIds) {
			Set<String> missingQlmDocIds = new HashSet<String>();
			for (String missingQlmId : missingQlmIds) {
				if (missingQlmId.startsWith(queryId)) {
					missingQlmDocIds.add(missingQlmId.split("_")[1]);
				}
			}
			if (!missingQlmDocIds.isEmpty()) {
				setQlmScore(queryId, missingQlmDocIds);	
			}
		}
		
		//正解文書の追加
		System.out.println("Scanning the scores whose docs are relevant..");
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(PATH_TO_REL_FILE);
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll(System.getProperty("line.separator"), "");
				String queryId = line.split("[\\s]+")[0];
				String docId = line.split("[\\s]+")[1];
				Double bm25 = Double.parseDouble(line.split("[\\s]+")[2]);
				Double ql = Double.parseDouble(line.split("[\\s]+")[3]);
				String id = queryId + "_" + docId;
				Scores scores = new Scores();
				scores.id = id;
				scores.bm25 = bm25;
				scores.ql = ql;
				idAndScoresMap.put(id, scores);
			}
		} catch (FileNotFoundException e) {
				e.printStackTrace();
	  } catch(IOException e) {
	    	e.printStackTrace();
	  } finally {
	    	if (br != null){
	    		try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	    	}
	  }
		
		//ファイルへ出力
		System.out.println("Emitting to the file..");
		File outputFile = new File(PATH_TO_OUTPUT_FILE);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
			
			for (Map.Entry<String, Scores> entry : idAndScoresMap.entrySet()) {
				String id = entry.getKey();
				Scores scores = entry.getValue();
				scores.fixValues();
				pw.println(id.replaceAll("_", " ") + " " + scores.bm25 + " " + scores.ql);
			}
		} catch (FileNotFoundException e) {
				e.printStackTrace();
	  } catch (IOException e) {
	    	e.printStackTrace();
	  } finally {
	    	if(pw != null){
	    		pw.close();
	    	}
	  }
		System.out.println("All processes have done.");
	}
	
	private static void setBM25Score(String id, Set<String> docIds) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_BM25_FILES + PREFIX_OF_BM25_FILE + id + ".csv");
			br = new BufferedReader(new FileReader(file));
			String docIdAndScore;
			int countFillingTheBlankScore = 0;
			while ((docIdAndScore = br.readLine()) != null) {
				if (countFillingTheBlankScore == docIds.size()) break;
				
				String docId = docIdAndScore.split(",")[0];
				double bm25 = Double.parseDouble(docIdAndScore.split(",")[1]);
				if (!docIds.contains(docId)) continue;
				
				String idAndDocId = id + "_" + docId;
				if (idAndScoresMap.containsKey(idAndDocId)) {
					Scores scores = idAndScoresMap.get(idAndDocId);
					scores.bm25 = bm25;
					countFillingTheBlankScore++;
//					System.out.println("Score Update! (" + docId +") " + idAndScoresMap.get(idAndDocId).bm25);
				}
			}
		} catch (FileNotFoundException e) {
				e.printStackTrace();
	  } catch (IOException e) {
	    	e.printStackTrace();
	  } finally {
	    	if(br != null){
	    		try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
	    	}
	  }
	}
	
	private static void setQlmScore(String queryId, Set<String> docIds) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_QL_FILES + PREFIX_OF_QL_FILE + queryId + ".csv");
			br = new BufferedReader(new FileReader(file));
			String docIdAndScore;
			int countFillingTheBlankScore = 0;
			while ((docIdAndScore = br.readLine()) != null) {
				if (countFillingTheBlankScore == docIds.size()) break;
				
				String docId = docIdAndScore.split(",")[0];
				double ql = Double.parseDouble(docIdAndScore.split(",")[1]);
				if (!docIds.contains(docId)) continue;
				
				String queryIdAndDocId = queryId + "_" + docId;
				if (idAndScoresMap.containsKey(queryIdAndDocId)) {
					Scores scores = idAndScoresMap.get(queryIdAndDocId);
					scores.ql = ql;
					//パラメータチューニング用に計算してあるクエリ尤度の取得
//					double[] qlArrayForTuning = new double[5];
//					for(int i=2;i<7;i++){
//						qlArrayForTuning[i-2] = Double.parseDouble(docIdAndScore.split(",")[i]);
//					}
//					scores.qlArrayForTuning = qlArrayForTuning;
					countFillingTheBlankScore++;
				}
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
	
	static class Scores {
		String id = "";
		double bm25 = -1d;
		double ql = -1d;
//		double[] qlArrayForTuning = new double[5];
		void fixValues() {
			if (bm25 == -1d && ql == -1d){
				System.out.println("All values are missing(ID:" + id + "). Replace the values to zero!");
				bm25 = 0d;
				ql = 0d;
			} else if (bm25 == -1d) {
				System.out.println("BM25 is missing(ID:" + id + "). Please check the output file.");
			} else if (ql == -1d) {
				System.out.println("Query-Likelihood is missing(ID:" + id + "). Please check the output file.");	
			}
		}
	}
}
