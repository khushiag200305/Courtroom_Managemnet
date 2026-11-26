package com.court.ui;

import com.court.model.CourtOrder;
import com.court.model.OrderOutcome;
import com.court.util.UiFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * A searchable feed of court orders (newest first), shown to lawyers.
 * Double-click an order to open the case file.
 */
public final class OrdersView {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private OrdersView() {
    }

    public static Node build(String title, String subtitle, List<CourtOrder> orders) {
        FilteredList<CourtOrder> shown = new FilteredList<>(FXCollections.observableArrayList(orders), o -> true);
        ListView<CourtOrder> list = new ListView<>(shown);
        list.setPlaceholder(UiFactory.emptyState("fth-file-text", "No orders yet."));
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CourtOrder o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) {
                    setGraphic(null);
                    return;
                }
                Label number = new Label(o.caseNumber());
                number.getStyleClass().add("case-number");
                Label parties = new Label(o.caseTitle());
                parties.getStyleClass().add("row-title");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label date = new Label(o.orderDate().format(DATE));
                date.getStyleClass().add("row-meta");
                HBox head = new HBox(10, number, parties, spacer,
                        UiFactory.chip(o.outcome().getDisplayName(),
                                o.outcome() == OrderOutcome.DISPOSED ? "chip-green" : "chip-gold"), date);
                head.setAlignment(Pos.CENTER_LEFT);

                Label body = new Label(o.text());
                body.setWrapText(true);
                body.getStyleClass().add("body-text");
                body.maxWidthProperty().bind(lv.widthProperty().subtract(60));
                Label by = new Label(o.judgeName() + (o.nextDate() == null ? "" : "   ·   next date " + o.nextDate().format(DATE)));
                by.getStyleClass().add("row-subtitle");
                VBox cell = new VBox(6, head, body, by);
                cell.setPadding(new Insets(4, 0, 4, 0));
                setGraphic(cell);
            }
        });
        list.setOnMouseClicked(e -> {
            CourtOrder o = list.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && o != null) CaseDetailsDialog.show(o.caseId());
        });

        TextField search = new TextField();
        search.setPromptText("Search case number, parties or order text");
        HBox.setHgrow(search, Priority.ALWAYS);
        search.textProperty().addListener((obs, a, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
            shown.setPredicate(o -> q.isEmpty()
                    || (o.caseNumber() + " " + o.caseTitle() + " " + o.text()).toLowerCase(Locale.ROOT).contains(q));
        });
        HBox searchBox = new HBox(8, new FontIcon("fth-search"), search);
        searchBox.getStyleClass().add("input-group");
        searchBox.setMaxWidth(420);

        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("page-subtitle");
        VBox card = new VBox(12, searchBox, list);
        card.getStyleClass().add("card");
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        VBox page = new VBox(18, new VBox(3, heading, sub), card);
        page.getStyleClass().add("page");
        page.setPadding(new Insets(28, 32, 28, 32));
        return page;
    }
}
