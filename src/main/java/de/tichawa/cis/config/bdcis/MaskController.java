package de.tichawa.cis.config.bdcis;

import de.tichawa.cis.config.CIS;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.beans.*;
import java.net.URL;
import java.util.*;

/**
 * Controller class for bdcis/Mask.fxml
 */
public class MaskController extends de.tichawa.cis.config.controller.MaskController<BDCIS> implements PropertyChangeListener {

    @FXML
    private Label currentLineRatePercentLabel;
    @FXML
    private Label tiViKeyLabel;

    /**
     * Default constructor that creates a new {@link BDCIS} object that gets changed by the user.
     */
    public MaskController() {
        CIS_DATA = new BDCIS();
        CIS_DATA.addObserver(this); // add this as observer to listen for changes to the model
    }

    private void initResolutionAndScanWidth() {
        scanWidthChoiceBox.getItems().clear();
        scanWidthChoiceBox.getItems().addAll();// TODO add scan widths
    }

    /**
     * Initializes the GUI for BDCIS.
     * This method is automatically called when the GUI for BDCIS is created.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        super.initialize(url, rb);

        initResolutionAndScanWidth();

        //TODO

        updateTiViKey();
    }

    /**
     * Returns a list of all available resolutions for BDCIS
     */
    @Override
    public List<CIS.Resolution> setupResolutions() {
        return BDCIS.getResolutions();
    }

    /**
     * Handles property changes to the model (the underlying {@link BDCIS} object)
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("BDCIS Maskcontroller: observed change for " + evt.getPropertyName() + " to " + evt.getNewValue());

        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * Updates the {@link #tiViKeyLabel} by setting the current tivi key of the cis
     */
    private void updateTiViKey() {
        tiViKeyLabel.setText(CIS_DATA.getTiViKey());
    }
}
