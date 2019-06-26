import java.io.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class TermParserDirect {

	public static StringBuilder htmlBody = new StringBuilder(10000);
	public static StringBuilder extractedWordsWithListTags = new StringBuilder(10000);
	//public static StringBuilder extractedWordsWithoutListTags = new StringBuilder(10000);
	public static void main(String[] args){
		if(args.length != 2) {
			System.out.println("java -cp .:nekohtml.jar:nekohtmlSamples.jar:xercesImpl-2.10.0.jar:xml-apis-1.4.01.jar TermParserDirect [splitClueWeb09 source dir] [output dir]");
			System.exit(1);
		}
		long start = System.currentTimeMillis();

		// target	-> splitClueWeb09
		// dir		-> en0000
		// subDir	-> 00
		// file		-> clueweb09-en0000-00-00000.txt
		String outputRootDir = args[1].replaceAll("/$", "");
		File targetDir = new File(args[0]);
		if(targetDir.exists() && targetDir.isDirectory()) {
			File[] dirList = targetDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return (name.equals("en0001"));
					// return !(name.equals(".DS_Store") || name.contains("._"));
				}
			});
			for (int i = 0; i < dirList.length; i++) {
				System.out.println("  " + (i + 1) + ": " + dirList[i].getName());
				String dirPath = outputRootDir + "/" + dirList[i].getName();
				File outputDir = new File(dirPath);
				outputDir.mkdir();
				File[] subDirList = dirList[i].listFiles(new FilenameFilter(){
					@Override
					public boolean accept(File dir, String name) {
						return !(name.equals(".DS_Store") || name.contains("._"));
					}
				});
				for(int j = 0; j < subDirList.length; j++) {
					long consume = System.currentTimeMillis() - start;
					String subDirNum = subDirList[j].getName();
					System.out.println("    time=" + (consume / 1000) + "(s), dir" + (j + 1) + ": " + subDirNum);
					String subDirPath = dirPath + "/" + subDirNum;
					File outputSubDir = new File(subDirPath);
					outputSubDir.mkdir();
					File[] fileList = subDirList[j].listFiles(new FilenameFilter(){
						@Override
						public boolean accept(File dir, String name) {
							return !(name.equals(".DS_Store") || name.contains("._"));
						}
					});
					for(int k = 0; k < fileList.length; k++) {
						//System.out.println("      " + fileList[k].getName());
						String filename = fileList[k].getName();
						if(filename.startsWith("unknown_")) {
							continue;
						}
						htmlBody = new StringBuilder(10000);
						extractedWordsWithListTags = new StringBuilder(10000);
						try {
							FileReader fr = new FileReader(fileList[k]);
							BufferedReader br = new BufferedReader(fr);
							String line = "";
		
							//このプログラムの処理対象となるファイルには，先頭数十行にWARCファイルのメタ情報が記載されていることを想定している．
							//WARCファイルのメタ情報はDOMパーサーでパースしないのでスキップする．
							//ただし，ファイルIDだけは抜き出しておく．
							while((line = br.readLine()) != null){
								line.replaceAll(System.getProperty("line.separator"),"");
								line.trim();
								if(line.length()==0) break;
							}
							while((line = br.readLine()) != null){
								htmlBody.append(line);
							}

							//上で抜き出したHTMLをDOMパーサーでパースして単語を抜き出す．
							//単語を抜き出すタグはタグリストに記載されている．
							DOMParser parser = new DOMParser();
							parser.setFeature("http://xml.org/sax/features/namespaces", false);
							parser.parse(new InputSource(new StringReader(htmlBody.toString())));
				
							Document doc = parser.getDocument();
							Element root = doc.getDocumentElement();
							walkTree(0, root);

							//1. リストタグを含む重要タグの単語をパースした結果を出力する
							String outputContents = extractedWordsWithListTags.toString();
							outputContents = outputContents.replaceAll(" +", " ");
							// tmpStr = tmpStr.replaceAll(" <", "<");
							outputContents = outputContents.replaceAll(" ",System.getProperty("line.separator"));
							File oFile = new File(subDirPath + File.separator +  filename);
							String oPath = oFile.getAbsolutePath();
							//System.out.println("      " + oPath);
							PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(oPath), "UTF-8"));
							pw.write(outputContents);
							pw.flush();
							pw.close();
							//2. リストタグを含む重要タグの単語をパースした結果を出力する
//							outputContents = extractedWordsWithoutListTags.toString();
//							outputContents = outputContents.replaceAll(" +", " ");
//							outputContents = outputContents.replaceAll("> ", ">");
//							// tmpStr = tmpStr.replaceAll(" <", "<");
//							outputContents = outputContents.replaceAll(" ",System.getProperty("line.separator"));
//							oFile = new File("wordsWithoutListTags" + File.separator + dirName + File.separator + warcName + File.separator + filename + ".txt");
//							oPath = oFile.getAbsolutePath();
//							pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(oPath), "UTF-8"));
//							pw.write(outputContents);
//							pw.flush();
//							pw.close();
							br.close();
							fr.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static boolean isHeader(final String line){
		Pattern p = Pattern.compile("^WARC-");
		Matcher m = p.matcher(line);
		if(m.find()) return true;
		
		p = Pattern.compile("^Content-");
		m = p.matcher(line);
		if(m.find()) return true;
		
		for(String metaTag : warcMetaTags){
			if(line.contains(metaTag)) return true;
		}
		
		return false;
	}
	
	/**
	 * 深さ優先で構造化された文書をたどっていく．maxlevelに最大の深さを記録している．
	 * 
	 * @param level
	 * @param elm
	 * @throws Exception
	 */
	private static void walkTree(final int level, final Element elm)throws Exception {
		NodeList children = elm.getChildNodes();
		if (children != null) {
			int len = children.getLength();
			for (int i = 0; i < len; i++) {
				Node child = (Node) children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					walkTree(level + 1, (Element) child);
				} else if (child.getNodeType() == Node.TEXT_NODE) {
					String txt = child.getNodeValue();
					txt = txt.replaceAll("\\n", " ");
					txt = txt.replaceAll("_", " ");
					txt = txt.toLowerCase();
					txt = txt.replaceAll("\\d", " ");
					txt = txt.replaceAll("\\W", " ");
					txt = checkIndexWord(txt);
					txt = txt.trim();
					
					if (txt.length() > 0 && isTagWithList(elm.getTagName())){
						extractedWordsWithListTags.append(txt).append(" ");
					}
//					if(txt.length() > 0 && isTagWithoutList(elm.getTagName())){
//						extractedWordsWithoutListTags.append(txt).append(" ");
//					}
					
				}
			}
		}
	}
	private static boolean isTagWithList(String element){
		for(String tag : tagsWithList){
			if(tag.equals(element)){
				return true;
			}
		}
		return false;
	}
//	private static boolean isTagWithoutList(String element){
//		for(String tag : tagsWithoutList){
//			if(tag.equals(element)){
//				return true;
//			}
//		}
//		return false;
//	}
	private static String checkIndexWord(String str){
	  	String index_str = "";
	    String[] split = str.split("\\s+");
	    for(int i = 0; i < split.length; i++){
	      String tmp = split[i];
	        index_str += tmp + " ";
	    }
	    return index_str.trim();
	}
	
	//タグリスト（リストタグを含む）
	private static String[] tagsWithList = {
		//list tag names
		"TABLE",
		"TH",
		"TD",
		"TR",
		"UL",
		"LI",
		"DL",
		"OL",
		"DD",
		"DT",
		"FORM",
		//base tag names
		"TITLE",
		"DIV",
		"A",
		"P",
		"B",
		"I",
		"U",
		"SPAN",
		"TBODY",
		"FONT",
		"OPTION",
		"SELECT",
		"STRONG",
		"CENTER",
		"BODY",
		"EM",
		"SMALL",
		"FIELDSET",
		"SUP",
		"H1",
		"H2",
		"H3",
		"H4",
		"H5",
	};
	
	//タグリスト（リストタグを含まない）
//	private static String[] tagsWithoutList = {
//		//base tag names
//		"TITLE",
//		"DIV",
//		"A",
//		"P",
//		"B",
//		"I",
//		"U",
//		"SPAN",
//		"TBODY",
//		"FONT",
//		"OPTION",
//		"SELECT",
//		"STRONG",
//		"CENTER",
//		"BODY",
//		"EM",
//		"SMALL",
//		"FIELDSET",
//		"SUP",
//		"H1",
//		"H2",
//		"H3",
//		"H4",
//		"H5",
//	};
	
	//WARCファイルのメタタグ
	private static String[] warcMetaTags = {
		//warcinfo
		"operator:",
		"software:",
		"robots:",
		"hostname:",
		"ip:",
		"http-header-user-agent:",
		"http-header-from:",
		"isPartOf:",
		"description:",
		"format:",
		"conformsTo:",
		//response
		"HTTP/1.1",
		"Date:",
		"Server:",
		"Last-Modified:",
		"ETag:",
		"Accept-Ranges:",
		"Connection:",
		"Pragma:",
		"Cache-Control:",
		"X-Powered-By:",
		"Set-Cookie:",
		"Expires:",
		"Vary:",
		"Via:",
		"X-Cache:",
		"X-Cache-Lookup:"
	};
}
