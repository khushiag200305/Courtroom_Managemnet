package com.court.controller;

import com.court.model.Lawyer;
import com.court.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.Objects;

/**
 * Lawyer's dashboard. Home screen shows the lawyer's Bar ID and contact details.
 */
public class LawyerDashboardController extends DashboardController {

    @FXML private Label barIdLabel;
    @FXML private Label emailLabel;

    @Override
    protected void onDashboardReady(User user) {
        Lawyer lawyer = (Lawyer) user;
        barIdLabel.setText(lawyer.getBarId());
        emailLabel.setText(Objects.requireNonNullElse(lawyer.getEmail(), "-"));
    }
}
