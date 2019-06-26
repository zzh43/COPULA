import java.io.*;

public class SimpleWarcParserDirect {
	
	public static void main(String[] args){
		if(args.length != 2) {
			System.out.println("java SimpleWarcParserDirect [ClueWeb09 source dir] [output dir]");
			System.exit(1);
		}
		long start = System.currentTimeMillis();
		
		int unknownFileNameCounter = 0;
		SimpleWarcParserDirect parser = new SimpleWarcParserDirect();
		InnerFile innerFile = parser.getInnerFileInstance();

		String outputDir = args[1];
		File targetDir = new File(args[0]);
		if( targetDir.exists() && targetDir.isDirectory() ) {
			// Changed on 2019/03/29 for removing .DS_Store files
			File[] dirList = targetDir.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name){
					return !(name.equals(".DS_Store") || name.contains("._"));
				}
			});
			for (int i = 0; i < dirList.length; i++) {
				System.out.println("  " + (i + 1) + ": " + dirList[i].getName());
				// Changed on 2019/03/29 for removing .DS_Store files
				File[] fileList = dirList[i].listFiles(new FilenameFilter(){
					@Override
					public boolean accept(File dir, String name){
						return !(name.equals(".DS_Store") || name.contains("._"));
					}
				});
				for(int j = 0; j < fileList.length; j++) {
					long consume = System.currentTimeMillis() - start;
					System.out.println("    time=" + (consume / 1000) + "(s), dir" + (j + 1) + ": " + fileList[j].getName());
					String subDirNum = fileList[j].getName().replace(".warc.txt", "");
					String subDirPath = outputDir.replaceAll("/$", "") + "/" + dirList[i].getName() + "/" + subDirNum;
					File outputSubDir = new File(subDirPath);
					outputSubDir.mkdir();
					try {
						FileReader fr = new FileReader(fileList[j]);
						BufferedReader br = new BufferedReader(fr);
						String line = "";
		
						while((line = br.readLine()) != null){
							//System.out.println(line);
				
							if(line.startsWith("WARC-TREC-ID")){
								String[] array = line.split(": ");
								if(array.length > 1) innerFile.setName(array[1]);
							}
			
							if(line.equals("WARC/0.18")){
								if(innerFile.hasContent()){
							
									if(innerFile.getName().length() == 0){
										innerFile.setName("unknown_file_name_"+unknownFileNameCounter);
										unknownFileNameCounter ++;
									}
					
									//String outPath = "/mnt/usbhdd/splitClueWeb09/" + dirList[i].getName() + "/" + innerFile.getName().trim() + ".txt";
									String outPath = outputSubDir.getAbsolutePath() + "/" + innerFile.getName() + ".txt";
									//System.out.println(outPath);
									File outputFile = new File(outPath);
									PrintWriter pw = null;
									try{
										if(!outputFile.exists()) outputFile.createNewFile();
										pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
										pw.println(innerFile.getContent());
									}catch(IOException e){
										e.printStackTrace();
									}finally{
										if(pw != null) pw.close();
									}
								}
								innerFile.reset();
							}else{
								innerFile.appendContent(line+System.getProperty("line.separator"));
							}
						}
						//String outPath = "/mnt/usbhdd/splitClueWeb09/" + dirList[i].getName() + "/" + innerFile.getName().trim() + ".txt";
						String outPath = outputSubDir.getAbsolutePath() + "/" + innerFile.getName() + ".txt";
						//System.out.println(outPath);
						File outputFile = new File(outPath);
						PrintWriter pw = null;
						try{
							if(!outputFile.exists()) outputFile.createNewFile();
							pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
							pw.println(innerFile.getContent());
						}catch(IOException e){
							e.printStackTrace();
						}finally{
							if(pw != null) pw.close();
						}
						br.close();
						fr.close();
						innerFile.reset();
					} catch(IOException e) {
						System.err.println(e);
					}
				}
			}
		}
	}
	
	public InnerFile getInnerFileInstance(){
		return new InnerFile();
	}
	
	public class InnerFile{
		private String name = "";
		private StringBuilder content = new StringBuilder();
		
		String getName(){
			return name;
		}
		void setName(String name){
			this.name = name;
		}
		String getContent(){
			return content.toString();
		}
		void appendContent(String content){
			this.content.append(content);
		}
		void setContent(String content){
			this.content = new StringBuilder(content);
		}
		void reset(){
			name = "";
			content = new StringBuilder();
		}
		boolean hasContent(){
			if(content.toString().isEmpty()) return false;
			else return true;
		}
	}
}

