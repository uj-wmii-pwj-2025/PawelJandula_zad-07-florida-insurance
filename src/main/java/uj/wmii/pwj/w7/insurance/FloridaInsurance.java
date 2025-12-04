package uj.wmii.pwj.w7.insurance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FloridaInsurance {

    private static final String ZIP_FILE_PATH = "FL_insurance.csv.zip";

    public static void main(String[] args) {
        try {
            List<InsuranceEntry> data = loadDataFromZip(ZIP_FILE_PATH);

            generateCountFile(data);
            generateTiv2012File(data);
            generateMostValuableFile(data);

            System.out.println("Zakończono generowanie plików.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateCountFile(List<InsuranceEntry> data) throws IOException {
        long count = data.stream()
                .map(InsuranceEntry::county)
                .distinct()
                .count();

        writeLineToFile("count.txt", null, String.valueOf(count));
    }

    private static void generateTiv2012File(List<InsuranceEntry> data) throws IOException {
        double totalTiv2012 = data.stream()
                .mapToDouble(InsuranceEntry::tiv2012)
                .sum();

        String formattedValue = String.format(Locale.US, "%.2f", totalTiv2012);
        
        writeLineToFile("tiv2012.txt", null, formattedValue);
    }

    private static void generateMostValuableFile(List<InsuranceEntry> data) throws IOException {
        List<String> resultLines = data.stream()
                .collect(Collectors.groupingBy(
                        InsuranceEntry::county,
                        Collectors.summingDouble(entry -> entry.tiv2012() - entry.tiv2011())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> {
                    String countyName = entry.getKey();
                    String value = String.format(Locale.US, "%.2f", entry.getValue());
                    return countyName + "," + value;
                })
                .collect(Collectors.toList());

        writeListToFile("most_valuable.txt", "country,value", resultLines);
    }

    private static void writeLineToFile(String fileName, String header, String content) throws IOException {
        writeListToFile(fileName, header, Collections.singletonList(content));
    }

    private static void writeListToFile(String fileName, String header, List<String> lines) throws IOException {
        try (PrintWriter writer = new PrintWriter(fileName, StandardCharsets.UTF_8)) {
            if (header != null) {
                writer.println(header);
            }
            for (String line : lines) {
                writer.println(line);
            }
        }
    }

    private static List<InsuranceEntry> loadDataFromZip(String zipFilePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            ZipEntry entry = zipFile.entries().nextElement();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zipFile.getInputStream(entry), StandardCharsets.UTF_8))) {
                
                return reader.lines()
                        .skip(1) 
                        .map(InsuranceEntry::parse)
                        .collect(Collectors.toList());
            }
        }
    }
    record InsuranceEntry(String county, double tiv2011, double tiv2012) {
        static InsuranceEntry parse(String line) {
            String[] columns = line.split(",");
            return new InsuranceEntry(
                    columns[2],
                    Double.parseDouble(columns[7]), 
                    Double.parseDouble(columns[8])  
            );
        }
    }
}