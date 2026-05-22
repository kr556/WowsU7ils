package org.w7ls.vmeditor;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static org.w7ls.vmeditor.LinkedFilePreTable.TxtKey.*;

public class LinkedFilePreTable extends JTable {
    private final AudioModification _mod;
    private final List<Consumer<VMAudioFile>> _selectedListener = new ArrayList<>();
    private final List<Consumer<VMAudioFile>> _addListener = new ArrayList<>();
    private final List<Consumer<VMAudioFile>> _removeListener = new ArrayList<>();
    private final List<Consumer<VMAudioFile>> _setListener = new ArrayList<>();
    private List<VMAudioFile> _elements = new ArrayList<>();
    private final DefaultTableModel _model;

    public enum TxtKey {
        TITLE,
        // 右クリックイベントのテキスト
        TXT_DEL_AUDIO,
    }

    private static final Map<TxtKey, String> text = Map.of(
            TITLE, "リンク済みファイル",
            TXT_DEL_AUDIO, "リンクを解除"
    );

    public LinkedFilePreTable(AudioModification mod) {
        this._mod = mod;
        _model = new UnsupportedVectorModel(new Object[]{text.get(TITLE)}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        setModel(_model);

        _model.addTableModelListener(l -> {
            var modelVec = ((UnsupportedVectorModel) _model)._getDataVector();
            switch (l.getType()) {
                case TableModelEvent.INSERT -> {
                    for (int i = l.getFirstRow(); i <= l.getLastRow(); i++) {
                        _elements.add((VMAudioFile) modelVec.get(i).get(0));
                    }
                }
                case TableModelEvent.DELETE -> {
                    if (l.getLastRow() >= l.getFirstRow()) {
                        _elements.subList(l.getFirstRow(), l.getLastRow() + 1).clear();
                    }
                }
                case TableModelEvent.UPDATE -> {
                    for (int i = l.getFirstRow(); i <= l.getLastRow(); i++) {
                        _elements.set(i, (VMAudioFile) modelVec.get(i).get(0));
                    }
                }
            }
        });

        // ====右クリックメニュー====
        JPopupMenu eventPopup = new JPopupMenu();

        JMenuItem itemDelAudio = new JMenuItem(text.get(TXT_DEL_AUDIO));
        itemDelAudio.addActionListener(l -> {
            int[] rows = getSelectedRows();

            for (int r : rows) {
                VMAudioFile audioFile = (VMAudioFile) getModel().getValueAt(r, 0);
                removeAudio(audioFile);
            }
        });

        eventPopup.add(itemDelAudio);
        // =====>>ここまで

        addMouseListener(new LinkedFilePreTableShowPopupMenuAdapter(eventPopup));

        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(sl -> {
            if (sl.getValueIsAdjusting()) {
                VMAudioFile selection = _elements.get(getSelectedRow());
                _selectedListener.forEach(l -> l.accept(selection));
            }
        });
    }

    public void setAudios(@Nullable List<VMAudioFile> linked) {
        for (int i = _model.getRowCount() - 1; i >= 0; i--) {
            _model.removeRow(i);
        }

        if (linked == null || linked.isEmpty()) return;

        for (var l : linked)
            _model.addRow(new Object[]{l});

        _setListener.forEach(linked::forEach);

        repaint();
    }

    public void update(AudioModification.ExternalEvent show) {
        setAudios(show.audios);
    }

    public void removeAudio(VMAudioFile linked) {
        _model.removeRow(_elements.indexOf(linked));
        _removeListener.forEach(l -> l.accept(linked));
        repaint();
    }

    public void addAudio(VMAudioFile linked) {
        _model.addRow(new Object[]{linked});
        _addListener.forEach(l -> l.accept(linked));
        repaint();
    }

    public void addAudio(Collection<VMAudioFile> linked) {
        linked.forEach(this::addAudio);
    }

// listener operator (
    public void addSelectedListener(Consumer<VMAudioFile> l) {
        _selectedListener.add(l);
    }

    public void removeSelectedListener(Consumer<VMAudioFile> l) {
        _selectedListener.remove(l);
    }

    public void addAddListener(Consumer<VMAudioFile> l) {
        _addListener.add(l);
    }

    public void removeAddListener(Consumer<VMAudioFile> l) {
        _addListener.remove(l);
    }

    public void addRemoveListener(Consumer<VMAudioFile> l) {
        _removeListener.add(l);
    }

    public void removeRemoveListener(Consumer<VMAudioFile> l) {
        _removeListener.remove(l);
    }

    public void addSetListener(Consumer<VMAudioFile> l) {
        _setListener.add(l);
    }

    public void removeSetListener(Consumer<VMAudioFile> l) {
        _setListener.remove(l);
    }
// )

    // Vectorを直接操作することで_elementsがおかしくなるのを防ぐためのもの
    @SuppressWarnings("rawtypes")
    private static class UnsupportedVectorModel extends DefaultTableModel{
        public UnsupportedVectorModel(Object[] o, int i) {
            super(o, i);
        }

        @Deprecated
        @Override
        public Vector<Vector> getDataVector() {
            throw new UnsupportedOperationException();
        }

        private Vector<Vector> _getDataVector() {
            return super.getDataVector();
        }
    }

    public final class LinkedFilePreTableShowPopupMenuAdapter extends MouseAdapter {
        private final JPopupMenu eventPopup;

        LinkedFilePreTableShowPopupMenuAdapter(JPopupMenu popupMenu) {
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
            eventPopup.show(LinkedFilePreTable.this, e.getX(), e.getY());
        }
    }
}
