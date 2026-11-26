package com.court.controller;

import com.court.exception.ValidationException;
import com.court.model.CaseDocument;
import com.court.model.CaseStatus;
import com.court.model.CourtCase;
import com.court.model.Judge;
import com.court.model.Party;
import com.court.service.CaseService;
import com.court.service.CourtroomService;
import com.court.service.DocumentService;
import com.court.service.HearingService;
import com.court.service.UserService;
import com.court.ui.FormDialog;
import com.court.ui.Notifier;
import com.court.ui.SlotPicker;
import com.court.ui.TableFilter;
import com.court.util.AlertUtil;
import com.court.util.BackgroundTask;
import com.court.util.SceneManager;
import com.court.util.UiFactory;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registrar's scrutiny queue (scrutiny.fxml): check a new filing, then register it
 * (case number, judge, first hearing) or return it to the lawyer as defective.
 */
public class ScrutinyController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    /** Usual reasons a filing is returned; ticking them fills the reason text. */
    private static final List<String> COMMON_DEFECTS = List.of(
            "Court fee not paid or short",
            "Vakalatnama (lawyer's authority) not attached",
            "Copies of documents relied upon are missing",
            "Address of a party is incomplete",
            "Petition is not signed or verified");

    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private TableView<CourtCase> table;
    @FXML private TableColumn<CourtCase, String> filedCol;
    @FXML private TableColumn<CourtCase, String> titleCol;
    @FXML private TableColumn<CourtCase, String> typeCol;
    @FXML private TableColumn<CourtCase, String> lawyerCol;
    @FXML private TableColumn<CourtCase, String> waitCol;
    @FXML private VBox emptyDetails;
    @FXML private VBox detailsBox;
    @FXML private HBox detailChips;
    @FXML private Label detailTitle;
    @FXML private Label detailMeta;
    @FXML private VBox detailBody;

    private final CaseService caseService = new CaseService();
    private final DocumentService documentService = new DocumentService();
    private final HearingService hearingService = new HearingService();
    private final UserService userService = new UserService();
    private final CourtroomService courtroomService = new CourtroomService();
    private TableFilter<CourtCase> filter;
    private CourtCase selected;

    /** The selected filing with its documents, loaded together. */
    private record Filing(CourtCase courtCase, List<CaseDocument> documents) {
    }

    @FXML
    private void initialize() {
        UiFactory.pageTable(table, "No new filings are waiting. All caught up.");
        UiFactory.textColumn(filedCol, c -> c.getFilingDate().format(DATE));
        UiFactory.textColumn(titleCol, c -> (c.isUrgent() ? "● " : "") + c.getTitle());
        UiFactory.textColumn(typeCol, c -> c.getType().getDisplayName());
        UiFactory.textColumn(lawyerCol, CourtCase::getFiledByName);
        UiFactory.chipColumn(waitCol, c -> c.isUrgent() ? "Urgent" : waiting(c),
                c -> c.isUrgent() ? "chip-red" : (c.daysSinceFiling() > 3 ? "chip-gold" : "chip-green"));

        filter = new TableFilter<>(table, searchField,
                c -> c.getTitle() + " " + c.getType() + " " + c.getFiledByName());
        filter.bindCount(countLabel, "waiting");

        // Listener: show the selected filing on the right
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, c) -> showDetails(c));
        load();
    }

    private void load() {
        selected = null;
        showDetails(null);
        BackgroundTask.run(() -> caseService.findByStatus(CaseStatus.FILED), cases -> filter.setItems(cases));
    }

    private static String waiting(CourtCase c) {
        long d = c.daysSinceFiling();
        return d == 0 ? "Today" : d == 1 ? "1 day" : d + " days";
    }

    // ------------------------------------------------------------------ details panel

    private void showDetails(CourtCase summary) {
        boolean has = summary != null;
        emptyDetails.setVisible(!has);
        detailsBox.setVisible(has);
        if (!has) return;

        BackgroundTask.run(() -> new Filing(
                caseService.findWithParties(summary.getId()).orElse(summary),
                documentService.findByCase(summary.getId())), data -> {
            CourtCase c = data.courtCase();
            List<CaseDocument> docs = data.documents();
            selected = c;

            detailChips.getChildren().setAll(UiFactory.chip(c.getType().getDisplayName(), "chip-neutral"));
            if (c.isUrgent()) detailChips.getChildren().add(UiFactory.chip("Urgent", "chip-red"));
            detailTitle.setText(c.getTitle());
            detailMeta.setText("Filed by " + c.getFiledByName() + " on " + c.getFilingDate().format(DATE));

            VBox parties = new VBox();
            for (Party p : c.getParties()) {
                parties.getChildren().add(UiFactory.listRow(UiFactory.chip(p.type().getDisplayName(), "chip-neutral"),
                        p.name(), p.address(), null));
            }
            VBox documents = new VBox();
            for (CaseDocument d : docs) {
                Path file = documentService.locate(d);
                Node action = file == null ? UiFactory.chip("Not on this computer", "chip-neutral")
                        : UiFactory.button("Open", "fth-external-link", "secondary-button", () -> SceneManager.openFile(file));
                documents.getChildren().add(UiFactory.listRow(UiFactory.icon("fth-file", 16), d.fileName(),
                        d.readableSize(), action));
            }
            if (docs.isEmpty()) {
                Label none = new Label("No documents attached - consider returning the filing.");
                none.getStyleClass().add("trend-bad");
                documents.getChildren().add(none);
            }
            Label description = new Label(c.getDescription());
            description.setWrapText(true);
            description.getStyleClass().add("body-text");

            detailBody.getChildren().setAll(section("Parties", parties), section("Case details", description),
                    section("Documents", documents));
        });
    }

    private VBox section(String title, Node content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("card-title");
        return new VBox(6, heading, content);
    }

    // ------------------------------------------------------------------ register

    @FXML
    private void handleRegister() {
        if (selected == null) return;
        CourtCase c = selected;
        List<Judge> judges = userService.assignableJudges();
        if (judges.isEmpty()) {
            AlertUtil.warning("No judge available", "Assign a judge to a courtroom on the Judges screen first.");
            return;
        }

        FormDialog form = new FormDialog("Register case",
                "A case number will be given, the case assigned to the judge, and the first hearing listed.",
                "Register case");
        Label title = form.add("Case", new Label(c.getTitle()));
        title.setWrapText(true);

        ComboBox<Judge> judgeBox = form.add("Judge", new ComboBox<>());
        judgeBox.getItems().setAll(judges);
        judgeBox.setMaxWidth(Double.MAX_VALUE);
        judgeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Judge j) {
                return j == null ? "" : "Room " + j.getCourtroomName() + "  ·  " + j.getName();
            }

            @Override
            public Judge fromString(String s) {
                return null;
            }
        });
        Map<Integer, String> roomNames = new HashMap<>();
        courtroomService.findAll().forEach(r -> roomNames.put(r.getId(), r.getName()));
        judgeBox.setValue(suggestJudge(c, judges, roomNames));

        SlotPicker slots = new SlotPicker(hearingService, LocalDate.now().plusDays(1));
        slots.addTo(form, "First hearing");
        slots.setParticipants(() -> judgeBox.getValue() == null ? null : new SlotPicker.Participants(
                judgeBox.getValue().getId(), judgeBox.getValue().getCourtroomId(), c.getFiledById()));
        judgeBox.valueProperty().addListener((obs, old, j) -> slots.refresh());

        String[] number = new String[1];
        if (form.showAndSave(() -> number[0] = caseService.register(c, judgeBox.getValue(), slots.date(), slots.time()))) {
            Notifier.success("Registered as " + number[0]);
            load();
        }
    }

    /** Picks the judge whose court name matches the case type, e.g. a family case goes to the Family Court. */
    private Judge suggestJudge(CourtCase c, List<Judge> judges, Map<Integer, String> roomNames) {
        String type = c.getType().getDisplayName().toLowerCase(Locale.ROOT);
        return judges.stream()
                .filter(j -> roomNames.getOrDefault(j.getCourtroomId(), "").toLowerCase(Locale.ROOT).contains(type))
                .findFirst()
                .orElse(judges.get(0));
    }

    // ------------------------------------------------------------------ return as defective

    @FXML
    private void handleReturn() {
        if (selected == null) return;
        CourtCase c = selected;
        FormDialog form = new FormDialog("Return for correction",
                "The lawyer sees the reason on their dashboard and can file the case again.", "Return filing");
        List<CheckBox> boxes = new ArrayList<>();
        VBox checks = new VBox(8);
        for (String defect : COMMON_DEFECTS) {
            CheckBox box = new CheckBox(defect);
            boxes.add(box);
            checks.getChildren().add(box);
        }
        form.add("Defects", checks);
        TextArea remarks = form.add("Other remarks", new TextArea());
        remarks.setPromptText("Anything else the lawyer must fix");
        remarks.setPrefRowCount(3);
        remarks.setWrapText(true);

        if (form.showAndSave(() -> {
            List<String> reasons = new ArrayList<>();
            boxes.stream().filter(CheckBox::isSelected).map(CheckBox::getText).forEach(reasons::add);
            if (!remarks.getText().isBlank()) reasons.add(remarks.getText().trim());
            if (reasons.isEmpty()) {
                throw new ValidationException("Tick at least one defect or write a remark.");
            }
            caseService.returnAsDefective(c, String.join("; ", reasons) + ".");
        })) {
            Notifier.success("Filing returned to " + c.getFiledByName());
            load();
        }
    }
}
