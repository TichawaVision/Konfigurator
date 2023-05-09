package de.tichawa.cis.config;

import javafx.scene.control.*;
import javafx.util.Callback;

import java.util.function.Function;

/**
 * class for buttons in a table cell
 *
 * @param <S> the type of the object that is displayed in the button's row
 */
public class ActionButtonTableCell<S> extends TableCell<S, Button> {

    private final Button actionButton;
    private final Function<S, Boolean> disabledFunction; // gets called on cell update and sets the button's disabled state
    private final Function<S, String> labelFunction; // gets called on cell update and sets the button's label text

    /**
     * Constructor for an action button table cell that applies the given function on button press.
     * Also sets the button's text according to the return value of the given second function that is called whenever the cell gets updated.
     *
     * @param function      the function that is called when the button is pressed
     * @param labelFunction the function that is called on update and sets the button text according to the return value. Won't set the text if the function is null
     */
    public ActionButtonTableCell(Function<S, S> function, Function<S, String> labelFunction) {
        this.actionButton = new Button();
        this.disabledFunction = null;
        this.actionButton.setOnAction(e -> function.apply(getCurrentItem()));
        this.actionButton.setMaxWidth(Double.MAX_VALUE);
        this.labelFunction = labelFunction;
    }

    /**
     * Constructor for an action button table cell that sets the given label text and applies the given function on button press.
     * Also sets the given disabled function to be called on update of the cell.
     *
     * @param label            the label text of the button
     * @param function         the function that is called when the button is pressed
     * @param disabledFunction the function that is called on update and sets the disabled state of the button. Won't set the state if the function is null
     */
    public ActionButtonTableCell(String label, Function<S, S> function, Function<S, Boolean> disabledFunction) {
        this.actionButton = new Button(label);
        this.actionButton.setOnAction(e -> function.apply(getCurrentItem()));
        this.actionButton.setMaxWidth(Double.MAX_VALUE);
        this.disabledFunction = disabledFunction;
        this.labelFunction = null;
    }

    /**
     * creates a callback usable for a cell factory for a {@link TableColumn} that creates a button with the given label that activates the given function
     *
     * @param label            the label of the created button
     * @param function         the function that activates on a button click
     * @param disabledFunction a function that is called on mouseover and passes its value to the {@link Button#setDisable(boolean)} method of the button
     * @param <S>              the type of the table cell value
     * @return a callback usable for a {@link TableColumn} cell factory
     */
    public static <S> Callback<TableColumn<S, Button>, TableCell<S, Button>> forTableColumn(String label, Function<S, S> function, Function<S, Boolean> disabledFunction) {
        return ignored -> new ActionButtonTableCell<>(label, function, disabledFunction);
    }

    /**
     * creates a callback usable for a cell factory for a {@link TableColumn} that creates a button with the given label that activates the given function
     *
     * @param label    the label of the created button
     * @param function the function that activates on a button click
     * @param <S>      the type of the table cell value
     * @return a callback usable for a {@link TableColumn} cell factory
     */
    public static <S> Callback<TableColumn<S, Button>, TableCell<S, Button>> forTableColumn(String label, Function<S, S> function) {
        return forTableColumn(label, function, ignored -> false);
    }

    /**
     * creates a callback usable for a cell factory for a {@link TableColumn} that creates a button that activates the given function when clicked.
     * The text of the button is determined by the given second function and set whenever the item gets updated.
     *
     * @param function      the function that activates on a button click
     * @param labelFunction the function for the button text that activates whenever the cell is updated
     * @param <S>           the type of the table cell value
     * @return a callback usable for a {@link TableColumn} cell factory
     */
    public static <S> Callback<TableColumn<S, Button>, TableCell<S, Button>> forTableColumn(Function<S, S> function, Function<S, String> labelFunction) {
        return ignored -> new ActionButtonTableCell<>(function, labelFunction);
    }

    /**
     * Returns the current item that is displayed in the row of this cell
     */
    public S getCurrentItem() {
        return getTableView().getItems().get(getIndex());
    }

    /**
     * Updates the given item by setting the graphic to this objects button.
     * Also sets the button text according to its return value if {@link #labelFunction} is not null.
     * Also sets the disabled state according to its return value if {@link #disabledFunction} is not null.
     *
     * @param item  the button cell object
     * @param empty whether the cell is empty
     */
    @Override
    public void updateItem(Button item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
        } else {
            setGraphic(actionButton);
            if (labelFunction != null)
                actionButton.setText(labelFunction.apply(getCurrentItem()));
            if (disabledFunction != null)
                actionButton.setDisable(disabledFunction.apply(getCurrentItem()));
        }
    }
}