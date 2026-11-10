package org.example.ui;

import org.example.export.TxtExporter;
import org.example.facade.EventFacade;
import org.example.model.Summary;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/** Added to the original booking window; it uses the same facade and selected event. */
public final class SummaryPanel extends JPanel {
    private final EventFacade facade;
    private final JTextArea preview = new JTextArea();
    private final JComboBox<String> formats = new JComboBox<>(new String[]{"TXT", "JSON", "HTML"});
    private final JButton export = new JButton("Экспорт / Export");
    private Summary summary;
    private boolean exporting;

    public SummaryPanel(EventFacade facade) {
        super(new BorderLayout(8, 8));
        this.facade = facade;
        preview.setEditable(false);
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);
        add(new JScrollPane(preview), BorderLayout.CENTER);
        JPanel actions = new JPanel();
        actions.add(new JLabel("Формат:"));
        actions.add(formats);
        actions.add(export);
        add(actions, BorderLayout.SOUTH);
        export.setEnabled(false);
        export.addActionListener(event -> chooseExport());
    }

    public void refresh(String eventId) {
        summary = null;
        try {
            if (eventId == null) {
                preview.setText("Выберите мероприятие.");
            } else {
                summary = facade.getSummary(eventId);
                preview.setText(TxtExporter.render(summary));
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            preview.setText("Итог недоступен: " + exception.getMessage()
                    + "\nВыберите подходящую стратегию цены.");
        }
        preview.setCaretPosition(0);
        export.setEnabled(summary != null && !exporting);
    }

    private void chooseExport() {
        Summary snapshot = summary;
        if (snapshot == null || exporting) return;
        String format = ((String) formats.getSelectedItem()).toLowerCase(Locale.ROOT);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Экспорт итога");
        chooser.setFileFilter(new FileNameExtensionFilter(format.toUpperCase(Locale.ROOT), format));
        chooser.setSelectedFile(new java.io.File("event-summary." + format));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path selected = chooser.getSelectedFile().toPath();
        if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + format)) {
            selected = selected.resolveSibling(selected.getFileName() + "." + format);
        }
        Path destination = selected;
        if (Files.exists(destination) && JOptionPane.showConfirmDialog(this,
                "Заменить файл " + destination + "?", "Экспорт", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        exporting = true;
        export.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                facade.exportSummary(snapshot, format, destination);
                return null;
            }

            @Override
            protected void done() {
                exporting = false;
                export.setEnabled(summary != null);
                try {
                    get();
                    JOptionPane.showMessageDialog(SummaryPanel.this,
                            "Сохранено: " + destination.toAbsolutePath(), "Экспорт", JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    ErrorDialog.show(SummaryPanel.this, exception);
                } catch (ExecutionException exception) {
                    ErrorDialog.show(SummaryPanel.this, exception.getCause());
                }
            }
        }.execute();
    }
}
