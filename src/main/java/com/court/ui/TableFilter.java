package com.court.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Live search for a TableView: typing in the search box filters the rows straight away
 * (event listener on the text), and clicking a column header still sorts them.
 * <p>
 * Generic, so the same class works for tables of users, cases or hearings.
 *
 * @param <T> the row type
 */
public class TableFilter<T> {

    private final ObservableList<T> source = FXCollections.observableArrayList();
    private final FilteredList<T> filtered = new FilteredList<>(source, row -> true);
    private final Function<T, String> searchText;
    private String query = "";
    private Predicate<T> extra = row -> true;

    public TableFilter(TableView<T> table, TextField searchField, Function<T, String> searchText) {
        this.searchText = searchText;
        SortedList<T> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            query = newText == null ? "" : newText.trim().toLowerCase(Locale.ROOT);
            apply();
        });
    }

    public void setItems(Collection<T> rows) {
        source.setAll(rows);
    }

    /** An additional condition, e.g. "status is Pending" from a ComboBox. */
    public void setExtra(Predicate<T> condition) {
        this.extra = condition;
        apply();
    }

    /** Keeps a label like "12 of 44" up to date. */
    public void bindCount(Label label, String noun) {
        Runnable update = () -> label.setText(filtered.size() == source.size()
                ? source.size() + " " + noun
                : filtered.size() + " of " + source.size() + " " + noun);
        filtered.addListener((javafx.collections.ListChangeListener<T>) change -> update.run());
        source.addListener((javafx.collections.ListChangeListener<T>) change -> update.run());
        update.run();
    }

    private void apply() {
        filtered.setPredicate(row -> extra.test(row)
                && (query.isEmpty() || searchText.apply(row).toLowerCase(Locale.ROOT).contains(query)));
    }
}
