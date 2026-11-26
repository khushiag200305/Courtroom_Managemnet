package com.court.util;

import com.court.model.CaseStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Small building blocks reused by every screen, so all lists, chips and tables look the same.
 */
public final class UiFactory {

    private static final double ROW_HEIGHT = 46;
    private static final double HEADER_HEIGHT = 38;

    private UiFactory() {
    }

    // ------------------------------------------------------------------ icons and chips

    /** A Feather icon, e.g. icon("fth-calendar", 16). Names: https://feathericons.com */
    public static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }

    public static Label chip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("chip", styleClass);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    public static Label statusChip(CaseStatus status) {
        return chip(status.getDisplayName(), status.getStyleClass());
    }

    public static Circle dot(String styleClass) {
        Circle dot = new Circle(4);
        dot.getStyleClass().addAll("dot", styleClass);
        return dot;
    }

    /** Circle with initials, e.g. "Justice Anil Mehta" -> "AM". */
    public static Label avatar(String name, String extraStyle) {
        Label avatar = new Label(initials(name));
        avatar.getStyleClass().add("avatar");
        if (extraStyle != null) {
            avatar.getStyleClass().add(extraStyle);
        }
        avatar.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return avatar;
    }

    public static String initials(String name) {
        // Ignore titles such as "Justice" and "Adv." so we get the person's initials
        List<String> words = Arrays.stream(name.trim().split("\\s+"))
                .filter(w -> !w.endsWith(".") && !w.equalsIgnoreCase("Justice"))
                .toList();
        if (words.isEmpty()) return "?";
        String first = words.get(0).substring(0, 1);
        String last = words.size() > 1 ? words.get(words.size() - 1).substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    // ------------------------------------------------------------------ tables

    /** Fills a text column from any property of the row, e.g. row -> row.caseNumber(). */
    public static <T> void textColumn(TableColumn<T, String> column, Function<T, String> getter) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(getter.apply(cell.getValue())));
    }

    /** A column that shows the case status as a coloured chip. */
    public static <T> void statusColumn(TableColumn<T, CaseStatus> column, Function<T, CaseStatus> getter) {
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(CaseStatus status, boolean empty) {
                super.updateItem(status, empty);
                setText(null);
                setGraphic(empty || status == null ? null : statusChip(status));
            }
        });
    }

    /** A column whose cells show a coloured chip, e.g. "Active" / "Inactive". */
    public static <T> void chipColumn(TableColumn<T, String> column, Function<T, String> text,
                                      Function<T, String> style) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                T row = getTableRow() == null ? null : getTableRow().getItem();
                setGraphic(empty || value == null || row == null ? null : chip(value, style.apply(row)));
            }
        });
    }

    /** A column of small icon buttons for each row (edit, delete, ...). */
    public static <T> void actionColumn(TableColumn<T, Void> column, Function<T, List<Button>> buttons) {
        column.setSortable(false);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                T row = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER_LEFT);
                box.getChildren().addAll(buttons.apply(row));
                setGraphic(box);
            }
        });
    }

    /** Small icon button with a tooltip, for table rows. */
    public static Button rowButton(String iconLiteral, String tooltip, Runnable action) {
        Button button = new Button();
        button.setGraphic(icon(iconLiteral, 15));
        button.getStyleClass().add("row-button");
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(e -> action.run());
        return button;
    }

    /** Button with text and icon, e.g. for page headers. */
    public static Button button(String text, String iconLiteral, String styleClass, Runnable action) {
        Button button = new Button(text);
        if (iconLiteral != null) {
            button.setGraphic(icon(iconLiteral, 15));
        }
        button.getStyleClass().add(styleClass);
        button.setOnAction(e -> action.run());
        return button;
    }

    /** Opens a row's details when it is double-clicked. */
    public static <T> void onDoubleClick(TableView<T> table, java.util.function.Consumer<T> action) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    action.accept(row.getItem());
                }
            });
            return row;
        });
    }

    /** Standard set-up for full-page tables: fills columns, friendly empty message. */
    public static void pageTable(TableView<?> table, String emptyMessage) {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(emptyState("fth-inbox", emptyMessage));
        table.setFixedCellSize(ROW_HEIGHT);
    }

    /**
     * Makes a table exactly as tall as its rows (up to maxRows), so there are no empty
     * striped rows, and shows a friendly message when it has no data.
     */
    public static void fitToRows(TableView<?> table, int maxRows, String emptyMessage) {
        table.setFixedCellSize(ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(emptyState("fth-inbox", emptyMessage));
        table.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
            int rows = table.getItems().size();
            int visible = rows == 0 ? 3 : Math.min(rows, maxRows);
            return HEADER_HEIGHT + visible * ROW_HEIGHT + 2;
        }, table.getItems()));
        table.minHeightProperty().bind(table.prefHeightProperty());
        table.maxHeightProperty().bind(table.prefHeightProperty());
    }

    // ------------------------------------------------------------------ lists

    /** A row used in side panels: [leading]  title / subtitle  [trailing]. */
    public static HBox listRow(Node leading, String title, String subtitle, Node trailing) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("row-title");
        VBox text = new VBox(2, titleLabel);
        if (subtitle != null) {
            Label sub = new Label(subtitle);
            sub.getStyleClass().add("row-subtitle");
            sub.setWrapText(true);
            text.getChildren().add(sub);
        }
        HBox.setHgrow(text, Priority.ALWAYS);
        text.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12);
        row.getStyleClass().add("list-row");
        row.setAlignment(Pos.CENTER_LEFT);
        if (leading != null) row.getChildren().add(leading);
        row.getChildren().add(text);
        if (trailing != null) row.getChildren().add(trailing);
        return row;
    }

    /** A labelled horizontal bar, used for simple charts like "cases by type". */
    public static HBox barRow(String label, int value, int max, String barStyle) {
        Label name = new Label(label);
        name.getStyleClass().add("bar-label");
        name.setMinWidth(78);

        ProgressBar bar = new ProgressBar(max == 0 ? 0 : (double) value / max);
        bar.getStyleClass().add(barStyle);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label count = new Label(String.valueOf(value));
        count.getStyleClass().add("bar-value");
        count.setMinWidth(28);
        count.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(12, name, bar, count);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static VBox emptyState(String iconLiteral, String message) {
        FontIcon icon = icon(iconLiteral, 22);
        icon.getStyleClass().add("empty-icon");
        Label text = new Label(message);
        text.getStyleClass().add("empty-text");
        text.setWrapText(true);
        VBox box = new VBox(8, icon, text);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
    }

    /** Shown for menu items whose screens are built in a later step. */
    public static VBox comingSoon(String title) {
        FontIcon icon = icon("fth-tool", 30);
        icon.getStyleClass().add("soon-icon");
        Label heading = new Label(title);
        heading.getStyleClass().add("page-title");
        Label text = new Label("This screen is being built in the next update of the project.");
        text.getStyleClass().add("page-subtitle");
        VBox card = new VBox(10, icon, heading, text);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().addAll("card", "soon-card");
        card.setMaxSize(460, 240);
        VBox page = new VBox(card);
        page.setAlignment(Pos.CENTER);
        page.getStyleClass().add("page");
        return page;
    }

    // ------------------------------------------------------------------ text helpers

    /** "today", "tomorrow", "in 3 days", "yesterday", "5 days ago". */
    public static String relativeDays(LocalDate date) {
        if (date == null) return "-";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (days == 0) return "today";
        if (days == 1) return "tomorrow";
        if (days == -1) return "yesterday";
        return days > 0 ? "in " + days + " days" : (-days) + " days ago";
    }
}
