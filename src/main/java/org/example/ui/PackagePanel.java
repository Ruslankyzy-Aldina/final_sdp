package org.example.ui;

import org.example.booking.Event;
import org.example.facade.EventFacade;
import org.example.servicepackage.CateringDecorator;
import org.example.servicepackage.DecorDecorator;
import org.example.servicepackage.PhotographerDecorator;
import org.example.servicepackage.ServicePackage;
import org.example.validation.InputParser;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.Optional;

/** Package configuration screen: base package plus optional extras (Decorator pattern). */
final class PackagePanel extends JPanel {
    private final EventFacade facade;
    final JComboBox<Event> eventBox = new JComboBox<>();
    final JTextField baseCostField = new JTextField("100000.00");
    final JCheckBox cateringBox = new JCheckBox("Кейтеринг (+" + CateringDecorator.DEFAULT_COST.toPlainString() + " KZT)");
    final JCheckBox photographerBox = new JCheckBox("Фотограф (+" + PhotographerDecorator.DEFAULT_COST.toPlainString() + " KZT)");
    final JCheckBox decorBox = new JCheckBox("Декор (+" + DecorDecorator.DEFAULT_COST.toPlainString() + " KZT)");
    final JLabel descriptionLabel = new JLabel("Состав: —");
    final JLabel costLabel = new JLabel("Итого: —");
    final JLabel savedLabel = new JLabel(" ");

    PackagePanel(EventFacade facade) {
        super(new BorderLayout(0, 8));
        this.facade = Objects.requireNonNull(facade, "facade");
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        eventBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof Event event) {
                    setText(event.name() + (event.date() == null ? "" : " — " + event.date()));
                }
                return this;
            }
        });

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Мероприятие"));
        form.add(eventBox);
        form.add(new JLabel("Базовый пакет, KZT"));
        form.add(baseCostField);
        form.add(new JLabel("Дополнительные услуги"));
        form.add(cateringBox);
        form.add(new JLabel());
        form.add(photographerBox);
        form.add(new JLabel());
        form.add(decorBox);
        add(form, BorderLayout.NORTH);

        JPanel result = new JPanel(new GridLayout(0, 1, 0, 4));
        result.setBorder(BorderFactory.createTitledBorder("Пакет"));
        result.add(descriptionLabel);
        result.add(costLabel);
        result.add(savedLabel);
        add(result, BorderLayout.CENTER);

        JButton save = new JButton("Сохранить пакет");
        save.addActionListener(event -> {
            try {
                savePackage();
            } catch (IllegalArgumentException | IllegalStateException exception) {
                ErrorDialog.show(this, exception);
            }
        });
        JPanel actions = new JPanel();
        actions.add(save);
        add(actions, BorderLayout.SOUTH);

        eventBox.addActionListener(event -> loadSavedPackage());
        cateringBox.addActionListener(event -> updatePreview());
        photographerBox.addActionListener(event -> updatePreview());
        decorBox.addActionListener(event -> updatePreview());
        baseCostField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
        });
        reloadEvents();
    }

    /** Re-reads the event list after events were created, edited or deleted. */
    void reloadEvents() {
        Event selected = (Event) eventBox.getSelectedItem();
        eventBox.removeAllItems();
        for (Event event : facade.events()) {
            eventBox.addItem(event);
        }
        if (selected != null) {
            for (int i = 0; i < eventBox.getItemCount(); i++) {
                if (eventBox.getItemAt(i).id().equals(selected.id())) {
                    eventBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        loadSavedPackage();
    }

    ServicePackage savePackage() {
        Event event = (Event) eventBox.getSelectedItem();
        ServicePackage servicePackage = facade.configurePackage(event == null ? null : event.id(),
                InputParser.parseMoney(baseCostField.getText(), "Стоимость базового пакета"),
                cateringBox.isSelected(), photographerBox.isSelected(), decorBox.isSelected());
        show(servicePackage);
        savedLabel.setText("Пакет сохранён для «" + event.name() + "»");
        return servicePackage;
    }

    private void loadSavedPackage() {
        Event event = (Event) eventBox.getSelectedItem();
        Optional<ServicePackage> saved = event == null ? Optional.empty() : facade.findPackage(event.id());
        if (saved.isPresent()) {
            ServicePackage servicePackage = saved.get();
            baseCostField.setText(servicePackage.getBaseCost().toPlainString());
            cateringBox.setSelected(servicePackage.catering());
            photographerBox.setSelected(servicePackage.photographer());
            decorBox.setSelected(servicePackage.decor());
            savedLabel.setText("Сохранённый пакет");
        } else {
            savedLabel.setText(event == null ? "Нет мероприятий" : "Пакет ещё не сохранён");
        }
        updatePreview();
    }

    /** Live preview; invalid input is reported in the label instead of a dialog while typing. */
    private void updatePreview() {
        try {
            ServicePackage preview = ServicePackage.configure("preview",
                    InputParser.parseMoney(baseCostField.getText(), "Стоимость базового пакета"),
                    cateringBox.isSelected(), photographerBox.isSelected(), decorBox.isSelected());
            show(preview);
        } catch (IllegalArgumentException exception) {
            descriptionLabel.setText("Состав: —");
            costLabel.setText("Ошибка: " + exception.getMessage());
        }
    }

    private void show(ServicePackage servicePackage) {
        descriptionLabel.setText("Состав: " + servicePackage.getDescription());
        costLabel.setText("Итого: " + servicePackage.getCost().toPlainString() + " KZT");
    }
}
