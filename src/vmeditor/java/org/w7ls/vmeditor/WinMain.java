package org.w7ls.vmeditor;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WinMain {
    public static final String pathPrjDir = "workspace/projects/";
    public static final String pathVmodDir = "vmod/";
    public static final String pathRvmprj = pathPrjDir + ".rvmprj";
    public static final String pathDefaultXml = pathVmodDir + "default.xml";

    public static AudioModification _mod;
    @TargetName(name = "Tab") public static JTabbedPane tabs;

    public static void main(String[] args) throws Exception {
        _mod = new AudioModification();
        _mod.load(new File(pathDefaultXml));
        String xmlIn = Files.readString(Paths.get(_mod.getFile().toURI()));
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("W7LS Framework VMEditor v0.1.0") {
                @Override
                public void paintComponents(Graphics g) {
                    super.paintComponents(g);

                }
            };

            frame.setJMenuBar(mkMenuBar());

            boolean[] edited = new boolean[]{false};
            var tab = mkEditorTab(_mod, () -> {
                edited[0] = true;
            });

            tabs = new JTabbedPane();
            tabs.addTab("VMエディタ", tab);

            frame.add(tabs);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setSize(900, 600);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    frame.dispose(); // TODO
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }

    private static JMenuBar mkMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("ファイル");

        // open project
        JMenuItem openProject = new JMenuItem("プロジェクトを開く");
        openProject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openProject.addActionListener(ac -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("プロジェクトファル", "vmprj"));
            fileChooser.setMultiSelectionEnabled(false);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File cfile = fileChooser.getSelectedFile();
                try (ProjectReader prjR = new ProjectReader(cfile)) {
                    var tab = mkEditorTab(prjR.getAudioModification(), () -> {});
                    tabs.addTab(prjR.getProjectName(), tab);
                    tabs.setSelectedComponent(tab);
                } catch (Exception e) {
                    StringBuilder sb = new StringBuilder();
                    Arrays.stream(e.getStackTrace()).forEach(st -> {
                        sb.append("\n").append(st);
                    });
                    JOptionPane.showMessageDialog(null, "プロジェクトの読み込みに失敗しました\n\n詳細 :\n" + e + sb
                            , "警告", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        });

        String wowsPath = "C:/Games/World_of_Warships/bin/12506899/res_mods/banks/Mods"; // TODO TEST

        // export to wows
        JMenuItem exportToWows = new JMenuItem("wowsに適用");
        exportToWows.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        exportToWows.addActionListener(e -> {
            try {
                String dirPath = wowsPath+"/テストmod";
                try {
                    Files.delete(Path.of(dirPath));
                } catch (IOException ignore) {}
                Files.createDirectories(Path.of(dirPath));
                Files.copy(Path.of(pathDefaultXml), Path.of(dirPath + "/mod.xml"));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "エクスポートに失敗しました", "警告", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
            JOptionPane.showMessageDialog(null, "正常にエクスポートされました");
        });

        fileMenu.add(openProject);
        fileMenu.add(exportToWows);
        menuBar.add(fileMenu);
        return menuBar;
    }

    /**
     *
     * @param mod
     * @param edit 編集を行ったときのリスナー．&lt;Event name, 編集を行ったExternalEvent&gt;
     * @return
     */
    private static Component mkEditorTab(AudioModification mod, Runnable edit) {
// 子要素のイニシャライズ (
        EventPreTable epreTable = new EventPreTable(mod);
        JScrollPane epreScroll = new JScrollPane(epreTable);

        AudioEditorPanel audioEditorPanel = new AudioEditorPanel();

        LinkedFilePreTable vpreList = new LinkedFilePreTable(mod);

        epreTable.addDeleteListener(e -> {
            audioEditorPanel.setAudioFile(null);
            edit.run();
        });
        epreTable.addAudioOpeListener((e, v) -> {
            vpreList.update(e.get(0));
            edit.run();
        });
        epreTable.addSelectionListener(show -> {
            vpreList.update(show);
            if (!show.audios.isEmpty())
                audioEditorPanel.setAudioFile(show.audios.get(0));
            else
                audioEditorPanel.setAudioFile(null);
        });
        epreScroll.addMouseListener(
                Arrays.stream(epreTable.getMouseListeners())
                        .filter(l -> l instanceof EventPreTable.EventPreTableShowPopupMenuAdapter)
                        .toList()
                        .get(0)
        );

        vpreList.addSelectedListener(audioFile -> {
            audioEditorPanel.setAudioFile(null);
            audioEditorPanel.setAudioFile(audioFile);
        });

        vpreList.addSetListener(audioFile -> {
            if (audioFile == null)
                audioEditorPanel.setAudioFile(null);
            edit.run();
        });
//)

        JSplitPane rSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JPanel(),
                vpreList
        );
        JSplitPane lSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                epreScroll,
                audioEditorPanel
        );
        rSplit.setResizeWeight(0.6);
        rSplit.setBorder(null);
        lSplit.setResizeWeight(0.75);
        lSplit.setBorder(null);

        JSplitPane tab = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                lSplit,
                rSplit
        );
        tab.setDividerSize(1);
        tab.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        super.paint(g);

                        g.setColor(Color.GRAY);

                        if (tab.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                            int x = getWidth() / 2;
                            g.drawLine(x, 0, x, getHeight());
                        } else {
                            int y = getHeight() / 2;
                            g.drawLine(0, y, getWidth(), y);
                        }
                    }
                };
            }
        });
        tab.setBorder(null);
        JPanel root = new JPanel(new BorderLayout());
        root.add(tab, BorderLayout.CENTER);
        return root;
    }
}
