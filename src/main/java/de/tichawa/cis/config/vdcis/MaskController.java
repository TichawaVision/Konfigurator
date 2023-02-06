package de.tichawa.cis.config.vdcis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.ldstd.LDSTD;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MaskController extends de.tichawa.cis.config.controller.MaskController<VDCIS> {

    @FXML
    ComboBox<String> CameraLinkMode;

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

        Color.valueProperty().addListener((observable, oldValue, newValue) ->
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
                Color.getSelectionModel().select(oldValue);
                return;
            }
            if (newValue.equals("Three phases (RGB)")) {
                InternalLightColor.getSelectionModel().selectFirst();
            }
            InternalLightColor.setDisable(newValue.equals("Three phases (RGB)"));

            MaxLineRate.setText(Math.round((CIS_DATA.getMaxLineRate() / 1000.0) * 100.0) / 100.0 + " kHz");
            SelLineRate.setMax(CIS_DATA.getMaxLineRate());
            SelLineRate.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);
        });
        Resolution.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedResolution(getResolutions().get(Resolution.getSelectionModel().getSelectedIndex()));

            if (CIS_DATA.getPhaseCount() > 4 && CIS_DATA.getSelectedResolution().getActualResolution() > 600) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Number of Phases greater than three.\nPlease reduce the resolution ");
                alert.show();
                Resolution.getSelectionModel().select(oldValue);
                return;
            }

            MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000.0 + " kHz");
            SelLineRate.setMax(CIS_DATA.getMaxLineRate());
            SelLineRate.setValue(CIS_DATA.getMaxLineRate());

            CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

            PixelSize.setText(CIS_DATA.getSelectedResolution().getPixelSize() + " mm");
            DefectSize.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * 3, 5) + " mm");
            Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        ScanWidth.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

            if (sw > 1200) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Selected scan width not available.");
                alert.show();
                ScanWidth.setValue(oldValue);
                return;
            }
            CIS_DATA.setScanWidth(sw);
        });
        SelLineRate.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            CIS_DATA.setSelectedLineRate(newValue.intValue());

            CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

            Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
            Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
            Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
            Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
        });
        InternalLightSource.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if (CIS_DATA.getPhaseCount() == 4 && newValue.equals("None")) {
                InternalLightSource.getSelectionModel().select(oldValue);
                return;
            }

            switch (InternalLightSource.getSelectionModel().getSelectedIndex()) {
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
        InternalLightColor.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
                .ifPresent(CIS_DATA::setLightColor));
        Interface.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setGigeInterface(Interface.getSelectionModel().getSelectedIndex() == 1));
        Cooling.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
                .findByDescription(newValue.split("\\(")[0].trim())
                .ifPresent(CIS_DATA::setCooling));
        Trigger.selectedProperty().addListener((observable, oldValue, newValue) -> CIS_DATA.setExternalTrigger(newValue));
        CameraLinkMode.valueProperty().addListener((observable, oldValue, newValue) ->
                CIS_DATA.setCLMode(CameraLinkMode.getSelectionModel().getSelectedItem()));

        Color.getSelectionModel().selectFirst();
        Resolution.getSelectionModel().selectFirst();
        ScanWidth.getSelectionModel().selectLast();
        InternalLightSource.getSelectionModel().select(2);
        InternalLightColor.getSelectionModel().select(1);
        Interface.getSelectionModel().selectFirst();
        Cooling.getSelectionModel().select(1);
        Trigger.setSelected(false);
        CameraLinkMode.getSelectionModel().selectLast();
    }
}
