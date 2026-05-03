package org.w7ls.vme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;

public class LinkedFileListPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;
    private AudioModification.ExternalEvent currentEvent;
    private final AudioEditorPanel editorPanel;

    public LinkedFileListPanel(AudioEditorPanel editorPanel) {
        super(new BorderLayout());
        this.editorPanel = editorPanel;

        tableModel = new DefaultTableModel(new Object[]{"リンク済みファイル"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setTableHeader(null);

        // 選択でオーディオパネルに反映
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && currentEvent != null && row < currentEvent.audios.size()) {
                    editorPanel.setAudioFile(currentEvent.audios.get(row));
                }
            }
        });

        // 右クリックメニュー
        JPopupMenu popup = new JPopupMenu();
        JMenuItem unlinkItem = new JMenuItem("リンクを解除");
        unlinkItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0 || currentEvent == null) return;
            int confirm = JOptionPane.showConfirmDialog(null,
                    "リンクを解除しますか？", "確認", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String fileName = (String) tableModel.getValueAt(row, 0);
                currentEvent.linkedFiles.remove(fileName);
                currentEvent.audios.remove(row);
                tableModel.removeRow(row);
            }
        });
        popup.add(unlinkItem);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            private void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
                popup.show(table, e.getX(), e.getY());
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setEvent(AudioModification.ExternalEvent event) {
        this.currentEvent = event;
        tableModel.setRowCount(0);
        if (event == null) return;
        for (VMAudioFile audio : event.audios) {
            tableModel.addRow(new Object[]{audio.audioName});
        }
    }
}