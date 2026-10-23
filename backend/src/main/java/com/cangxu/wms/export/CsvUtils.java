package com.cangxu.wms.export;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    /**
     * Excel on Windows needs a UTF-8 BOM to display Chinese correctly.
     */
    public static byte[] write(List<String> headers, List<List<?>> rows) {
        StringBuilder csv = new StringBuilder("﻿");
        appendRow(csv, headers);
        for (List<?> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append("\r\n");
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
