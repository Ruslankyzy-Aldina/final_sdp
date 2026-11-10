package org.example.ui;

import org.example.booking.Event;
import org.example.booking.EventBuilder;
import org.example.facade.EventFacade;
import org.example.validation.InputParser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.UncheckedIOException;
import java.util.Objects;

/** Event list screen: view, create (via EventBuilder), edit and delete events. */
final class EventListPanel extends JPanel {
    private final EventFacade facade;
    private final Runnable onEventsChanged;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Название", "Дата", "Место", "Вместимость", "Свободно", "Цена билета"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    final JTextField nameField = new JTextField();
    final JTextField dateField = new JTextField();
    final JTextField locationField = new JTextField();
    final JTextField capacityField = new JTextField();
    final JTextField priceField = new JTextField();

    EventListPanel(EventFacade facade, Runnable onEventsChanged) {
        super(new BorderLayout(0, 8));
        this.facade = Objects.requireNonNull(facade, "facade");
        this.onEventsChanged = Objects.requireNonNull(onEventsChanged, "onEventsChanged");
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                fillFormFromSelection();
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 5, 8, 4));
        form.add(new JLabel("Название"));
        form.add(new JLabel("Дата (ГГГГ-ММ-ДД)"));
        form.add(new JLabel("Место"));
        form.add(new JLabel("Вместимость"));
        form.add(new JLabel("Цена билета, KZT"));
        form.add(nameField);
        form.add(dateField);
        form.add(locationField);
        form.add(capacityField);
        form.add(priceField);

        JButton create = new JButton("Создать мероприятие");
        create.addActionListener(event -> runSafely(this::createEvent));
        JButton save = new JButton("Сохранить изменения");
        save.addActionListener(event -> runSafely(this::updateEvent));
        JButton delete = new JButton("Удалить мероприятие");
        delete.addActionListener(event -> runSafely(this::deleteEvent));
        JButton clear = new JButton("Очистить форму");
        clear.addActionListener(event -> clearForm());
        JPanel actions = new JPanel();
        actions.add(create);
        actions.add(save);
        actions.add(delete);
        actions.add(clear);

        JPanel south = new JPanel(new BorderLayout(0, 4));
        south.setBorder(BorderFactory.createTitledBorder("Мероприятие"));
        south.add(form, BorderLayout.CENTER);
        south.add(actions, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
        refresh();
    }

    void refresh() {
        String selected = selectedIdOrNull();
        tableModel.setRowCount(0);
        for (Event event : facade.events()) {
            tableModel.addRow(new Object[]{event.id(), event.name(),
                    event.date() == null ? "—" : event.date().toString(),
                    event.location().isEmpty() ? "—" : event.location(),
                    event.capacity(), facade.availableSeats(event.id()), event.unitPrice().toPlainString()});
        }
        if (selected != null) {
            selectById(selected);
        }
    }

    void createEvent() {
        Event created = facade.createEvent(builderFromForm());
        afterChange(created.id());
        clearForm();
    }

    void updateEvent() {
        String id = requireSelectedId();
        afterChange(facade.updateEvent(id, builderFromForm()).id());
    }

    void deleteEvent() {
        String id = requireSelectedId();
        int answer = JOptionPane.showConfirmDialog(this, "Удалить мероприятие «" + facade.findEvent(id).name() + "»?",
                "Удаление", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            facade.deleteEvent(id);
            afterChange(null);
            clearForm();
        }
    }

    /** Builder pattern in the GUI: each form field becomes one builder step. */
    private EventBuilder builderFromForm() {
        return new EventBuilder()
                .name(nameField.getText())
                .date(InputParser.parseDate(dateField.getText(), "Дата"))
                .location(locationField.getText())
                .capacity(InputParser.parseInt(capacityField.getText(), "Вместимость"))
                .price(InputParser.parseMoney(priceField.getText(), "Цена билета"));
    }

    private void afterChange(String selectId) {
        refresh();
        if (selectId != null) {
            selectById(selectId);
        }
        onEventsChanged.run();
    }

    private void fillFormFromSelection() {
        String id = selectedIdOrNull();
        if (id == null) {
            return;
        }
        Event event = facade.findEvent(id);
        nameField.setText(event.name());
        dateField.setText(event.date() == null ? "" : event.date().toString());
        locationField.setText(event.location());
        capacityField.setText(String.valueOf(event.capacity()));
        priceField.setText(event.unitPrice().toPlainString());
    }

    private void clearForm() {
        table.clearSelection();
        for (JTextField field : new JTextField[]{nameField, dateField, locationField, capacityField, priceField}) {
            field.setText("");
        }
    }

    void selectById(String id) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (id.equals(tableModel.getValueAt(row, 0))) {
                int viewRow = table.convertRowIndexToView(row);
                table.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    private String selectedIdOrNull() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : (String) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
    }

    private String requireSelectedId() {
        String id = selectedIdOrNull();
        if (id == null) {
            throw new IllegalStateException("Сначала выберите мероприятие в списке");
        }
        return id;
    }

    int rowCount() {
        return tableModel.getRowCount();
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException exception) {
            ErrorDialog.show(this, exception);
        }
    }
}
