package org.w7ls.vme;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;

public class TextEditorPanel extends JPanel {

    private final JTextPane textPane;
    private final JTextArea lineNumbers;
    private final JLabel statusLabel;
    private final Font editorFont;
    String text = "";

    public TextEditorPanel(String text) {
        super(new BorderLayout());

        // フォント
        editorFont = loadFont("/fonts/FiraCode-Medium.ttf", 14f);

        // テキストペイン
        textPane = new JTextPane();
        textPane.setFont(editorFont);
        initStyles();

        // 初期テキスト
        try {
            textPane.getStyledDocument().insertString(0, text,
                    textPane.getStyle("default"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        highlightAll();

        // 行番号
        lineNumbers = new JTextArea();
        lineNumbers.setFont(editorFont);
        lineNumbers.setEditable(false);
        lineNumbers.setFocusable(false);
        lineNumbers.setBackground(new Color(0x2b, 0x2b, 0x2b));
        lineNumbers.setForeground(new Color(0x88, 0x88, 0x88));
        lineNumbers.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        // スクロールペイン
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumbers);

        // ステータスバー
        statusLabel = new JLabel("行: 1  列: 1");
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusBar.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, new Color(0x44, 0x44, 0x44)));
        statusBar.add(statusLabel);

        // リスナー登録
        textPane.getStyledDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    private void update(javax.swing.event.DocumentEvent e) throws BadLocationException {
                        SwingUtilities.invokeLater(() -> {
                            updateLineNumbers();
                            highlightChangedLine(e);
                        });
                        TextEditorPanel.this.text = e.getDocument().getText(0, e.getLength() - 1);
                    }
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        try {
                            update(e);
                        } catch (BadLocationException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        try {
                            update(e);
                        } catch (BadLocationException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {}
                });

        textPane.addCaretListener(e -> updateStatus());

        updateLineNumbers();

        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    // ── テキストの取得・セット ──────────────────

    public String getText() {
        return textPane.getText();
    }

    public void setText(String text) {
        textPane.setText("");
        try {
            textPane.getStyledDocument().insertString(0, text,
                    textPane.getStyle("default"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        highlightAll();
    }

    // ── プライベートメソッド ────────────────────

    private void initStyles() {
        Style def = textPane.addStyle("default", null);
        StyleConstants.setForeground(def, new Color(0xA9, 0xB7, 0xC6));

        Style keyword = textPane.addStyle("keyword", null);
        StyleConstants.setForeground(keyword, new Color(0xFF, 0x79, 0x00));

        Style string = textPane.addStyle("string", null);
        StyleConstants.setForeground(string, new Color(0x6A, 0x8A, 0x59));

        Style comment = textPane.addStyle("comment", null);
        StyleConstants.setForeground(comment, new Color(0x80, 0x80, 0x80));
    }

    private void highlightAll() {
        SwingUtilities.invokeLater(() -> {
            try {
                String text = textPane.getText();
                String[] lines = text.split("\n", -1);
                int offset = 0;
                for (String line : lines) {
                    highlightLine(offset, line);
                    offset += line.length() + 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void highlightChangedLine(javax.swing.event.DocumentEvent e) {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            int offset = e.getOffset();
            int lineIndex = doc.getDefaultRootElement().getElementIndex(offset);
            Element lineEl = doc.getDefaultRootElement().getElement(lineIndex);
            int start = lineEl.getStartOffset();
            int end = lineEl.getEndOffset();
            String lineText = doc.getText(start, end - start);
            highlightLine(start, lineText);
        } catch (Exception ex) {
            // ignore
        }
    }

    private void highlightLine(int lineStart, String lineText) {
        StyledDocument doc = textPane.getStyledDocument();

        doc.setCharacterAttributes(lineStart, lineText.length(),
                textPane.getStyle("default"), true);

        // キーワード
        String[] keywords = {"<?xml version=\"1.0\"?>",
                             "<AudioModification.xml>",
                             "<AudioModification>",
                             "<Name>",
                             "<ExternalEvent>",
                             "<Name>",
                             "<Container>",
                             "<Name>",
                             "<ExternalId>",
                             "<Path>",
                             "<StateList>",
                             "<State>"};
        for (String kw : keywords) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b" + kw + "\\b").matcher(lineText);
            while (m.find()) {
                doc.setCharacterAttributes(lineStart + m.start(), m.end() - m.start(),
                        textPane.getStyle("keyword"), true);
            }
        }

        // 文字列リテラル
        java.util.regex.Matcher sm = java.util.regex.Pattern
                .compile("\"[^\"]*\"").matcher(lineText);
        while (sm.find()) {
            doc.setCharacterAttributes(lineStart + sm.start(), sm.end() - sm.start(),
                    textPane.getStyle("string"), true);
        }

        // コメント
        java.util.regex.Matcher cm = java.util.regex.Pattern
                .compile("//.*").matcher(lineText);
        while (cm.find()) {
            doc.setCharacterAttributes(lineStart + cm.start(), cm.end() - cm.start(),
                    textPane.getStyle("comment"), true);
        }
    }

    private void updateLineNumbers() {
        int lines = textPane.getText().split("\n", -1).length;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i).append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    private void updateStatus() {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            int pos = textPane.getCaretPosition();
            int line = doc.getDefaultRootElement().getElementIndex(pos) + 1;
            int col = pos - doc.getDefaultRootElement()
                    .getElement(line - 1).getStartOffset() + 1;
            statusLabel.setText("行: " + line + "  列: " + col);
        } catch (Exception e) {
            // ignore
        }
    }

    private Font loadFont(String resourcePath, float size) {
        try {
            var stream = TextEditorPanel.class.getResourceAsStream(resourcePath);
            if (stream == null) {
                System.err.println("フォントが見つかりません: " + resourcePath);
                return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
            }
            return Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
        }
    }
}