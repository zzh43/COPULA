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

public class EmitBm25QlPairOfRelevantDocs {
	/**
	 * クエリリストファイル
	 */
	private static String PATH_TO_QUERY_LIST = "";//"queries.101-150.txt.20151225";
	/**
	 * 正解ラベルが付与されている文書のリストファイル
	 */
	private static String PATH_TO_LABELED_DOCS_FILE = "";//"qrels.adhoc.relevant.only";// TREC'2011の正解データ
	/**
	 * クエリ尤度モデルによって算出したスコアが記録されているファイルが置いてあるディレクトリ
	 */
	private static String DIR_OF_QL_FILES = "";//"querylikelihood/";
	/**
	 * BM25によって算出したスコアが記録されているファイルが置いているディレクトリ
	 */
	private static  String DIR_OF_BM25_FILES = "";//"bm25.by.query/";
	/**
	 * クエリ尤度モデルによって算出したスコアが記録されているファイルの接頭辞
	 */
	private static final String PREFIX_OF_QL_FILE = "sort.ql.";
	/**
	 * BM25によって算出したスコアが記録されているファイルの接頭辞
	 */
	private static final String PREFIX_OF_BM25_FILE = "sort.bm25.";
	/**
	 * クエリ尤度及びBM25の正解文書が保持されたファイル
	 */
	private static String PATH_TO_OUTPUT_FILE = "";//"relevant.docs.bm25-ql.score";
	/**
	 * クエリIDと文書IDから構成されるIDをキー,キーに対応するスコアをバリューとする変数
	 */
	private static Map<String,Scores> idAndScoresMap = new HashMap<String,Scores>();
	/**
	 * 正解文書について,クエリIDと文書IDから構成されるIDを保持する変数
	 */
	private static Set<String> relevantDataIds = new HashSet<String>();
	/**
	 * 処理対象となるクエリIDのリストを保持する変数
	 */
	private static Set<String> targetQueryIds = new HashSet<String>();
	
	private static void setQueryId(String path) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(path);
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				String queryId = line.split(":")[0];
				targetQueryIds.add(queryId);
			}
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
	
	public static void main(String[] args) {
		if (args.length != 5) {
			System.out.println("Usage : java QueryLikelihoodCalculator <file of query list> <file with judgement> <directory for querylikelihood files> <directory for bm25 files> <output file name>");
			System.exit(1);
		}
		PATH_TO_QUERY_LIST = args[0];
		PATH_TO_LABELED_DOCS_FILE = args[1];
		DIR_OF_QL_FILES = args[2] + "/";
		DIR_OF_BM25_FILES = args[3] + "/";
		PATH_TO_OUTPUT_FILE = args[4];
		
		setQueryId(PATH_TO_QUERY_LIST);
		
		System.out.println("Scannning the labeled Docs file..");
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(PATH_TO_LABELED_DOCS_FILE);
			br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.replaceAll(System.getProperty("line.separator"), "");
				String queryId = line.split("[\\s]+")[0];
				String docId = line.split("[\\s]+")[2];
				String relevancy = line.split("[\\s]+")[3];
				
				//対象のクエリか確認
				if (!targetQueryIds.contains(queryId)) continue;
				//対象のデータセット内の文書か確認
				////対象となる文書はカテゴリーBに含まれる文書
				////カテゴリーAにあってカテゴリーBにない文書やwiki文書は対象外であるため除外
				String dirName = docId.split("-")[1];
				if (dirName.compareTo("en0000") < 0 || dirName.compareTo("en0011") > 0) continue;
				if (dirName.startsWith("enwp")) continue;
				
				//正解文書の場合,変数に加える
				if (relevancy.equals("1") || relevancy.equals("2")) {
					docId = docId.replaceAll("clueweb09-en00", "");
					relevantDataIds.add(queryId + "_" + docId);
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
		System.out.println("LENGTH OF RELEVANT_DATA_IDS : " + relevantDataIds.size());
		
		for (String relevantDataId : relevantDataIds) {
			Scores scores = new Scores();
			scores.id = relevantDataId;
			idAndScoresMap.put(relevantDataId, scores);
		}
		
		System.out.println("LENGTH OF ID_SCORES_MAP : " + idAndScoresMap.size());
		
		Set<String> missingBm25IdList = new HashSet<String>();
		Set<String> missingQlIdList = new HashSet<String>();
		Set<String> missingIdList = new HashSet<String>();
		
		for (Map.Entry<String, Scores> entry : idAndScoresMap.entrySet()) {
			Scores scores = entry.getValue();
			missingIdList.add(scores.id);
			
			if (scores.bm25 == -1d) {
				missingBm25IdList.add(scores.id);
			}
			if (scores.ql == -1d) {
				missingQlIdList.add(scores.id);
			}
		}
		
		System.out.println("Filling the score that cannot be got from the BM25-ranking file..");
		for (String queryId : targetQueryIds) {
			Set<String> missingBm25DocIds = new HashSet<String>();
			
			for (String missingBm25Id : missingIdList) {
				if (missingBm25Id.startsWith(queryId)) {
					missingBm25DocIds.add(missingBm25Id.split("_")[1]);
				}
			}
			
			if (!missingBm25DocIds.isEmpty()) {
				setBM25Score(queryId, missingBm25DocIds);
			}
		}
		
		System.out.println("Filling the score that cannot be got from the QL-ranking file..");
		for (String queryId : targetQueryIds) {
			Set<String> missingQlDocIds = new HashSet<String>();
			for (String missingBm25Id : missingIdList){
				if (missingBm25Id.startsWith(queryId)) {
					missingQlDocIds.add(missingBm25Id.split("_")[1]);
				}
			}
			
			if (!missingQlDocIds.isEmpty()) {
				setQlScore(queryId, missingQlDocIds);	
			}
		}
		
		System.out.println("Emitting to the file..");
		File outputFile = new File(PATH_TO_OUTPUT_FILE);
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(outputFile,true)));
			
			for (Map.Entry<String, Scores> entry : idAndScoresMap.entrySet()) {
				String id = entry.getKey();
				Scores scores = entry.getValue();
				scores.fixValue();
				String outputQlLinears = "";
				for (double ql : scores.qlArrayForTuning) {
					outputQlLinears += " " + ql;
				}
				pw.println(id.replaceAll("_", " ") + " " + scores.bm25 + " " + scores.ql + outputQlLinears);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } finally {
	    	if (pw != null) {
	    		pw.close();
	    	}
	    }
		System.out.println("All processes have done.");	
	}

	private static void setBM25Score(String id , Set<String> docIds) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_BM25_FILES + PREFIX_OF_BM25_FILE + id + ".csv");
			br = new BufferedReader(new FileReader(file));
			String docIdAndScore;
			int countFillingTheBlankScore = 0;
			while ((docIdAndScore = br.readLine()) != null) {
				if (countFillingTheBlankScore == docIds.size()) break;
				docIdAndScore = docIdAndScore.replaceAll(System.getProperty("line.separator"), "");
				
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
	    	if (br != null) {
	    		try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
	    	}
	    }
	}
	
	private static void setQlScore(String queryId , Set<String> docIds) {
		File file = null;
		BufferedReader br = null;
		try {
			file = new File(DIR_OF_QL_FILES + PREFIX_OF_QL_FILE + queryId + ".csv");
			
			br = new BufferedReader(new FileReader(file));
			String docIdAndScore;
			int countFillingTheBlankScore = 0;
			while ((docIdAndScore = br.readLine()) != null) {
				if (countFillingTheBlankScore == docIds.size()) break;
				docIdAndScore = docIdAndScore.replaceAll(System.getProperty("line.separator"), "");
				
				String docId = docIdAndScore.split(",")[0];
				double ql = Double.parseDouble(docIdAndScore.split(",")[1]);
				
				if (!docIds.contains(docId)) continue;
				
				String queryIdAndDocId = queryId + "_" + docId;
				if (idAndScoresMap.containsKey(queryIdAndDocId)) {
					Scores scores = idAndScoresMap.get(queryIdAndDocId);
					scores.ql = ql;
					
					//パラメータチューニング用に計算してあるクエリ尤度の取得
					double[] qlArrayForTuning = new double[5];
					for (int i = 2; i < 7; i++) {
						qlArrayForTuning[i - 2] = Double.parseDouble(docIdAndScore.split(",")[i]);
					}
					scores.qlArrayForTuning = qlArrayForTuning;
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
		double[] qlArrayForTuning = new double[10];
		
		void fixValue() {
			if (bm25 == -1d && ql == -1d) {
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
