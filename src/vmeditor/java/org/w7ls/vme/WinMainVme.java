package org.w7ls.vme;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WinMainVme {
    private WinMainVme() {}

    static HashMap<AudioModification.ExternalEvent, Boolean> visibleEvent = new HashMap<>();

    public static JPanel create(AudioModification mod,
                                Consumer<AudioModification.ExternalEvent> onSelectionChanged) {
        JPanel root = new JPanel(new BorderLayout());

        // イベントテーブル
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"イベント"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (AudioModification.ExternalEvent event : mod.getEvents()) {
            if (visibleEvent.getOrDefault(event, false)) {
                tableModel.addRow(new Object[]{event.shortName});
            }
        }

        // オーディオエディタ、ファイルリスト、リンクファイルリスト
        AudioEditorPanel audioEditorPanel = new AudioEditorPanel();
        FileListPanel fileListPanel = new FileListPanel(new File("vmod"), audioEditorPanel);
        fileListPanel.setOnLinked(() -> System.out.println("リンク済み"));
        LinkedFileListPanel linkedFileList = new LinkedFileListPanel(audioEditorPanel);

        JTable table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    AudioModification.ExternalEvent event = mod.getEvents().get(row);
                    fileListPanel.setSelectedEvent(event);
                    linkedFileList.setEvent(event);
                    onSelectionChanged.accept(event);
                }
            }
        });

        // 右クリックメニュー
        JPopupMenu eventPopup = new JPopupMenu();

        JMenuItem linkFileItem = new JMenuItem("ファイルをリンク");
        linkFileItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            AudioModification.ExternalEvent event = mod.getEvents().get(row);
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "音声ファイル", "wav", "ogg", "mp3", "wem", "m4a"));
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                for (File f : chooser.getSelectedFiles()) {
                    if (!event.linkedFiles.contains(f.getName())) {
                        event.linkedFiles.add(f.getName());
                        event.audios.add(new VMAudioFile(f));
                    }
                }
                linkedFileList.setEvent(event);
                table.repaint();
            }
            linkedFileList.paintComponents(linkedFileList.getGraphics());
        });

        JMenuItem deleteEventItem = new JMenuItem("イベントを削除");
        deleteEventItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) return;
            int confirm = JOptionPane.showConfirmDialog(null,
                    "イベントを削除しますか？", "確認", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                AudioModification.ExternalEvent event = mod.getEvents().get(row);
                visibleEvent.put(event, false); // 非表示にする
                tableModel.removeRow(row);
            }
            linkedFileList.paintComponents(linkedFileList.getGraphics());
        });

        JMenuItem addEventItem = new JMenuItem("イベントを追加");
        addEventItem.addActionListener(e -> {
            // 非表示のイベントを取得
            List<AudioModification.ExternalEvent> hiddenEvents = visibleEvent.entrySet().stream()
                    .filter(entry -> !entry.getValue())
                    .map(Map.Entry::getKey)
                    .toList();

            if (hiddenEvents.isEmpty()) {
                JOptionPane.showMessageDialog(null, "追加できるイベントがありません");
                return;
            }

            // リストで表示
            DefaultListModel<String> listModel = new DefaultListModel<>();
            for (AudioModification.ExternalEvent ev : hiddenEvents) {
                listModel.addElement(ev.shortName);
            }
            JList<String> list = new JList<>(listModel);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            JScrollPane scroll = new JScrollPane(list);
            scroll.setPreferredSize(new Dimension(300, 400));

            int result = JOptionPane.showConfirmDialog(null, scroll,
                    "追加するイベントを選択", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                for (int idx : list.getSelectedIndices()) {
                    AudioModification.ExternalEvent selected = hiddenEvents.get(idx);
                    visibleEvent.put(selected, true);
                    tableModel.addRow(new Object[]{selected.shortName});
                }
            }
        });

        eventPopup.add(linkFileItem);
        eventPopup.addSeparator();
        eventPopup.add(addEventItem);
        eventPopup.add(deleteEventItem);

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
                deleteEventItem.setEnabled(row >= 0);
                linkFileItem.setEnabled(row >= 0);
                eventPopup.show(table, e.getX(), e.getY());
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);

        // 左：イベントリスト（上）＋ オーディオエディタ（下）
        JSplitPane leftSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScroll,
                audioEditorPanel
        );
        leftSplit.setResizeWeight(0.75);
        leftSplit.setBorder(null);

        // 右：ファイルリスト（上）＋ リンクファイルリスト（下）
        JSplitPane rightSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                fileListPanel,
                linkedFileList
        );
        rightSplit.setResizeWeight(0.6);
        rightSplit.setBorder(null);

        // 左右を分割
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftSplit,
                rightSplit
        );
        mainSplit.setResizeWeight(0.4);
        mainSplit.setBorder(null);

        root.add(mainSplit, BorderLayout.CENTER);
        return root;
    }

    public static void main(String[] args) throws Exception {
        String xmlIn = Files.readString(
                Paths.get("C:/Games/wows_my_mod_installer/python/vmod/mod.xml"));

        AudioModification mod = new AudioModification();
        mod.load(new File("vmod/default.xml"));

        for (AudioModification.ExternalEvent event : mod.getEvents()) {
            visibleEvent.put(event, true);
        }

        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("VM Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 600);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Test1", create(mod, event -> {}));
            tabs.addTab("Test2", new TextEditorPanel(xmlIn));

            frame.add(tabs);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {}
            });
            frame.setVisible(true);
        });
    }
}