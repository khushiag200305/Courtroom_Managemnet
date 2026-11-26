package com.court.ui;

import com.court.model.CaseDocument;
import com.court.model.CaseStatus;
import com.court.model.CourtCase;
import com.court.model.CourtOrder;
import com.court.model.Hearing;
import com.court.model.HearingStatus;
import com.court.model.Party;
import com.court.service.CaseService;
import com.court.service.DocumentService;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The case file: overview, parties, hearings, orders and documents in one window (TabPane).
 * Used by all three roles.
 */
public final class CaseDetailsDialog {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private CaseDetailsDialog() {
    }

    /** Loads the case in the background, then opens the window. */
    public static void show(int caseId) {
        CaseService service = new CaseService();
        BackgroundTask.run(() -> service.details(caseId), CaseDetailsDialog::open);
    }

    private static void open(CaseService.CaseDetails details) {
        CourtCase c = details.courtCase();

        Label number = new Label(c.displayNumber());
        number.getStyleClass().add("case-number");
        HBox chips = new HBox(8, number, UiFactory.statusChip(c.getStatus()));
        if (c.isUrgent()) {
            chips.getChildren().add(UiFactory.chip("Urgent", "chip-red"));
        }
        Label title = new Label(c.getTitle());
        title.getStyleClass().add("dialog-title");
        title.setWrapText(true);
        VBox header = new VBox(6, chips, title);

        TabPane tabs = new TabPane(
                tab("Overview", overview(c)),
                tab("Hearings (" + details.hearings().size() + ")", hearings(details)),
                tab("Orders (" + details.orders().size() + ")", orders(details)),
                tab("Documents (" + details.documents().size() + ")", documents(details)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefSize(720, 440);

        VBox content = new VBox(14, header, tabs);
        content.setPadding(new Insets(6));

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Case file - " + c.displayNumber());
        dialog.initOwner(SceneManager.getStage());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStylesheets().add(SceneManager.css());
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private static Tab tab(String name, Node content) {
        return new Tab(name, content);
    }

    // ------------------------------------------------------------------ overview

    private static Node overview(CourtCase c) {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(9);
        int r = 0;
        r = row(grid, r, "Case type", c.getType().getDisplayName());
        r = row(grid, r, "Filed by", c.getFiledByName());
        r = row(grid, r, "Filed on", date(c.getFilingDate()));
        r = row(grid, r, "Registered on", date(c.getRegistrationDate()));
        r = row(grid, r, "Judge", c.getJudgeName() == null ? "Not assigned yet" : c.getJudgeName());
        r = row(grid, r, "Courtroom", c.getRoomNo() == null ? "-" : "Room " + c.getRoomNo());
        r = row(grid, r, "Next hearing", c.getNextHearingDate() == null ? "Not scheduled"
                : date(c.getNextHearingDate()) + "  (" + UiFactory.relativeDays(c.getNextHearingDate()) + ")");
        if (c.getDisposalDate() != null) {
            row(grid, r, "Decided on", date(c.getDisposalDate()));
        }

        VBox parties = new VBox();
        for (Party p : c.getParties()) {
            String sub = (p.address() == null ? "" : p.address()) + (p.phone() == null ? "" : "  ·  " + p.phone());
            parties.getChildren().add(UiFactory.listRow(UiFactory.chip(p.type().getDisplayName(), "chip-neutral"),
                    p.name(), sub.isBlank() ? null : sub, null));
        }

        VBox box = new VBox(16, grid, section("Parties", parties), section("Case details", text(c.getDescription())));
        if (c.getStatus() == CaseStatus.DEFECTIVE && c.getDefectReason() != null) {
            Label defect = new Label("Returned by the Registrar: " + c.getDefectReason());
            defect.getStyleClass().add("error-label");
            defect.setWrapText(true);
            box.getChildren().add(1, defect);
        }
        if (c.getJudgment() != null) {
            box.getChildren().add(section("Judgment", text(c.getJudgment())));
        }
        box.setPadding(new Insets(14, 6, 14, 6));
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private static int row(GridPane grid, int r, String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("result-key");
        Label v = new Label(value == null ? "-" : value);
        v.getStyleClass().add("result-value");
        grid.add(k, 0, r);
        grid.add(v, 1, r);
        return r + 1;
    }

    private static VBox section(String title, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        return new VBox(6, heading, content);
    }

    private static Label text(String value) {
        Label label = new Label(value == null ? "-" : value);
        label.setWrapText(true);
        label.getStyleClass().add("body-text");
        return label;
    }

    // ------------------------------------------------------------------ hearings

    private static Node hearings(CaseService.CaseDetails details) {
        TableView<Hearing> table = new TableView<>();
        TableColumn<Hearing, String> dateCol = new TableColumn<>("Date");
        TableColumn<Hearing, String> timeCol = new TableColumn<>("Time");
        TableColumn<Hearing, String> roomCol = new TableColumn<>("Room");
        TableColumn<Hearing, String> judgeCol = new TableColumn<>("Judge");
        TableColumn<Hearing, String> statusCol = new TableColumn<>("Status");
        UiFactory.textColumn(dateCol, h -> h.date().format(DATE));
        UiFactory.textColumn(timeCol, h -> h.time().format(TIME));
        UiFactory.textColumn(roomCol, h -> "Room " + h.roomNo());
        UiFactory.textColumn(judgeCol, Hearing::judgeName);
        UiFactory.chipColumn(statusCol, h -> h.status().getDisplayName(), h -> switch (h.status()) {
            case SCHEDULED -> "chip-gold";
            case COMPLETED -> "chip-green";
            case CANCELLED -> "chip-neutral";
        });
        table.getColumns().addAll(java.util.List.of(dateCol, timeCol, roomCol, judgeCol, statusCol));
        UiFactory.pageTable(table, "No hearings yet.");
        table.getItems().setAll(details.hearings());
        VBox box = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    // ------------------------------------------------------------------ orders

    private static Node orders(CaseService.CaseDetails details) {
        ListView<CourtOrder> list = new ListView<>();
        list.getItems().setAll(details.orders());
        list.setPlaceholder(UiFactory.emptyState("fth-file-text", "No orders have been passed yet."));
        // Custom cells: date and outcome on top, the order text below
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CourtOrder o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) {
                    setGraphic(null);
                    return;
                }
                Label top = new Label(o.orderDate().format(DATE) + "  ·  " + o.judgeName());
                top.getStyleClass().add("row-subtitle");
                Label body = new Label(o.text());
                body.setWrapText(true);
                body.setMaxWidth(640);
                HBox head = new HBox(10, UiFactory.chip(o.outcome().getDisplayName(),
                        o.outcome().name().equals("DISPOSED") ? "chip-green" : "chip-gold"), top);
                VBox cell = new VBox(6, head, body);
                if (o.nextDate() != null) {
                    Label next = new Label("Next date: " + o.nextDate().format(DATE));
                    next.getStyleClass().add("row-meta");
                    cell.getChildren().add(next);
                }
                setGraphic(cell);
            }
        });
        VBox box = new VBox(list);
        VBox.setVgrow(list, Priority.ALWAYS);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    // ------------------------------------------------------------------ documents

    private static Node documents(CaseService.CaseDetails details) {
        DocumentService documentService = new DocumentService();
        VBox list = new VBox();
        for (CaseDocument d : details.documents()) {
            Path file = documentService.locate(d);
            Node action;
            if (file != null) {
                Button open = UiFactory.button("Open", "fth-external-link", "secondary-button",
                        () -> SceneManager.openFile(file));
                action = open;
            } else {
                action = UiFactory.chip("File not on this computer", "chip-neutral");
            }
            String sub = d.readableSize() + "  ·  uploaded by " + d.uploadedByName()
                    + (d.uploadedAt() == null ? "" : " on " + d.uploadedAt().toLocalDate().format(DATE));
            list.getChildren().add(UiFactory.listRow(UiFactory.icon("fth-file", 18), d.fileName(), sub, action));
        }
        if (details.documents().isEmpty()) {
            list.getChildren().add(UiFactory.emptyState("fth-paperclip", "No documents attached."));
        }
        list.setPadding(new Insets(12, 4, 12, 4));
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private static String date(LocalDate d) {
        return d == null ? "-" : d.format(DATE);
    }

    /** Text for the hearing status column elsewhere. */
    public static String hearingStyle(HearingStatus status) {
        return switch (status) {
            case SCHEDULED -> "chip-gold";
            case COMPLETED -> "chip-green";
            case CANCELLED -> "chip-neutral";
        };
    }
}
