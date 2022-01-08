package org.matsim.prepare.freight.bvm;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Objects;

/**
 * @author zmeng
 *
 * convert data
 * origin   destination1    value2   destination2    value2   destination3    value3
 *
 * to
 * origin destination1 value1
 * origin destination2 value2
 * origin destination3 value3
 *
 */
public class ConvertFiles {
    private final static File folder = new File("../shared-svn/projects/realLabHH/data/BVM_Modelldaten/UmlegungsmatrizenAnalyse2018/");
    private final static Logger log = Logger.getLogger(ConvertFiles.class);

    public static void main(String[] args) throws IOException {

        File[] listOfFolder = folder.listFiles();

        assert listOfFolder != null;
        for (File folder : listOfFolder) {
            if(!folder.getName().startsWith(".")){
                log.info("go to folder" + folder);
                for (File file : Objects.requireNonNull(folder.listFiles())) {
                    if(!file.getName().startsWith(".")){
                        log.info("convert file" + file);
                        convertFile(file);
                    }
                }
            }
        }
    }

    private static void convertFile(File file) {
        try {
            String fileName = file.getName().replace(".fma","");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            File writeFolder = new File(folder + "_convert/");
            if (!writeFolder.exists()){
                writeFolder.mkdirs();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(writeFolder + "/" + fileName + "_convert.csv"));
            writer.write("origin,destination,"+fileName);
            String line = reader.readLine();
            int num = 0;
            while (line != null){
                num++;
                line = reader.readLine();
                if(num < 11)
                    continue;
                processLine(writer,line);
            }
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processLine(BufferedWriter writer, String line) {
        if(line != null){
            String[] array = line.split(" ");
            for (int i = 1; i < array.length-1; i++) {
                try {
                    writer.newLine();
                    writer.write(array[0] + "," + array[i] + "," +array[++i]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
