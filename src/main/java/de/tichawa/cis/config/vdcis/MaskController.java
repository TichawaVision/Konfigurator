package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.ldstd.LDSTD;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;

public class MaskController extends de.tichawa.cis.config.controller.MaskController<VDCIS> {

    @FXML
    ComboBox<String> cameraLinkModeComboBox;

    public MaskController() {
        CIS_DATA = new VDCIS();
        LDSTD_DATA = new LDSTD();
    }

    @Override
    public List<CIS.Resolution> setupResolutions() {
        return Arrays.asList(
                new CIS.Resolution(1000, 1000, true, 2.5, 0.0254),
                new CIS.Resolution(1000, 1000, false, 2.5, 0.0254),
                new CIS.Resolution(500, 500, false, 5.0, 0.0508),
                new CIS.Resolution(250, 250, false, 10.0, 0.1016),
                new CIS.Resolution(125, 250, false, 10.0, 0.2032),
                new CIS.Resolution(100, 500, false, 10.0, 0.254),
                new CIS.Resolution(50, 250, false, 10.0, 0.508),
                new CIS.Resolution(25, 250, false, 10.0, 1.016));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        CIS_DATA.setPhaseCount(2);
        CIS_DATA.setLightColor(CIS.LightColor.RED);
        CIS_DATA.setSelectedResolution(getResolutions().get(0));
        CIS_DATA.setScanWidth(1200);
        CIS_DATA.setExternalTrigger(false);
        CIS_DATA.setCLMode("Full80");
        CIS_DATA.setCooling(CIS.Cooling.FAIR);
        CIS_DATA.setDiffuseLightSources(1);
        CIS_DATA.setCoaxLightSources(0);

        colorComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            switch (newValue) {
                case "One phase (Monochrome)": {
                    CIS_DATA.setPhaseCount(2);
                    break;
                }
                case "Two phases": {
                    CIS_DATA.setPhaseCount(3);
                    break;
                }
                case "Three phases (RGB)": {
                    CIS_DATA.setPhaseCount(4);
                    break;
                }
                case "Four phases": {
                    CIS_DATA.setPhaseCount(5);
                    break;
                }
                case "Five phases": {
                    CIS_DATA.setPhaseCount(6);
                    break;
                }
                case "Six phases": {
                    CIS_DATA.setPhaseCount(7);
                    break;
                }
            }
            if (CIS_DATA.getPhaseCount() > 4 && CIS_DATA.getSelectedResolution().getActualResolution() > 600) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Not available with the selected Resolution");
                alert.show();
                colorComboBox.getSelectionModel().select(oldValue);
                return;
            }
            if (newValue.equals("Three phases (RGB)")) {
                internalLightColorComboBox.getSelectionModel().selectFirst();
            }
            internalLightColorComboBox.setDisable(newValue.equals("Three phases (RGB)"));

            maxLineRateLabel.setText(Math.round((CIS_DATA.getMaxLineRate() / 1000.0) * 100.0) / 100.0 + " kHz");
            selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
            selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);
        });
        resolutionComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedResolution(getResolutions().get(resolutionComboBox.getSelectionModel().getSelectedIndex()));

            if (CIS_DATA.getPhaseCount() > 4 && CIS_DATA.getSelectedResolution().getActualResolution() > 600) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Number of Phases greater than three.\nPlease reduce the resolution ");
                alert.show();
                resolutionComboBox.getSelectionModel().select(oldValue);
                return;
            }

            maxLineRateLabel.setText(CIS_DATA.getMaxLineRate() / 1000.0 + " kHz");
            selectedLineRateSlider.setMax(CIS_DATA.getMaxLineRate());
            selectedLineRateSlider.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            pixelSizeLabel.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
            defectSizeLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        scanWidthComboBox.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

            if (sw > 1200) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Selected scan width not available.");
                alert.show();
                scanWidthComboBox.setValue(oldValue);
                return;
            }
            CIS_DATA.setScanWidth(sw);
        });
        selectedLineRateSlider.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedLineRate(newValue.intValue());

            currentLineRateLabel.setText(newValue.intValue() / 1000.0 + " kHz");

            speedmmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            speedmsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            speedmminLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            speedipsLabel.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        internalLightSourceComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (CIS_DATA.getPhaseCount() == 4 && newValue.equals("None")) {
                internalLightSourceComboBox.getSelectionModel().select(oldValue);
                return;
            }

            switch (internalLightSourceComboBox.getSelectionModel().getSelectedIndex()) {
                case 0:
                    CIS_DATA.setDiffuseLightSources(0);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 1:
                    CIS_DATA.setDiffuseLightSources(1);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 2:
                    CIS_DATA.setDiffuseLightSources(2);
                    CIS_DATA.setCoaxLightSources(0);
                    break;
                case 3:
                    CIS_DATA.setDiffuseLightSources(2);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
                case 4:
                    CIS_DATA.setDiffuseLightSources(0);
                    CIS_DATA.setCoaxLightSources(1);
                    break;
            }
        });
        internalLightColorComboBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
                .ifPresent(CIS_DATA::setLightColor));
        interfaceComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setGigeInterface(interfaceComboBox.getSelectionModel().getSelectedIndex() == 1));
        coolingComboBox.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
                .findByDescription(newValue.split("\\(")[0].trim())
                .ifPresent(CIS_DATA::setCooling));
        externalTriggerCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setExternalTrigger(newValue));
        cameraLinkModeComboBox.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setCLMode(cameraLinkModeComboBox.getSelectionModel().getSelectedItem()));

        colorComboBox.getSelectionModel().selectFirst();
        resolutionComboBox.getSelectionModel().selectFirst();
        scanWidthComboBox.getSelectionModel().selectLast();
        internalLightSourceComboBox.getSelectionModel().select(2);
        internalLightColorComboBox.getSelectionModel().select(1);
        interfaceComboBox.getSelectionModel().selectFirst();
        coolingComboBox.getSelectionModel().select(1);
        externalTriggerCheckbox.setSelected(false);
        cameraLinkModeComboBox.getSelectionModel().selectLast();
    }
}
