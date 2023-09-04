package de.tichawa.cis.config.controller.price_update;

import de.tichawa.cis.config.model.tables.Price;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for PriceUpdateDeletionOverview.fxml
 */
public class PriceUpdateDeletionOverviewController implements Initializable {
    
    @FXML
    private TableView<Pair<Integer, String>> overviewTable;

    /**
     * Deletes the given items from the price table
     *
     * @return the number of items deleted
     * @throws IOException if an error occurs during deletion from the database
     */
    private static int deleteItemsFromPriceTable(Collection<Integer> items) throws IOException {
        return PriceUpdateController.getDatabaseOrThrowException().deleteFrom(Price.PRICE).where(Price.PRICE.ART_NO.in(items)).execute();
    }

    /**
     * Handles the confirm deletion button press. Invokes the deletion by calling {@link #deleteItemsFromPriceTable(Collection)} and shows the user the number of items deleted.
     * If an error occurs, shows an error alert.
     */
    @FXML
    private void handleConfirmDeletion() {
        try {
            int numberOfDeletedItems = deleteItemsFromPriceTable(overviewTable.getItems().stream().map(Pair::getKey).collect(Collectors.toSet()));
            // show info to user
            (new Alert(Alert.AlertType.INFORMATION, numberOfDeletedItems + " items deleted.")).showAndWait();
        } catch (IOException e) {
            (new Alert(Alert.AlertType.ERROR, "Error while deleting articles: " + e)).showAndWait();
            e.printStackTrace();
        }
        ((Stage) overviewTable.getScene().getWindow()).close();
    }

    /**
     * Initializes the view. In particular, sets up the table view of the overview table to consist of two columns: article number, ferix key
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // setup table view
        overviewTable.getColumns().clear();
        TableColumn<Pair<Integer, String>, Integer> articleNumberColumn = new TableColumn<>("Article number");
        articleNumberColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getKey()));
        TableColumn<Pair<Integer, String>, String> ferixKeyColumn = new TableColumn<>("Ferix key");
        ferixKeyColumn.setCellValueFactory(i -> new ReadOnlyObjectWrapper<>(i.getValue().getValue()));
        overviewTable.getColumns().add(articleNumberColumn);
        overviewTable.getColumns().add(ferixKeyColumn);
    }

    /**
     * Accepts the passed data and initializes the content of the overview table.
     *
     * @param items the items to be deleted (and shown in the overview table). A map that maps article number to the corresponding ferix key.
     */
    public void passData(Map<Integer, String> items) {
        // init table view with items
        overviewTable.getItems().clear();
        overviewTable.getItems().addAll(items.entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }
}
