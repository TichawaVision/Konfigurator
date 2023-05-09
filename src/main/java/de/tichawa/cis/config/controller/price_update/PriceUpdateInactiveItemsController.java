package de.tichawa.cis.config.controller.price_update;

import de.tichawa.cis.config.*;
import de.tichawa.cis.config.model.tables.*;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.jooq.DSLContext;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for PriceUpdateInactiveItems.fxml
 */
public class PriceUpdateInactiveItemsController implements Initializable {
    @FXML
    private TableView<Pair<Integer, String>> overviewTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // setup table
        overviewTable.getColumns().clear();
        TableColumn<Pair<Integer, String>, Integer> articleNumberColumn = new TableColumn<>("Article number");
        articleNumberColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getKey()));
        TableColumn<Pair<Integer, String>, String> ferixKeyColumn = new TableColumn<>("Ferix key");
        ferixKeyColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getValue()));
        TableColumn<Pair<Integer, String>, Button> replaceArticleColumn = new TableColumn<>();
        replaceArticleColumn.setCellFactory(ActionButtonTableCell.forTableColumn("Replace article", pair -> {
            handleReplaceArticle(pair);
            return pair;
        }));
        overviewTable.getColumns().add(articleNumberColumn);
        overviewTable.getColumns().add(ferixKeyColumn);
        overviewTable.getColumns().add(replaceArticleColumn);
    }

    public void passData(Map<Integer, String> items) {
        overviewTable.getItems().clear();
        overviewTable.getItems().addAll(items.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

    /**
     * Handles the replace article button press.
     * Lets the user input the new article number first.
     * Checks whether the new article is already in the database.
     * If it is, the article is replaced via {@link #replaceItem(int, int)}
     * If not, the user is asked for necessary inputs by launching the PriceUpdateNewItem.fxml for further handling
     *
     * @param article the old article to replace given as a pair of the article number and the ferix key
     */
    private void handleReplaceArticle(Pair<Integer, String> article) {
        // ask for article number first
        TextInputDialog articleNumberInputDialog = new TextInputDialog();
        articleNumberInputDialog.setTitle("New article number");
        articleNumberInputDialog.setHeaderText("Insert the new article number (FERIX alt)");
        articleNumberInputDialog.setContentText("Article number:");
        Optional<String> userInput = articleNumberInputDialog.showAndWait();

        // check if user gave an answer -> if not (cancel or X), just do nothing (return)
        if (!userInput.isPresent())
            return;

        // check if input is valid
        int newArticleNumber;
        try {
            newArticleNumber = Integer.parseInt(userInput.get());
        } catch (NumberFormatException e) {
            System.err.println("user input not a number");
            return;
        }

        // check if article number is already in database
        PriceRecord existingRecord;
        try {
            existingRecord = PriceUpdateController.getDatabaseOrThrowException().selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.eq(newArticleNumber)).fetchOne();
        } catch (IOException e) {
            System.err.println("failed to read from database");
            return;
        }

        if (existingRecord != null) {
            // if found: show info and ask if continue
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Already found the new article " + newArticleNumber + " in configurator database with ferix key: " + existingRecord.getFerixKey()
                    + "\nAre you sure you want to replace the old article " + article.getKey() + ", " + article.getValue() + "?");

            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == ButtonType.OK)
                    replaceItem(article.getKey(), newArticleNumber);
            });
            return;
        } // else (did not find the article in the database)

        // launch input mask for new article
        Pair<Stage, FXMLLoader> stageWithLoader = Util.createNewStageWithLoader("price_update/PriceUpdateNewItem.fxml", "Add new article data");
        ((PriceUpdateNewItemController) stageWithLoader.getValue().getController()).passData(article.getKey(), newArticleNumber);
        stageWithLoader.getKey().showAndWait();
        //TODO could maybe move the replaceItem call inside this back up here (and make it private) via calling a "I'm done" method of the parent (this)
        // would need to pass the reference to the parent via passData then
    }

    /**
     * Replaces the article number for all entries in the equipment, electronic and mechanics table.
     *
     * @param oldArticleNumber the article number to replace
     * @param newArticleNumber the new article number after replacing
     */
    public static void replaceItem(int oldArticleNumber, int newArticleNumber) {
        System.out.println("replacing " + oldArticleNumber + " with " + newArticleNumber);
        try {
            // update in database in all possible locations
            DSLContext database = PriceUpdateController.getDatabaseOrThrowException();

            // replace in equipment table (only ferix old)
            int equipmentNumber = database.update(Equipment.EQUIPMENT)
                    .set(Equipment.EQUIPMENT.ART_NO_FERIX_OLD, newArticleNumber)
                    .where(Equipment.EQUIPMENT.ART_NO_FERIX_OLD.eq(oldArticleNumber)).execute();
            // replace in electronics table
            int electronicNumber = database.update(Electronic.ELECTRONIC)
                    .set(Electronic.ELECTRONIC.ART_NO, newArticleNumber)
                    .where(Electronic.ELECTRONIC.ART_NO.eq(oldArticleNumber)).execute();
            // replace in mechanics table: as art_no
            int mechanicNumber = database.update(Mechanic.MECHANIC)
                    .set(Mechanic.MECHANIC.ART_NO, newArticleNumber)
                    .where(Mechanic.MECHANIC.ART_NO.eq(oldArticleNumber)).execute();
            // replace in mechanics table: as next_size_art_no
            int mechanicNextNumber = database.update(Mechanic.MECHANIC)
                    .set(Mechanic.MECHANIC.NEXT_SIZE_ART_NO, newArticleNumber)
                    .where(Mechanic.MECHANIC.NEXT_SIZE_ART_NO.eq(oldArticleNumber)).execute();

            // show alert to user of updated records
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Replaced article in database:");
            alert.setContentText("Updated\n"
                    + equipmentNumber + " entries in equipment table,\n"
                    + electronicNumber + " entries in electronic table,\n"
                    + mechanicNumber + " entries in mechanic table for art_no,\n"
                    + mechanicNextNumber + " entries in mechanic table for next_size_art_no.");
            alert.showAndWait();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to replace article in database: " + e);
            e.printStackTrace();
        }
    }

    //TODO option to print
}