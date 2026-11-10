package org.example.ui;

import org.example.booking.Booking;
import org.example.booking.BookingService;
import org.example.booking.BookingStatus;
import org.example.booking.Event;
import org.example.booking.UiRefreshListener;
import org.example.facade.EventFacade;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.UncheckedIOException;

public final class BookingFrame extends JFrame {
    private final EventFacade facade;
    private final SummaryPanel summaryPanel;
    private final PackagePanel packagePanel;
    private final EventListPanel eventListPanel;
    private final JComboBox<Event> eventBox;
    private final JComboBox<PricingOption> strategyBox = new JComboBox<>(PricingOption.values());
    private final JTextField customerField = new JTextField();
    private final JSpinner seatSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10_000, 1));
    private final JLabel priceLabel = new JLabel();
    private final JTextArea notificationArea = new JTextArea();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Событие", "Клиент", "Мест", "Статус", "Цена"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public BookingFrame(BookingService service) {
        super("Бронирования");
        this.facade = new EventFacade(service);
        summaryPanel = new SummaryPanel(facade);
        packagePanel = new PackagePanel(facade);
        eventListPanel = new EventListPanel(facade, this::onEventsChanged);
        eventBox = new JComboBox<>(facade.events().toArray(Event[]::new));
        eventBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                           int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Event event) {
                    setText(event.name() + " (свободно: " + facade.availableSeats(event.id()) + ")");
                }
                return this;
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 620);
        setLocationRelativeTo(null);
        buildLayout();
        wireActions();
        facade.subscribe(new UiRefreshListener(() -> {
            if (SwingUtilities.isEventDispatchThread()) {
                refreshView();
            } else {
                SwingUtilities.invokeLater(this::refreshView);
            }
        }));
        refreshView();
    }

    private void buildLayout() {
        JPanel form = new JPanel(new GridLayout(2, 4, 8, 6));
        form.add(new JLabel("Событие"));
        form.add(new JLabel("Клиент"));
        form.add(new JLabel("Мест"));
        form.add(new JLabel("Стратегия цены"));
        form.add(eventBox);
        form.add(customerField);
        form.add(seatSpinner);
        form.add(strategyBox);
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        header.add(form, BorderLayout.CENTER);
        header.add(priceLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        JPanel bookings = new JPanel(new BorderLayout());
        bookings.setBorder(BorderFactory.createTitledBorder("Брони"));
        bookings.add(new JScrollPane(table));
        notificationArea.setEditable(false);
        JPanel notifications = new JPanel(new BorderLayout());
        notifications.setBorder(BorderFactory.createTitledBorder("Уведомления"));
        notifications.add(new JScrollPane(notificationArea));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bookings, notifications);
        split.setResizeWeight(0.7);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Бронирования", split);
        tabs.addTab("Итог и экспорт", summaryPanel);
        tabs.addTab("Мероприятия", eventListPanel);
        tabs.addTab("Пакет услуг", packagePanel);
        add(tabs, BorderLayout.CENTER);

        JButton create = new JButton("Создать");
        create.addActionListener(event -> runSafely(this::createBooking));
        JButton confirm = new JButton("Подтвердить");
        confirm.addActionListener(event -> runSafely(() -> facade.changeStatus(selectedId(), BookingStatus.CONFIRMED)));
        JButton cancel = new JButton("Отменить");
        cancel.addActionListener(event -> runSafely(() -> facade.changeStatus(selectedId(), BookingStatus.CANCELLED)));
        JPanel actions = new JPanel();
        actions.add(create);
        actions.add(confirm);
        actions.add(cancel);
        add(actions, BorderLayout.SOUTH);
    }

    private void wireActions() {
        strategyBox.addActionListener(event -> {
            PricingOption option = (PricingOption) strategyBox.getSelectedItem();
            facade.setPricingStrategy(option.strategy(), option.toString());
            refreshView();
        });
        eventBox.addActionListener(event -> {
            updatePrice();
            refreshSummary();
        });
        seatSpinner.addChangeListener(event -> updatePrice());
        table.getSelectionModel().addListSelectionListener(event -> updatePrice());
    }

    private void createBooking() {
        Event event = (Event) eventBox.getSelectedItem();
        if (event == null) {
            throw new IllegalStateException("Нет доступных событий");
        }
        String customer = customerField.getText().trim();
        int seats = (int) seatSpinner.getValue();
        facade.addBooking(event.id(), customer, seats);
        customerField.setText("");
    }

    private void refreshView() {
        String selected = table.getSelectedRow() < 0 ? null : selectedId();
        tableModel.setRowCount(0);
        for (Booking booking : facade.bookings()) {
            Event event = facade.findEvent(booking.eventId());
            String price;
            try {
                price = facade.calculate(booking).toPlainString();
            } catch (IllegalArgumentException exception) {
                price = "—";
            }
            tableModel.addRow(new Object[]{booking.id(), event.name(), booking.customerName(),
                    booking.seatCount(), booking.status(), price});
        }
        if (selected != null) {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                if (selected.equals(tableModel.getValueAt(row, 0))) {
                    int viewRow = table.convertRowIndexToView(row);
                    table.setRowSelectionInterval(viewRow, viewRow);
                    break;
                }
            }
        }
        notificationArea.setText(String.join(System.lineSeparator(), facade.notifications()));
        notificationArea.setCaretPosition(notificationArea.getDocument().getLength());
        eventBox.repaint();
        eventListPanel.refresh();
        updatePrice();
        refreshSummary();
    }

    /** Keeps the booking/package event selectors in sync after the event list changed. */
    private void onEventsChanged() {
        Event selected = (Event) eventBox.getSelectedItem();
        eventBox.removeAllItems();
        for (Event event : facade.events()) {
            eventBox.addItem(event);
            if (selected != null && event.id().equals(selected.id())) {
                eventBox.setSelectedItem(event);
            }
        }
        packagePanel.reloadEvents();
        refreshView();
    }

    private void refreshSummary() {
        Event selected = (Event) eventBox.getSelectedItem();
        summaryPanel.refresh(selected == null ? null : selected.id());
    }

    private void updatePrice() {
        try {
            Booking booking;
            if (table.getSelectedRow() >= 0) {
                String id = selectedId();
                booking = facade.findBooking(id);
            } else {
                Event event = (Event) eventBox.getSelectedItem();
                if (event == null) {
                    priceLabel.setText("Цена: —");
                    return;
                }
                booking = new Booking("preview", event.id(), "preview", (int) seatSpinner.getValue(),
                        event.unitPrice(), BookingStatus.CREATED);
            }
            priceLabel.setText("Цена: " + facade.calculate(booking).toPlainString());
        } catch (IllegalArgumentException exception) {
            priceLabel.setText("Цена: " + exception.getMessage());
        }
    }

    private String selectedId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            throw new IllegalStateException("Сначала выберите бронь");
        }
        return (String) tableModel.getValueAt(table.convertRowIndexToModel(viewRow), 0);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | UncheckedIOException exception) {
            ErrorDialog.show(this, exception);
        }
    }
}
