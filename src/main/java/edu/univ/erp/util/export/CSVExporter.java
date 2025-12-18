package edu.univ.erp.util.export;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSVExporter {

  private static final Logger log = LoggerFactory.getLogger(CSVExporter.class);

  public static void writeCSV(String filePath, List<String[]> rows) throws Exception {
    log.info("Writing CSV file to {}", filePath);
    try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
      for (String[] row : rows) {
        writer.writeNext(row);
      }
      log.info("CSV writing completed: {} rows", rows.size());
    } catch (Exception ex) {
      log.error("Failed to write CSV to {}", filePath, ex);
      throw ex;
    }
  }
}
