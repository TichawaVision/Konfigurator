package de.tichawa.cis.config.bdcis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.controller.MaskControllerCisWith5Lights;

import java.beans.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller class for bdcis/Mask.fxml
 */
public class MaskController extends MaskControllerCisWith5Lights<BDCIS> implements PropertyChangeListener {
    /**
     * List of possible scan widths for a BDCIS
     */
    private static final List<Integer> SCAN_WIDTH_OPTIONS = Arrays.asList(260, 520, 780, 1040, 1300, 1560, 1820, 2080);

    /**
     * Default constructor that creates a new {@link BDCIS} object that gets changed by the user.
     */
    public MaskController() {
        CIS_DATA = new BDCIS();
        CIS_DATA.addObserver(this); // add this as observer to listen for changes to the model
    }

    /**
     * Returns a list of all light colors that can be used in the BDCIS
     */
    @Override
    protected List<CIS.LightColor> getLightColorOptions() {
        return Arrays.stream(CIS.LightColor.values()).filter(BDCIS::isBDCISLightColor).collect(Collectors.toList());
    }

    /**
     * Returns a list of all light colors that can be used in the BDCIS with coax. As BDCIS does not support coax lighting this is the same as {@link #getLightColorOptions()}
     */
    @Override
    protected List<CIS.LightColor> getLightColorOptionsCoax() {
        return getLightColorOptions();
    }

    /**
     * Returns a list of all scan width options for BDCIS
     */
    @Override
    protected List<Integer> getScanWidthOptions() {
        return SCAN_WIDTH_OPTIONS;
    }

    /**
     * Handles a change to the coax lighting.
     * Since BDCIS does not use coax lighting this should not ever be called.
     *
     * @throws IllegalStateException if this is called as there should not be a coax lighting change
     */
    @Override
    protected void handleCoaxChange(CIS.LightColor oldValue, CIS.LightColor newValue) {
        // do nothing (no coax for BDCIS)
        throw new IllegalStateException("there should not be a coax light change for BDCIS");
    }

    /**
     * Initializes the color and light sources section of the user interface by disabling dark field and coax choice boxes
     */
    @Override
    protected void initColorAndLightSources() {
        super.initColorAndLightSources();
        coaxChoiceBox.setDisable(true);
        darkFieldLeftChoiceBox.setDisable(true);
        darkFieldRightChoiceBox.setDisable(true);
    }

    /**
     * Initializes the optics section by setting the lens code label text (the other lens options are already disabled via the Mask.fxml)
     */
    @Override
    protected void initOptics() {
        lensCodeLabel.setText(CIS_DATA.getLensTypeCode());
    }

    /**
     * Returns whether the given phase is a valid phase option for BDCIS
     *
     * @return true if the phase count is less than 4 or false otherwise
     */
    @Override
    protected boolean isValidPhaseOption(Phases phase) {
        switch (phase) {
            case ONE:
                //case TWO:
            case THREE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Handles property changes to the model (the underlying {@link BDCIS} object)
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
    }

    /**
     * Returns a list of all available resolutions for BDCIS
     */
    @Override
    public List<CIS.Resolution> setupResolutions() {
        return BDCIS.getResolutions();
    }
}
