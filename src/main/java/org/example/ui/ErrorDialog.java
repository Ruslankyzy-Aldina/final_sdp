package org.example.ui;

import javax.swing.JOptionPane;
import java.awt.Component;

final class ErrorDialog {
    private ErrorDialog() { }

    static void show(Component parent, Throwable error) {
        String message = error.getMessage();
        JOptionPane.showMessageDialog(parent,
                message == null || message.isBlank() ? error.getClass().getSimpleName() : message,
                "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
