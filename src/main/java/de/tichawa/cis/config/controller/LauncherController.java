package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.util.ResourceBundle;

/**
 * Controller class for Launcher.fxml
 */
public class LauncherController {

    protected CIS CIS_DATA;
    @FXML
    private ComboBox<String> cisComboBox;

    /**
     * handles the continue button press by opening the mask for the selected CIS
     */
    @FXML
    private void handleContinue() {
        Util.createNewStage(cisComboBox.getSelectionModel().getSelectedItem().toLowerCase() + "/Mask.fxml",
                        cisComboBox.getSelectionModel().getSelectedItem() + "_" + ResourceBundle.getBundle("de.tichawa.cis.config.version").getString("version"))
                .show();
    }

    /**
     * handles the update prices button press by launching the price update view.
     */
    @FXML
    private void handleUpdate() {
        Util.createNewStage("price_update/PriceUpdate.fxml", "Configurator price update").show();
    }
}
