package de.tichawa.cis.config.controller.price_update;

import de.tichawa.cis.config.model.tables.Price;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

/**
 * Controller class for PriceUpdateNewItem.fxml
 */
public class PriceUpdateNewItemController {
    private int oldArticleNumber;

    @FXML
    private Label articleNumberLabel;
    @FXML
    private TextField ferixKeyTextField;
    @FXML
    private TextField assemblyTimeTextField;
    @FXML
    private TextField powerConsumptionTextField;
    @FXML
    private TextField weightTextField;
    @FXML
    private TextField photoValueTextField;

    public void passData(int oldArticleNumber, int newArticleNumber) {
        this.oldArticleNumber = oldArticleNumber;
        articleNumberLabel.setText(String.valueOf(newArticleNumber));

        try {
            PriceRecord oldArticle = PriceUpdateController.getDatabaseOrThrowException().selectFrom(Price.PRICE).where(Price.PRICE.ART_NO.eq(oldArticleNumber)).fetchOne();
            if (oldArticle == null) {
                System.err.println("did not find old article data in database");
                return;
            }

            ferixKeyTextField.setText(oldArticle.getFerixKey());
            assemblyTimeTextField.setText(String.valueOf(oldArticle.getAssemblyTime()));
            powerConsumptionTextField.setText(String.valueOf(oldArticle.getPowerConsumption()));
            weightTextField.setText(String.valueOf(oldArticle.getWeight()));
            photoValueTextField.setText(String.valueOf(oldArticle.getPhotoValue()));
        } catch (IOException e) {
            System.err.println("failed to read old article data from database");
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            // convert all inputs to right format
            int newArticleNumber = Integer.parseInt(articleNumberLabel.getText());
            double assemblyTime = Double.parseDouble(assemblyTimeTextField.getText());
            double powerConsumption = Double.parseDouble(powerConsumptionTextField.getText());
            double weight = Double.parseDouble(weightTextField.getText());
            double photoValue = Double.parseDouble(photoValueTextField.getText());

            // add new to database
            if (addArticleToDatabase(newArticleNumber, ferixKeyTextField.getText(), assemblyTime, powerConsumption, weight, photoValue))
                // and replace in existing
                PriceUpdateInactiveItemsController.replaceItem(oldArticleNumber, newArticleNumber);
        } catch (NumberFormatException e) {
            // wrong user inputs -> show alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("invalid format: all values but the ferix key must be parsable to double!");
            alert.showAndWait();
        }
    }

    private boolean addArticleToDatabase(int articleNumber, String ferixKey, double assemblyTime, double powerConsumption, double weight, double photoValue) {
        try {
            int inserted = PriceUpdateController.getDatabaseOrThrowException().insertInto(Price.PRICE,
                            Price.PRICE.PRICE_, Price.PRICE.ART_NO, Price.PRICE.FERIX_KEY, Price.PRICE.ASSEMBLY_TIME, Price.PRICE.POWER_CONSUMPTION, Price.PRICE.WEIGHT, Price.PRICE.PHOTO_VALUE, Price.PRICE.DISPLAY_NAME)
                    .values(0d, articleNumber, ferixKey, assemblyTime, powerConsumption, weight, photoValue, "").execute();
            if (inserted == 1) {
                System.out.println("added article " + articleNumber + " to database");
                return true;
            } else
                System.out.println("did not insert 1 article into database but " + inserted);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Database connection failed: Unable to add new article.");
            alert.showAndWait();
        }
        return false;
    }
}