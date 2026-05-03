package org.w7ls.vme;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class FileListPanel extends JPanel {

    // リンク済みファイルを追跡
    private final Set<String> linkedFiles = new HashSet<>();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final List<File> files = new ArrayList<>();
    private final JButton linkButton;

    // 外部からイベント選択を受け取るためのコールバック
    private AudioModification.ExternalEvent selectedEvent = null;
    private Runnable onLinked = null;

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp3", "wem", "ogg", "wav", "m4a"
    );

    public FileListPanel(File initialFolder, AudioEditorPanel editorPanel) {
        super(new BorderLayout());

        // テーブルを先に初期化
        tableModel = new DefaultTableModel(new Object[]{"ファイル名"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setTableHeader(null);
        table.setDefaultRenderer(Object.class, new LinkedFileRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateLinkButton();
        });

        // 上部：フォルダ選択ボタン（tableModel初期化後）
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel folderLabel = new JLabel("フォルダ未選択");
        folderLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        JButton folderButton = new JButton("📂");
        folderButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (initialFolder != null) chooser.setCurrentDirectory(initialFolder);
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                folderLabel.setText(selected.getName());
                files.clear();
                tableModel.setRowCount(0);
                loadFolder(selected);
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && editorPanel != null) {
                    editorPanel.setAudioFile(
                            new VMAudioFile(files.get(row))
                    );
                }
                updateLinkButton();
            }
        });
        topPanel.add(folderButton, BorderLayout.EAST);
        topPanel.add(folderLabel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, new Color(0x44, 0x44, 0x44)));

        JScrollPane scrollPane = new JScrollPane(table);

        linkButton = new JButton("🔗 リンク");
        linkButton.setVisible(false);
        linkButton.addActionListener(e -> doLink());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(linkButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setupDrop();

        if (initialFolder != null && initialFolder.isDirectory()) {
            folderLabel.setText(initialFolder.getName());
            loadFolder(initialFolder);
        }
    }

    // ── 公開メソッド ──────────────────────────

    // 右パネルのイベント選択と連動
    public void setSelectedEvent(AudioModification.ExternalEvent event) {
        this.selectedEvent = event;
        updateLinkButton();
        table.repaint(); // リンク済み色を更新
    }

    // リンク完了時のコールバック
    public void setOnLinked(Runnable onLinked) {
        this.onLinked = onLinked;
    }

    // リンク済みかどうかの判定（外部から参照用）
    public boolean isLinked(String fileName) {
        return linkedFiles.contains(fileName);
    }

    // ── プライベートメソッド ──────────────────

    private void loadFolder(File folder) {
        for (File f : Objects.requireNonNull(folder.listFiles())) {
            if (f.isFile() && isSupportedFile(f)) {
                addFile(f);
            }
        }
    }

    private void addFile(File file) {
        // 重複チェック
        for (File f : files) {
            if (f.getAbsolutePath().equals(file.getAbsolutePath())) return;
        }
        files.add(file);
        tableModel.addRow(new Object[]{file.getName()});
    }

    private boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private void updateLinkButton() {
        int row = table.getSelectedRow();
        boolean fileSelected = row >= 0;
        boolean eventSelected = selectedEvent != null;
        linkButton.setVisible(fileSelected && eventSelected);
    }

    private void doLink() {
        int row = table.getSelectedRow();
        if (row < 0 || selectedEvent == null) return;

        File file = files.get(row);
        String fileName = file.getName();

        if (!selectedEvent.linkedFiles.contains(fileName)) {
            selectedEvent.linkedFiles.add(fileName);
            selectedEvent.audios.add(new VMAudioFile(file));  // ← 追加
        }

        linkedFiles.add(fileName);
        linkButton.setVisible(false);
        table.repaint();

        if (onLinked != null) onLinked.run();
        paintComponents(getGraphics());
    }

    private void setupDrop() {
        new DropTarget(table, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable t = e.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>)
                                t.getTransferData(DataFlavor.javaFileListFlavor);
                        for (File f : droppedFiles) {
                            if (isSupportedFile(f)) addFile(f);
                        }
                    }
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
            }
        });
    }

    // ── カスタムレンダラー ────────────────────

    private class LinkedFileRenderer extends DefaultTableCellRenderer {
        private final Color linkedColor   = new Color(0x4a, 0x8a, 0x4a);
        private final Color defaultColor  = new Color(0x2b, 0x2b, 0x43);

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String fileName = (String) value;
                if (linkedFiles.contains(fileName)) {
                    setBackground(linkedColor);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(defaultColor);
                    setForeground(Color.LIGHT_GRAY);
                }
            }
            return this;
        }
    }
}