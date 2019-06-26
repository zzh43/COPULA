import java.io.*;

class CountAvgdl {
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("java CountAvgdl [target dir]");
			System.exit(1);
		}
		long start = System.currentTimeMillis();
		long total_term = 0;
		long total_file = 0;
		double avg_term = 0;
		double total_subdir = 0;

		File targetDir = new File(args[0]);
		if(targetDir.exists() && targetDir.isDirectory()) {
			File[] dirList = targetDir.listFiles();
			for (int i = 0; i < dirList.length; i++) {
				System.out.println("  " + (i + 1) + ": " + dirList[i].getName());
				File[] subDirList = dirList[i].listFiles();
				for(int j = 0; j < subDirList.length; j++) {
					long consume = System.currentTimeMillis() - start;
					// String subDirNum = subDirList[j].getName();
					System.out.print("    time=" + (consume / 1000) + "(s), dir" + (j + 1));
					File[] fileList = subDirList[j].listFiles();
					total_term = 0;
					total_file = 0;
					for(int k = 0; k < fileList.length; k++) {
						//System.out.println("      " + fileList[k].getName());
						total_file++;
						try {
							FileReader fr = new FileReader(fileList[k]);
							BufferedReader br = new BufferedReader(fr);
							// String line = "";
							while(br.readLine() != null){
								//System.out.println(line);
								total_term++;
							}
							br.close();
							fr.close();
						} catch(Exception e) {
							System.err.println(e);
						}
					}
					avg_term += (double) total_term / total_file;
					total_subdir++;
					System.out.println(": avg " + avg_term / total_subdir);
				}
			}
			System.out.println(avg_term / total_subdir);
		}
	}
}
