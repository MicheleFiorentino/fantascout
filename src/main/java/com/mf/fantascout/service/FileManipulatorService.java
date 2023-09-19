package com.mf.fantascout.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileManipulatorService {

    private final String currentDirectory;

    FileManipulatorService(){
        this.currentDirectory = System.getProperty("user.dir");
    }

    public List<String> readFile(String fileName){
        String filePath = this.currentDirectory + "/" + fileName;
        List<String> fileContent = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(filePath))){
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContent;
    }

    public boolean writeFile(String fileName, String line, boolean append){
        String filePath = this.currentDirectory + "/" + fileName;
        return writeToFile(filePath,line, append);
    }

    public boolean writeFile(String fileName, List<String> lines, boolean append){
        String filePath = this.currentDirectory + "/" + fileName;
        return writeToFile(filePath,lines, append);
    }

    private boolean writeToFile(String filePath, String line, boolean append){
        List<String> lineInArray = new ArrayList<>();
        lineInArray.add(line);
        return writeToFile(filePath, lineInArray, append);
    }

    private static boolean writeToFile(String filePath, List<String> lines, boolean append) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append))) {
            for(String line : lines){
                String formattedLine = line + "\n";
                writer.write(formattedLine);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
