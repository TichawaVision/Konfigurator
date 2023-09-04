package de.tichawa.cis.config.bdcis;

import de.tichawa.cis.config.CIS;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.beans.*;
import java.net.URL;
import java.util.*;

public class MaskController extends de.tichawa.cis.config.controller.MaskController<BDCIS> implements PropertyChangeListener {

    @FXML
    private Label tiViKeyLabel;

    public MaskController() {
        CIS_DATA = new BDCIS();
        CIS_DATA.addObserver(this); // add this as observer to listen for changes to the model
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateTiViKey();
        //throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        // TODO
        return new LinkedList<>();
    }

    /**
     * updates the {@link #tiViKeyLabel} by setting the current tivi key of the cis
     */
    private void updateTiViKey() {
        tiViKeyLabel.setText(CIS_DATA.getTiViKey());
    }
}
