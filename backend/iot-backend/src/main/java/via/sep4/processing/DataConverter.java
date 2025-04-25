package via.sep4.processing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DataConverter {
    private static final int MAX_ROWS = 1000;
    private static final int MAX_COLS = 100;

    private List<Map<String, String>> data;
    private List<String> headers;
    private boolean hasHeaders;
    private int rows;
    private int cols;

    public DataConverter() {
        this(true);
    }

    public DataConverter(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
        this.headers = new ArrayList<>();
        this.data = new ArrayList<>();
        this.rows = 0;
        this.cols = 0;
    }

    public boolean parseInput(String input, char delimiter) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        data.clear();
        headers.clear();
        rows = 0;
        cols = 0;

        String[] lines = input.split("\n");
        if (lines.length == 0) {
            return false;
        }

        String[] firstLineTokens = lines[0].split(String.valueOf(delimiter));
        cols = firstLineTokens.length;

        if (hasHeaders) {
            for (String token : firstLineTokens) {
                headers.add(token.trim());
            }
        } else {
            for (int i = 0; i < cols; i++) {
                headers.add("column" + (i + 1));
            }

            Map<String, String> rowData = new HashMap<>();
            for (int i = 0; i < firstLineTokens.length; i++) {
                rowData.put(headers.get(i), firstLineTokens[i].trim());
            }
            data.add(rowData);
            rows++;
        }

        int startLine = hasHeaders ? 1 : 0;
        for (int i = startLine; i < lines.length && rows < MAX_ROWS; i++) {
            if (i < startLine) {
                continue;
            }

            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split(String.valueOf(delimiter));
            Map<String, String> rowData = new HashMap<>();

            for (int j = 0; j < Math.min(tokens.length, cols); j++) {
                rowData.put(headers.get(j), tokens[j].trim());
            }

            for (int j = tokens.length; j < cols; j++) {
                rowData.put(headers.get(j), "");
            }

            data.add(rowData);
            rows++;
        }

        return rows > 0;
    }

    public boolean exportToJson(String filename) {
        if (filename == null || data.isEmpty()) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("[\n");

            for (int i = 0; i < data.size(); i++) {
                Map<String, String> row = data.get(i);
                writer.write("  {\n");

                int columnCount = 0;
                for (String header : headers) {
                    String value = row.get(header);
                    writer.write("    \"" + header + "\": ");

                    if (value != null && !value.isEmpty()
                            && value.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$")) {
                        writer.write(value);
                    } else {
                        writer.write("\"" + (value != null ? value : "") + "\"");
                    }

                    if (columnCount < headers.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                    columnCount++;
                }

                writer.write("  }");
                if (i < data.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }

            writer.write("]\n");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportToCsv(String filename, char delimiter) {
        if (filename == null || data.isEmpty()) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int i = 0; i < headers.size(); i++) {
                boolean needsQuotes = headers.get(i).contains(String.valueOf(delimiter)) ||
                        headers.get(i).contains("\"") ||
                        headers.get(i).contains("\n");

                if (needsQuotes) {
                    writer.write("\"" + headers.get(i).replace("\"", "\"\"") + "\"");
                } else {
                    writer.write(headers.get(i));
                }

                if (i < headers.size() - 1) {
                    writer.write(delimiter);
                }
            }
            writer.write("\n");

            for (Map<String, String> row : data) {
                int columnCount = 0;
                for (String header : headers) {
                    String value = row.get(header);

                    boolean needsQuotes = value != null && (value.contains(String.valueOf(delimiter)) ||
                            value.contains("\"") ||
                            value.contains("\n"));

                    if (needsQuotes) {
                        writer.write("\"" + (value != null ? value.replace("\"", "\"\"") : "") + "\"");
                    } else {
                        writer.write(value != null ? value : "");
                    }

                    if (columnCount < headers.size() - 1) {
                        writer.write(delimiter);
                    }
                    columnCount++;
                }
                writer.write("\n");
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, String>> getData() {
        return data;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean hasHeaders() {
        return hasHeaders;
    }
}
