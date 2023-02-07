package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.Tables;
import de.tichawa.cis.config.model.tables.Price;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import de.tichawa.util.Tuple;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import org.jooq.Result;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LauncherController {
    protected CIS CIS_DATA;
    @FXML
    private ComboBox<String> selectCIS;

    /**
     * handles the continue button press by opening the mask for the selected CIS
     *
     * @param a the button press action event
     */
    @FXML
    private void handleContinue(ActionEvent a) {
        Util.createNewStage(
                        selectCIS.getSelectionModel().getSelectedItem().toLowerCase() + "/Mask.fxml",
                        selectCIS.getSelectionModel().getSelectedItem() + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"))
                .show();
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
        try {
            // export prices to  csv file
            Desktop.getDesktop().open(Launcher.ferixHome.resolve("Priceexport.lnk").toFile());

            if (new Alert(AlertType.CONFIRMATION, "Please wait until FERIX has finished the export to " + Launcher.ferixHome.resolve("Export/art.csv") + ", then press OK to continue.",
                    ButtonType.OK, ButtonType.CANCEL).showAndWait()
                    .orElse(ButtonType.CANCEL) == ButtonType.OK) {
                // read csv to map: artNo -> (price, inactive)
                Map<Integer, Tuple<Double, Boolean>> sourcePrices = Files.readAllLines(Launcher.ferixHome.resolve("Export/art.csv")).stream()
                        .map(line -> line.split("\t"))
                        .filter(line -> CIS.isInteger(line[0]) && CIS.isDouble(line[1]))
                        .map(line -> new Tuple<>(Integer.parseInt(line[0]), new Tuple<>(Double.parseDouble(line[1]) / CIS.decodeQuantity(line[2]), line.length < 4 || line[3].equals("A"))))
                        .filter(t -> t.getV().getU() > 0)
                        .collect(Collectors.toMap(Tuple::getU, Tuple::getV, (oldVal, newVal) -> newVal));

                // read current prices from database
                Util.getDatabase().ifPresent(database -> {
                    List<Double> oldPrices = database.selectFrom(Tables.PRICE)
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

                    // inform user of number of changes
                    new Alert(Alert.AlertType.CONFIRMATION, "Update successful. " + updateCount + " entries updated.")
                            .showAndWait();

                    // select all components to check for inactive
                    Set<Integer> components = database.selectFrom(Price.PRICE).fetchSet(Price.PRICE.ART_NO);

                    // determine inactive components
                    List<Integer> inactiveComponentsArtNos = sourcePrices.entrySet().stream()
                            .filter(e -> !e.getValue().getV())
                            .map(Map.Entry::getKey)
                            .filter(components::contains)
                            .collect(Collectors.toList());

                    // inform user of inactive components
                    if (!inactiveComponentsArtNos.isEmpty()) {
                        Result<PriceRecord> result = database.selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.in(inactiveComponentsArtNos)).fetch();
                        Alert inactiveAlert = new Alert(AlertType.WARNING);
                        inactiveAlert.setHeaderText("Some components are inactive now. Please consider replacing the components with these IDs:");
                        inactiveAlert.setContentText(result.stream().map(priceRecord -> priceRecord.getArtNo() + ", " + priceRecord.getFerixKey())
                                .collect(Collectors.joining("\n")));
                        inactiveAlert.showAndWait();
                    }
                });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
