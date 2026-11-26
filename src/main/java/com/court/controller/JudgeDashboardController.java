package com.court.controller;

import com.court.model.Judge;
import com.court.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Judge's dashboard. Home screen shows the judge's courtroom and designation.
 */
public class JudgeDashboardController extends DashboardController {

    @FXML private Label courtroomLabel;
    @FXML private Label designationLabel;

    @Override
    protected void onDashboardReady(User user) {
        Judge judge = (Judge) user;   // safe: only Judge objects open this dashboard
        courtroomLabel.setText(judge.hasCourtroom() ? "Room " + judge.getCourtroomName() : "Not assigned");
        designationLabel.setText(judge.getDesignation());
    }
}
