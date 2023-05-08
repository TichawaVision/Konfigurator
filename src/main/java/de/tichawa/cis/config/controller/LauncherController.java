package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.Tables;
import de.tichawa.cis.config.model.tables.*;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import de.tichawa.util.Tuple;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import org.jooq.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LauncherController {
    private static final String PRICE_EXPORT_FILENAME = "Priceexport.lnk";
    private static final String EXPORTED_PRICES_FILENAME = "Export/art.csv";

    protected CIS CIS_DATA;
    @FXML
    private ComboBox<String> cisComboBox;

    /**
     * handles the continue button press by opening the mask for the selected CIS
     *
     * @param a the button press action event
     */
    @FXML
    private void handleContinue(ActionEvent a) {
        Util.createNewStage(cisComboBox.getSelectionModel().getSelectedItem().toLowerCase() + "/Mask.fxml",
                        cisComboBox.getSelectionModel().getSelectedItem() + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"))
                .show();
    }


    /**
     * Starts the ferix export process and shows the user an alert to wait for it to finish so future methods have the exported file ready.
     *
     * @return whether the export is done (determined by the user pressing the OK-Button). Returns false if there was an error or the user did not press OK.
     */
    private boolean exportFerixPrices() {
        try {
            // export prices to  csv file (by opening the export file)
            Desktop.getDesktop().open(Launcher.ferixHome.resolve(PRICE_EXPORT_FILENAME).toFile());

            // show alert that user should wait for export to finish
            return new Alert(AlertType.CONFIRMATION,
                    "Please wait until FERIX has finished the export to " + Launcher.ferixHome.resolve(EXPORTED_PRICES_FILENAME) + ", then press OK to continue.",
                    ButtonType.OK, ButtonType.CANCEL)
                    // show alert and let user wait for export to finish
                    .showAndWait()
                    // if no button pressed, it is treated as cancel
                    .orElse(ButtonType.CANCEL)
                    // return whether ok button was pressed
                    == ButtonType.OK;
        } catch (IOException e) {
            Alert alert = new Alert(AlertType.ERROR, "Ferix export failed.");
            alert.showAndWait();
        }
        return false;
    }

    /**
     * handles the update prices button press.
     * Starts the price export from ferix and reads the generated csv file. Updates the database entries accordingly.
     * Counts the updated entries and shows the result to the user. Also shows a list of inactive items to the user.
     *
     * @param a the button press event
     */
    @FXML
    private void handleUpdate(ActionEvent a) {
        //TODO maybe we want to inform the user of the actual components (ferix keys) that changed)

        // update prices
        try {
            if (exportFerixPrices()) {
                int updateCount = updatePrices();

                // inform user of number of changes
                new Alert(Alert.AlertType.CONFIRMATION, "Update successful. " + updateCount + " entries updated.").showAndWait();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // delete unused inactive components
        try {
            //TODO ask user first or move this to another button?
            int deletedComponents = deleteUnusedInactiveComponents();

            // inform user of number of changes
            new Alert(Alert.AlertType.CONFIRMATION, "Update successful. " + deletedComponents + " components deleted.").showAndWait();
        } catch (IOException e) {

            e.printStackTrace();
        }

        // start the process of updating inactive items
        //TODO move this to another button?
        showInactiveComponents();
    }

    /**
     * Updates the prices in the price database table according to the new prices in the ferix export file.
     * Requires the ferix export to be finished so that the file can be read.
     *
     * @return the number of changed records
     * @throws IOException if an error occurred while reading the file or accessing the database
     */
    private static int updatePrices() throws IOException {
        // read csv to map: artNo -> (price, inactive)
        Map<Integer, Tuple<Double, Boolean>> sourcePrices = readFerixPrices();

        // get database connection
        DSLContext database = getDatabaseOrThrowException();

        // read current prices from database
        List<Double> oldPrices = database.selectFrom(Tables.PRICE)
                // only consider articles that are in ferix
                .where(Tables.PRICE.ART_NO.in(sourcePrices.keySet()))
                .orderBy(Tables.PRICE.ART_NO.asc())
                .fetch(Tables.PRICE.PRICE_);

        // batch update prices with new prices
        database.batchUpdate(sourcePrices.entrySet().stream().map(e -> {
            PriceRecord record = new PriceRecord();
            record.setArtNo(e.getKey());
            record.setPrice(e.getValue().getU());
            record.changed(Tables.PRICE.ART_NO, false);
            return record;
        }).collect(Collectors.toList())).execute();

        // select new prices to count changes
        List<Double> newPrices = database.selectFrom(Tables.PRICE)
                .where(Tables.PRICE.ART_NO.in(sourcePrices.keySet()))
                .orderBy(Tables.PRICE.ART_NO.asc())
                .fetch(Tables.PRICE.PRICE_);

        // count changes
        int updateCount = 0;
        for (int x = 0; x < newPrices.size(); x++) {
            if (!newPrices.get(x).equals(oldPrices.get(x))) {
                updateCount++;
            }
        }

        return updateCount;
    }

    /**
     * Determines the unused and inactive components. Calculates the inactive components via {@link #getInactiveComponents()} and filters the unused components.
     * Unused components are these that are in the price table but neither in the mechanics nor the electronics nor the equipment table.
     *
     * @return A list of all inactive components from the latest ferix export that are in the price table but not used.
     * @throws IOException if there is an error reading the export file or database
     */
    private static List<Integer> getUnusedInactiveComponents() throws IOException {
        // determine all inactive ferix components
        List<Integer> inactiveComponents = getInactiveComponents();

        // determine unused components from price table
        DSLContext database = getDatabaseOrThrowException();
        // - everything from price that is not
        Set<Integer> unusedComponents = database.selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.notIn(
                // - electronics table and not in
                database.select(Electronic.ELECTRONIC.ART_NO).from(Electronic.ELECTRONIC).union(
                        // - mechanics table and not in
                        database.select(Mechanic.MECHANIC.ART_NO).from(Mechanic.MECHANIC).union(
                                // - equipment table
                                database.select(Equipment.EQUIPMENT.ART_NO_FERIX_OLD).from(Equipment.EQUIPMENT)))
        )).fetchSet(Price.PRICE.ART_NO);

        // filter inactive ferix components by unused from price table in database
        return inactiveComponents.stream().filter(unusedComponents::contains).collect(Collectors.toList());
    }

    /**
     * Reads the ferix prices from the exported csv file and converts them to a handy map
     *
     * @return A map of the exported ferix prices that maps the article number to a tuple containing the price as first value and a boolean as second that determines whether the article is active.
     * @throws IOException if there was an error reading the file
     */
    private static Map<Integer, Tuple<Double, Boolean>> readFerixPrices() throws IOException {
        return Files.readAllLines(Launcher.ferixHome.resolve(EXPORTED_PRICES_FILENAME)).stream()
                .map(line -> line.split("\t"))
                // filter malformed lines (item at index 0 should be article number, at index 1 price (per items at index 2)
                .filter(line -> CIS.isInteger(line[0]) && CIS.isDouble(line[1]))
                // convert line to tuple <article number, <price, isActive>>
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), new Tuple<>(Double.parseDouble(line[1]) / CIS.decodeQuantity(line[2]), line.length < 4 || line[3].equals("A"))))
                // filter items by price > 0
                .filter(t -> t.getV().getU() > 0)
                // convert to map: article number -> <price, isActive>
                .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));
    }

    /**
     * Creates a list of articles in the database that the ferix export shows as inactive.
     *
     * @return The list containing the article numbers of the inactive articles in the ferix export
     * @throws IOException if there was an error reading from the database or the ferix export file
     */
    private static List<Integer> getInactiveComponents() throws IOException {
        // get database
        DSLContext database = getDatabaseOrThrowException();

        // select all components in database to compare to ferix export
        Set<Integer> components = database.selectFrom(Price.PRICE).fetchSet(Price.PRICE.ART_NO);

        // extract components that are in ferix and inactive
        return readFerixPrices().entrySet().stream()
                // filter only inactive components of ferix export
                .filter(e -> !e.getValue().getV())
                // map entry to only its article number
                .map(Map.Entry::getKey)
                // only show components that are also in the prices table
                .filter(components::contains)
                // and return them as list
                .collect(Collectors.toList());
    }

    /**
     * Deletes the unused inactive components from the price table.
     *
     * @return the number of components deleted
     * @throws IOException if an error occurs during the determination of unused inactive components or the deletion from the database
     */
    private static int deleteUnusedInactiveComponents() throws IOException {
        List<Integer> inactiveUnusedComponents = getUnusedInactiveComponents();
        DSLContext database = getDatabaseOrThrowException();
        return database.deleteFrom(Price.PRICE).where(Price.PRICE.ART_NO.in(inactiveUnusedComponents)).execute();
    }

    private void showInactiveComponents() {
        try {
            // determine inactive components
            List<Integer> inactiveComponentsArtNos = getInactiveComponents();

            // get database
            DSLContext database = getDatabaseOrThrowException();

            // inform user of inactive components
            //TODO replace with table with buttons to replace the current component with a new one (and automatically replace it in mechanics, electronics and equipment tables)
            if (!inactiveComponentsArtNos.isEmpty()) {
                Result<PriceRecord> result = database.selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.in(inactiveComponentsArtNos)).fetch();
                Alert inactiveAlert = new Alert(AlertType.WARNING);
                inactiveAlert.setHeaderText("Some components are inactive now. Please consider replacing the components with these IDs:");
                inactiveAlert.setContentText(result.stream().map(priceRecord -> priceRecord.getArtNo() + ", " + priceRecord.getFerixKey())
                        .collect(Collectors.joining("\n")));
                inactiveAlert.showAndWait();
            }
        } catch (IOException e) {
            e.printStackTrace();//TODO exception handling
        }
    }

    /**
     * Returns the database's DSL context or throws an error if it did not work
     */
    private static DSLContext getDatabaseOrThrowException() throws IOException {
        return Util.getDatabase().orElseThrow(() -> new IOException("unable to read database"));
    }
}
