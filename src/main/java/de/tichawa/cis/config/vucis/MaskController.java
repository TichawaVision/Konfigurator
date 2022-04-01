package de.tichawa.cis.config.vucis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.ldstd.LDSTD;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

import java.net.URL;
import java.util.*;

public class MaskController extends de.tichawa.cis.config.MaskController<VUCIS>
{

  @FXML
  public ChoiceBox<String> LightPreset;
  @FXML
  public ChoiceBox<String> LensType;
  @FXML
  private ChoiceBox<String> BrightFieldLeft;
  @FXML
  private ChoiceBox<String> Coax;
  @FXML
  private ChoiceBox<String> BrightFieldRight;
  @FXML
  private ChoiceBox<String> DarkFieldLeft;
  @FXML
  private ChoiceBox<String> DarkFieldRight;

  public MaskController()
  {
    CIS_DATA = new VUCIS();
    LDSTD_DATA = new LDSTD();
  }

  @Override
  public List<CIS.Resolution> setupResolutions()
  {
    return Arrays.asList(
            new CIS.Resolution(1000,1000,true,0,0.025),
            new CIS.Resolution(1000,1000,false,0,0.025),
            new CIS.Resolution(500,1000,false,0,0.05),
            new CIS.Resolution(250,1000,false,0,0.1),
            new CIS.Resolution(125,1000,false,0,0.02),
            new CIS.Resolution(100,1000,false,0,0.25),
            new CIS.Resolution(50,1000,false,0,0.5),
            new CIS.Resolution(25,1000,false,0,1.0));
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    CIS_DATA.setSelectedResolution(getResolutions().get(0));
    CIS_DATA.setScanWidth(1200);
    CIS_DATA.setExternalTrigger(false);
    CIS_DATA.setPhaseCount(2);
    CIS_DATA.setCooling(CIS.Cooling.LICO);

    LightPreset.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      Optional<VUCIS.LightPreset> lightPreset = VUCIS.LightPreset.findByDescription(newValue);
      lightPreset.ifPresent(CIS_DATA::setLightPreset);

      boolean disable = !CIS_DATA.getLightPreset().equals(VUCIS.LightPreset.MANUAL);
      BrightFieldLeft.setDisable(disable);
      Coax.setDisable(disable);
      BrightFieldRight.setDisable(disable);
      DarkFieldLeft.setDisable(disable);
      DarkFieldRight.setDisable(disable);
    });

    BrightFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLeftBrightField));
    Coax.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setCoaxLight));
    BrightFieldRight.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setRightBrightField));
    DarkFieldLeft.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLeftDarkField));
    DarkFieldRight.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LightColor.findByDescription(newValue)
            .ifPresent(CIS_DATA::setRightDarkField));
    LensType.valueProperty().addListener((observable, oldValue, newValue) -> VUCIS.LensType.findByDescription(newValue)
            .ifPresent(CIS_DATA::setLensType));

    Resolution.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      CIS_DATA.setSelectedResolution(getResolutions().get(Resolution.getSelectionModel().getSelectedIndex()));

      MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
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
    ScanWidth.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setScanWidth(Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim())));
    SelLineRate.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      CIS_DATA.setSelectedLineRate(newValue.intValue());

      CurrLineRate.setText(newValue.intValue() / 1000.0 + " kHz");

      Speedmms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate(), 3) + " mm/s");
      Speedms.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() / 1000, 3) + " m/s");
      Speedmmin.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.06, 3) + " m/min");
      Speedips.setText(CIS.round(CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate() * 0.03937, 3) + " ips");
    });

    Interface.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setGigeInterface(Interface.getSelectionModel().getSelectedIndex() == 1));
    Cooling.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
            .findByDescription(newValue.split("\\(")[0].trim())
            .ifPresent(CIS_DATA::setCooling));

    LightPreset.getSelectionModel().selectFirst();
    BrightFieldLeft.getSelectionModel().select(1);
    Coax.getSelectionModel().selectFirst();
    BrightFieldRight.getSelectionModel().select(1);
    DarkFieldLeft.getSelectionModel().selectFirst();
    DarkFieldRight.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectLast();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    LensType.getSelectionModel().selectFirst();
  }
}
