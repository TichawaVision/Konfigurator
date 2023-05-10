package de.tichawa.cis.config.controller.price_update;

import de.tichawa.cis.config.model.tables.Price;
import de.tichawa.cis.config.model.tables.records.PriceRecord;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller class for PriceUpdateNewItem.fxml
 */
public class PriceUpdateNewItemController {
    private boolean addingSuccessful = false;
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

    /**
     * Accepts the data for initialization. Initializes the form values for the new article with the existing values of the old one.
     *
     * @param oldArticleNumber the article number of the old article. This is used to read the existing values for the other form fields.
     * @param newArticleNumber the article number of the new article. This is shown in the form as the new article number.
     */
    public void passData(int oldArticleNumber, int newArticleNumber) {
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

    /**
     * Handles the submit button press. Inserts a new article to the database via {@link #addArticleToDatabase(int, String, double, double, double, double)}.
     * Sets the {@link #addingSuccessful} flag to true if the creation was successful.
     * Shows an alert otherwise or if the user input has the wrong format.
     */
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
            if (addArticleToDatabase(newArticleNumber, ferixKeyTextField.getText(), assemblyTime, powerConsumption, weight, photoValue)) {
                // set flag true so that old one can be replaced
                addingSuccessful = true;
                // close window for further processing
                ((Stage) articleNumberLabel.getScene().getWindow()).close();
            }
        } catch (NumberFormatException e) {
            // wrong user inputs -> show alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("invalid format: all values but the ferix key must be parsable to double!");
            alert.showAndWait();
        }
    }

    /**
     * Inserts the given article into the database's price table
     *
     * @param articleNumber    the article number of the new article
     * @param ferixKey         the ferix key of the new article
     * @param assemblyTime     the assembly time of the new article
     * @param powerConsumption the power consumption of the new article
     * @param weight           the weight of the new article
     * @param photoValue       the photo value of the new article
     * @return whether the inserting succeeded
     */
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

    /**
     * Getter for the flag {@link #addingSuccessful}.
     * To be called after the user closed the form to check whether the old item can be replaced with the new one.
     *
     * @return whether the insertion of the new article into the price database table was successful
     */
    public boolean wasAddingSuccessful() {
        return addingSuccessful;
    }
}