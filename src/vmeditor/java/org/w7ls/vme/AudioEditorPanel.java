package org.w7ls.vme;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class AudioEditorPanel extends JPanel {
    @Nullable private VMAudioFile audioFile;
    private JButton playButton;
    private JButton stopButton;
    private TimelinePanel timeline;

    private JLabel nonSelectingLabel;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private static final String EMPTY = "empty";
    private static final String EDITOR = "editor";

    private boolean plaing;

    public AudioEditorPanel() {
        super(new BorderLayout());
        nonSelectingLabel = new JLabel("ファイルを選択してください", SwingConstants.CENTER);
        nonSelectingLabel.setForeground(Color.WHITE);
        nonSelectingLabel.setPreferredSize(new Dimension(getWidth(), 150));

        // 再生ボタン
        playButton = new JButton("▶ 再生");
        playButton.setEnabled(true);
        playButton.addActionListener(e -> togglePlay());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.add(playButton);

        // タイムライン
        timeline = new TimelinePanel();

        cards.add(nonSelectingLabel, EMPTY);
        cards.add(timeline, EDITOR);

        add(cards, BorderLayout.CENTER);

        cardLayout.show(cards, EMPTY);
    }

    // ファイルをセットして表示を更新
    public void setAudioFile(VMAudioFile audioFile) {
        this.audioFile = audioFile;
        paintComponent(getGraphics());
        paint(getGraphics());
        this.timeline.update();
        this.timeline.paintComponent(timeline.getGraphics());

        cardLayout.show(cards, EDITOR);
        timeline.update();
    }

    // 再生開始
    private void startPlay() {
        Objects.requireNonNull(audioFile).startTrimed();
    }

    // 再生停止
    private void stopPlay() {
        // TODO: 実装
    }

    // 再生/停止トグル
    private void togglePlay() {
        plaing = !plaing;
    }

    private void paintUpdate() {
        timeline.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        super.paintComponent(g);
    }

    private class TimelinePanel extends JPanel {
        private double trimMin;
        private double trimMax;

        private TrimPosLabel minCircle;
        private TrimPosLabel maxCircle;
        private int tipBorder; // パネルの両端から，再生ラインまでの距離
        private int lineHeight; // 再生ライン中央の高さ
        private double position; // 0 ~ 1
        private int trimRectHeight;

        private static Color trimColor = new Color(79, 192, 252);

        public TimelinePanel() {
            tipBorder = 20;
            lineHeight = 50;
            trimRectHeight = 30;

            minCircle = new TrimPosLabel(6);
            maxCircle = new TrimPosLabel(6);

            setLocation(0, 0);
            setPreferredSize(new Dimension(getWidth(), 150));
            setLayout(null);

            add(minCircle);
            add(maxCircle);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    update();
                }
            });
        }

        public void update() {
            if (audioFile != null) {
                setPositions(
                        audioFile.trimStart / audioFile.duration,
                        audioFile.trimEnd / audioFile.duration,
                        audioFile.position / audioFile.duration
                );
            }else {
                setPositions(
                        0,
                        1,
                        0
                );
            }

            circleUpdate();
        }

        private void circleUpdate() {
            int lineWidth = getLineWidth();

            minCircle.setLocation(
                    (int)(trimMin * lineWidth + tipBorder) - minCircle.r,
                    lineHeight - minCircle.r
            );
            maxCircle.setLocation(
                    (int)(trimMax * lineWidth + tipBorder) - maxCircle.r,
                    lineHeight - maxCircle.r
            );
        }

        private void setPositions(double trimStart, double trimEnd, double position) {
            this.trimMin = trimStart;
            this.trimMax = trimEnd;
            this.position = position;
        }

        @Override
        protected void paintComponent (Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int lineWidth = getLineWidth();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // 再生線
            g2.setColor(Color.WHITE);
            g2.drawLine(tipBorder, lineHeight, getWidth() - tipBorder, lineHeight);

            // トリミング範囲
            int startX = minCircle.getX() + minCircle.r;
            int endX   = maxCircle.getX() + maxCircle.r;
            g2.setColor(new Color(trimColor.getRed(), trimColor.getGreen(), trimColor.getBlue(), 40));
            g2.fillRect(startX, lineHeight, endX - startX, trimRectHeight);

            // ファイル名
            if (audioFile != null)  {
                g2.setFont(getFont());
                g2.drawString(audioFile.audioName, tipBorder, lineHeight - 30);
            }
        }

        private int getLineWidth() {
            return getWidth() - 2 * tipBorder;
        }

        private class TrimPosLabel extends JLabel {
            private final int r;
            private int dragOffsetX = 0;
            private int startX = 0;
            private int currentX = 0;
            private int pos;

            TrimPosLabel(int r_) {
                r = r_;
                setSize(r * 2, trimRectHeight + r * 2);

                MouseAdapter ma = new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        currentX = getX() + r;
                        startX = e.getXOnScreen() - currentX;
                    }

                    @Override
                    public void mouseDragged(MouseEvent e) {
                        currentX = e.getXOnScreen() - startX;
                        currentX = Math.max(tipBorder, Math.min(currentX, getParent().getWidth() - tipBorder));
                        if (TrimPosLabel.this == minCircle) {
                            currentX = Math.min(currentX, maxCircle.getX() + maxCircle.r - 1);
                        } else {
                            currentX = Math.max(currentX, minCircle.getX() + minCircle.r + 1);
                        }
                        setLocation(currentX - r, getY());

                        if (audioFile != null) {
                            double ratio = (double)(currentX - tipBorder) / getLineWidth();
                            if (TrimPosLabel.this == minCircle) {
                                audioFile.trimStart = ratio * audioFile.duration;
                            } else {
                                audioFile.trimEnd = ratio * audioFile.duration;
                            }
                        }
                        getParent().repaint();
                    }
                };

                addMouseListener(ma);
                addMouseMotionListener(ma);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(trimColor);
                g2.drawLine(r, r, r, 50);
                g2.fillOval(0, trimRectHeight, r * 2, r * 2);
            }
        }
    }
}