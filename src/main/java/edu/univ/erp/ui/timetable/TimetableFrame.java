package edu.univ.erp.ui.timetable;

import edu.univ.erp.domain.Section;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimetableFrame extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(TimetableFrame.class);

    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("H:mm");

    public TimetableFrame(List<Section> sections) {
        super("My Timetable");
        log.debug("Initializing TimetableFrame with {} sections", sections.size());

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // build model
        log.debug("Building TimetableModel");
        TimetableModel model = new TimetableModel(sections);

        String[] cols = new String[TimetableModel.DAYS.size() + 1];
        cols[0] = "Time";

        for (int i = 0; i < TimetableModel.DAYS.size(); i++) {
            cols[i + 1] = TimetableModel.DAYS.get(i);
        }

        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        log.debug("Populating timetable rows");

        for (LocalTime slotStart : model.getSlotStarts()) {
            String[] row = new String[cols.length];
            row[0] = slotStart.format(TIME_LABEL);

            int rowIdx = model.getSlotStarts().indexOf(slotStart);
            for (int c = 0; c < TimetableModel.DAYS.size(); c++) {
                String day = TimetableModel.DAYS.get(c);
                row[c + 1] = model.getCell(rowIdx, day);
            }
            tableModel.addRow(row);
        }

        JTable table = new JTable(tableModel);
        table.setRowHeight(90);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);

        DefaultTableCellRenderer multi = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                JTextArea area = new JTextArea();
                area.setText(value == null ? "" : value.toString());
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setOpaque(true);

                if (row % 2 == 0) area.setBackground(new Color(0xF7F7F7));
                else area.setBackground(Color.WHITE);

                return area;
            }
        };

        log.debug("Setting renderers for timetable columns");

        for (int col = 1; col < table.getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(multi);
        }

        for (int col = 0; col < table.getColumnCount(); col++) {
            int width = 120;
            for (int row = 0; row < table.getRowCount(); row++) {
                Component comp = table.prepareRenderer(
                        table.getCellRenderer(row, col), row, col);
                width = Math.max(width, comp.getPreferredSize().width + 20);
            }
            table.getColumnModel().getColumn(col).setPreferredWidth(width);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scroll, BorderLayout.CENTER);

        log.info("TimetableFrame initialized and ready");
    }
}
