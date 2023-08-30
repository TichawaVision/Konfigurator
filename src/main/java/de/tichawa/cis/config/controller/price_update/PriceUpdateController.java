package de.tichawa.cis.config.controller.price_update;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.Tables;
import de.tichawa.cis.config.model.tables.*;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import de.tichawa.util.Tuple;
import javafx.fxml.*;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jooq.DSLContext;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for PriceUpdate.fxml
 */
public class PriceUpdateController {
    private static final String PRICE_EXPORT_FILENAME = "Priceexport.lnk";
    private static final String EXPORTED_PRICES_FILENAME = "Export/art.csv";

    /**
     * Handles the export Ferix prices button press. Starts the Ferix export and shows the user an information to wait for it to finish.
     * If an error occurs, shows an error alert.
     */
    @FXML
    private void handleExportFerixPrices() {
        try {
            // export prices to csv file (by opening the export file)
            Desktop.getDesktop().open(Launcher.ferixHome.resolve(PRICE_EXPORT_FILENAME).toFile());

            // show information to user to wait
            (new Alert(Alert.AlertType.WARNING, "Please wait until the Ferix export to " + Launcher.ferixHome.resolve(EXPORTED_PRICES_FILENAME)
                    + " has finished before updating prices!")).showAndWait();
        } catch (IOException e) {
            (new Alert(Alert.AlertType.ERROR, "Ferix export failed: " + e)).showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Handles the update prices button press. Updates the prices in the database via {@link #updatePrices()} and shows the user and information on the number of updates.
     * If an error occurs, shows an error alert.
     */
    @FXML
    private void handleUpdatePrices() {
        try {
            // update prices in database's price table
            int numberOfUpdates = updatePrices();

            // show info to user
            (new Alert(Alert.AlertType.INFORMATION, numberOfUpdates + " prices updated.")).showAndWait();
        } catch (IOException e) {
            (new Alert(Alert.AlertType.ERROR, "Updating prices failed: " + e)).showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Handles the show inactive items button press.
     * Determines the inactive items via {@link #getInactiveItemsInDatabase()} and launches an overview table for the user to replace them.
     */
    @FXML
    private void handleShowInactiveItems() {
        try {
            Map<Integer, String> inactiveItems = getInactiveItemsInDatabase();

            // launch overview of inactive items
            // - create stage
            Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("price_update/PriceUpdateInactiveItems.fxml", "Inactive articles");
            // - pass the items to show to the view
            ((PriceUpdateInactiveItemsController) stageWithLoader.getValue().getController()).passData(inactiveItems);
            // - show stage
            stageWithLoader.getKey().showAndWait();
        } catch (IOException e) {
            (new Alert(Alert.AlertType.ERROR, "Error while gathering inactive articles: " + e)).showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * Handles the delete inactive unused items button press.
     * Determines the items to delete by calling {@link #getUnusedInactiveItems()} and launches an overview table for the user to confirm the deletion.
     */
    @FXML
    private void handleDeleteInactiveUnusedItems() {
        try {
            // get inactive unused items
            Map<Integer, String> inactiveUnusedItems = getUnusedInactiveItems();

            // launch overview of items to be deleted to be confirmed by the user
            // - create stage
            Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("price_update/PriceUpdateDeletionOverview.fxml", "Inactive unused items");
            // - pass the items to show to the view
            ((PriceUpdateDeletionOverviewController) stageWithLoader.getValue().getController()).passData(inactiveUnusedItems);
            // - show the stage
            stageWithLoader.getKey().showAndWait();
        } catch (IOException e) {
            (new Alert(Alert.AlertType.ERROR, "Error while gathering articles to delete: " + e)).showAndWait();
            e.printStackTrace();
        }
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
        java.util.List<Double> oldPrices = database.selectFrom(Tables.PRICE)
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
     * Determines the unused inactive components. Calculates the inactive components via {@link #getInactiveItemsInDatabase()} and filters the unused components.
     * Unused components are those that are in the price table but neither in the mechanics nor the electronics nor the equipment table.
     *
     * @return A map of all inactive components from the latest ferix export that are in the price table but not used. The map maps the article number to the corresponding ferix key
     * @throws IOException if there is an error reading the export file or database
     */
    private static Map<Integer, String> getUnusedInactiveItems() throws IOException {
        // determine all inactive ferix components
        Map<Integer, String> inactiveItemsInDatabase = getInactiveItemsInDatabase();

        // determine unused components from price table
        DSLContext database = getDatabaseOrThrowException();
        // - everything from price that
        return database.selectFrom(Price.PRICE)
                // - is inactive
                .where(Price.PRICE.ART_NO.in(inactiveItemsInDatabase.keySet())
                        // - and not in
                        .and(Price.PRICE.ART_NO.notIn(
                                        // - electronics table and not in
                                        database.select(Electronic.ELECTRONIC.ART_NO).from(Electronic.ELECTRONIC).union(
                                                // - mechanics table and not in
                                                database.select(Mechanic.MECHANIC.ART_NO).from(Mechanic.MECHANIC).union(
                                                        // - equipment table
                                                        database.select(Equipment.EQUIPMENT.ART_NO_FERIX_OLD).from(Equipment.EQUIPMENT))))
                                // convert result to map: article number -> ferix key
                        )).fetchMap(Price.PRICE.ART_NO, Price.PRICE.FERIX_KEY);
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
                .map(line -> new Tuple<>(Integer.parseInt(line[0]), new Tuple<>(Double.parseDouble(line[1]) / decodeQuantity(line[2]), line.length < 4 || line[3].equals("printError"))))
                // filter items by price > 0
                .filter(t -> t.getV().getU() > 0)
                // convert to map: article number -> <price, isActive>
                .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));
    }

    /**
     * Creates a map of articles in the database that the ferix export shows as inactive.
     *
     * @return A map that maps article numbers to the corresponding ferix key for articles in the database's price table that are inactive in the ferix export.
     * @throws IOException if there was an error reading from the database or the ferix export file
     */
    private static Map<Integer, String> getInactiveItemsInDatabase() throws IOException {
        // read inactive components from ferix export file
        List<Integer> inactiveArtNos = readFerixPrices().entrySet().stream()
                .filter(e -> !e.getValue().getV())
                .map(Map.Entry::getKey).collect(Collectors.toList());

        // select all components in database to compare to ferix export
        return getDatabaseOrThrowException().selectFrom(Price.PRICE)
                .where(Price.PRICE.ART_NO.in(inactiveArtNos))
                .fetchMap(Price.PRICE.ART_NO, Price.PRICE.FERIX_KEY);
    }

    /**
     * Returns the database's DSL context or throws an error if it did not work
     */
    public static DSLContext getDatabaseOrThrowException() throws IOException {
        return Util.getDatabase().orElseThrow(() -> new IOException("unable to read database"));
    }

    /**
     * Converts the ferix export quantity value to the corresponding multiplier
     *
     * @param ferixQuantityIdentifier the quantity identifier from the ferix export
     * @return the quantity multiplier that corresponds to the given identifier
     */
    private static double decodeQuantity(String ferixQuantityIdentifier) {
        switch (ferixQuantityIdentifier) {
            case "2": // per 1000
                return 1000;
            case "1": // per 100
                return 100;
            case "0": // per 1
            default:
                return 1;
        }
    }
}
