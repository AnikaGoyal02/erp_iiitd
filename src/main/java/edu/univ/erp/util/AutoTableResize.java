package edu.univ.erp.util;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;

public class AutoTableResize {

    public static void autoResize(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        for (int col = 0; col < table.getColumnCount(); col++) {
            int maxWidth = 50;

            TableColumn column = table.getColumnModel().getColumn(col);

            TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
            Component headerComp = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, 0, col);
            maxWidth = Math.max(maxWidth, headerComp.getPreferredSize().width);

            for (int row = 0; row < table.getRowCount(); row++) {
                TableCellRenderer cellRenderer = table.getCellRenderer(row, col);
                Component comp = cellRenderer.getTableCellRendererComponent(
                        table, table.getValueAt(row, col), false, false, row, col);
                maxWidth = Math.max(maxWidth, comp.getPreferredSize().width);
            }

            column.setPreferredWidth(maxWidth + 10);
        }
    }
}
