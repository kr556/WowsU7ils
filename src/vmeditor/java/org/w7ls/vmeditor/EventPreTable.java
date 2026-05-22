package org.w7ls.vmeditor;

import org.jetbrains.annotations.NotNull;
import org.w7ls.vmeditor.AudioModification.ExternalEvent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.w7ls.vmeditor.EventPreTable.TxtKey.*;

public class EventPreTable extends JTable {
    AudioModification _mod;
    private final List<ExternalEvent> _externalEvents;
    private final List<ExternalEvent> _invisibleEvents = new ArrayList<>();
    private final List<Consumer<ExternalEvent>> _selectionListener = new ArrayList<>();
    private final List<Consumer<ExternalEvent>> _deleteListener = new ArrayList<>();
    private final List<Consumer<ExternalEvent>> _addListener = new ArrayList<>();
    private final List<BiConsumer<List<ExternalEvent>, List<VMAudioFile>>> _audioOpeListener = new ArrayList<>();

    public enum TxtKey {
        TXT_LINK_FILE_EVENT,
        TXT_ADD_EVENT,
        TXT_DEL_EVENT,
        // ファイル選択関連のポップアップ
        TITLE_NOTING_FILE_POPUP,
        TXT_NOTING_FILE_POPUP_INFO,
        // イベント削除の確認ポップアップ
        TITLE_EVENT_DEL_POPUP,
        TXT_EVENT_DEL_POPUP_INFO,
        // イベント追加の確認ポップアップ
        TITLE_EVENT_ADD_POPUP,
        TITLE_NOTHING_ADD_EVENT,
        TXT_NOTHING_ADD_EVENT_INFO
    }

    private static final Map<TxtKey, String> text = Map.of(
            TXT_LINK_FILE_EVENT, "ファイルをリンクする",
            TXT_ADD_EVENT, "イベントを追加",
            TXT_DEL_EVENT, "イベントを削除",
            TITLE_EVENT_DEL_POPUP, "確認",
            TXT_EVENT_DEL_POPUP_INFO, "リンクしたオーディオも解除されますが\nよろしいですか？",
            TITLE_EVENT_ADD_POPUP, "イベントを追加します",
            TITLE_NOTHING_ADD_EVENT, "おや？",
            TXT_NOTHING_ADD_EVENT_INFO, "追加できるイベントがありません",
            TITLE_NOTING_FILE_POPUP, "エラー",
            TXT_NOTING_FILE_POPUP_INFO, "ファイルが存在しません"
    );

    public EventPreTable(AudioModification mod) {
        this._mod  = mod;
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"イベント"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        _externalEvents = Collections.unmodifiableList(mod.getEvents());

        _externalEvents.forEach(e -> {
            tableModel.addRow(new Object[]{e.shortName});
        });

        setModel(tableModel);
        getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(se -> { // 選択時のイベント
            if (se.getValueIsAdjusting()) {
                _selectionListener.forEach(l -> _getSelectedEvents().forEach(l));
            }
        });

        // ====右クリックメニュー====
        JPopupMenu eventPopup = new JPopupMenu();

        JMenuItem itemSelectFile = new JMenuItem(text.get(TXT_LINK_FILE_EVENT));
        itemSelectFile.addActionListener(l -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "音声ファイル", "wav", "ogg", "mp3", "wem", "m4a"));
            int result = chooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) return;
            var audios = Arrays.stream(chooser.getSelectedFiles())
                    .map(f -> new VMAudioFile(f, ignore -> {
                        JOptionPane.showMessageDialog(null,
                                text.get(TXT_NOTING_FILE_POPUP_INFO),
                                text.get(TITLE_NOTING_FILE_POPUP),
                                JOptionPane.ERROR_MESSAGE);
                    }))
                    .toList();
            var selected = _getSelectedEvents();
            selected.forEach(e ->
                    e.audios.addAll(audios));
            _audioOpeListener.forEach(al -> {
                al.accept(selected, audios);
            });
        });

        JMenuItem itemDelEvent = new JMenuItem(text.get(TXT_DEL_EVENT));
        itemDelEvent.addActionListener(l -> {
            int[] rows = EventPreTable.this.getSelectedRows();

            int ok = JOptionPane.showConfirmDialog(null,
                    text.get(TXT_EVENT_DEL_POPUP_INFO),
                    text.get(TITLE_EVENT_DEL_POPUP),
                    JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;

            List<String> visibleList = tableModel.getDataVector().stream()
                    .map(o -> o.get(0).toString())
                    .toList();
            for (int i : rows) {
                String name = visibleList.get(i);
                ExternalEvent del = _externalEvents.stream()
                        .filter(e -> Objects.equals(e.shortName, name))
                        .toList()
                        .get(0);
                deleteEvent(del);
                tableModel.removeRow(rows[0]);
            }

            repaint();
        });

        JMenuItem itemAddEvent = new JMenuItem(text.get(TXT_ADD_EVENT));
        itemAddEvent.addActionListener(l -> {
            if (_invisibleEvents.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        text.get(TXT_NOTHING_ADD_EVENT_INFO),
                        text.get(TITLE_NOTHING_ADD_EVENT),
                        JOptionPane.PLAIN_MESSAGE);
                return;
            }

            DefaultListModel<String> invisibles = new DefaultListModel<>();
            for (var str : _invisibleEvents)
                invisibles.addElement(str.shortName);

            JList<String> list = new JList<>(invisibles);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            JScrollPane sc = new JScrollPane(list);
            sc.setPreferredSize(new Dimension(300, 400));

            int result = JOptionPane.showConfirmDialog(null,
                    sc,
                    text.get(TITLE_EVENT_ADD_POPUP),
                    JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                int[] selectedIds = list.getSelectedIndices();
                ExternalEvent[] add = new ExternalEvent[selectedIds.length];
                for (int i = 0; i < selectedIds.length; i++) {
                    ExternalEvent e = _invisibleEvents.get(selectedIds[i]);
                    add[i] = e;
                    tableModel.addRow(new Object[]{e.shortName});
                }
                addEvents(List.of(add));
                repaint();
            }
        });

        eventPopup.add(itemSelectFile);
        eventPopup.addSeparator();
        eventPopup.add(itemAddEvent);
        eventPopup.add(itemDelEvent);
        // =====>>ここまで

        MouseAdapter showPopupMouseAdapter = new EventPreTableShowPopupMenuAdapter(eventPopup);
        addMouseListener(showPopupMouseAdapter);

        // component set

    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        if (row < 0) return null;
        String name = (String) getModel().getValueAt(row, 0);

        StringBuilder sb = new StringBuilder();
        for (var aItre = getByShortName(name).audios.iterator(); aItre.hasNext(); ) {
            VMAudioFile n = aItre.next();
            sb.append(n.audioName);
            if (aItre.hasNext())
                sb.append("\n");
        }
        return sb.toString();
    }

    private List<ExternalEvent> _getSelectedEvents() {
        return Arrays.stream(getSelectedRows())
                .mapToObj(r -> (getValueAt(r, 0)))
                .map(s -> getByShortName((String) s))
                .toList();
    }

    private ExternalEvent getByShortName(String name) {
        return _externalEvents.stream().filter(e -> Objects.equals(e.shortName, name)).toList().get(0);
    }

    public List<ExternalEvent> getShownEvents() {
        return _externalEvents.stream()
                .filter(e -> !_invisibleEvents.contains(e))
                .toList();
    }

    public void addEvents(Collection<ExternalEvent> events) {
        events.forEach(this::_addEvent);
        _invisibleEvents.removeAll(events);
    }

    public void addEvent(ExternalEvent event) {
        _addEvent(event);
    }

    private void _addEvent(ExternalEvent event) {
        _addListener.forEach(l -> {
            l.accept(event);
        });
    }

    public void deleteEvent(int idx) {
        ExternalEvent event = _deleteEvent(idx);
        _deleteEvent(event);
        _invisibleEvents.add(event);
    }

    public void deleteEvent(ExternalEvent event) {
        _deleteEvent(event);
        _invisibleEvents.add(event);
    }

    // dont call _externalEvents.remove() in here.
    private ExternalEvent _deleteEvent(ExternalEvent d) {
        _deleteListener.forEach(l -> {
            l.accept(d);
        });
        d.audios.clear();
        return d;
    }

    // don't call _externalEvents.remove() in here.
    private ExternalEvent _deleteEvent(int idx) {
        var visibleEvents = _externalEvents.stream()
                .filter(e -> !_invisibleEvents.contains(e))
                .toList();
        return _deleteEvent(visibleEvents.get(idx));
    }

    public void addSelectionListener(Consumer<@NotNull ExternalEvent> e) {
        _selectionListener.add(e);
    }

    public void addDeleteListener(Consumer<ExternalEvent> e) {
        _deleteListener.add(e);
    }

    public void addAddListener(Consumer<ExternalEvent> e) {
        _addListener.add(e);
    }

    public void addAudioOpeListener(BiConsumer<List<ExternalEvent>, List<VMAudioFile>> e) {
        _audioOpeListener.add(e);
    }

    public void removeSelectionListener(Consumer<ExternalEvent> e) {
        _selectionListener.remove(e);
    }

    public void removeDeleateListener(Consumer<ExternalEvent> e) {
        _deleteListener.remove(e);
    }

    public void removeAddListener(Consumer<ExternalEvent> e) {
        _addListener.remove(e);
    }

    public void removeAudioOpeListener(Consumer<VMAudioFile> e) {
        _audioOpeListener.remove(e);
    }

    public final class EventPreTableShowPopupMenuAdapter extends MouseAdapter {
        private final JPopupMenu eventPopup;

        EventPreTableShowPopupMenuAdapter(JPopupMenu popupMenu) {
            this.eventPopup = popupMenu;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (!e.isPopupTrigger()) return;
            if (row >= 0 && !isRowSelected(row)) {
                setRowSelectionInterval(row, row);
            }
            eventPopup.show(EventPreTable.this, e.getX(), e.getY());
        }
    }
}
