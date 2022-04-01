package de.tichawa.cis.config.vhcis;

import de.tichawa.cis.config.CIS;
import de.tichawa.cis.config.ldstd.LDSTD;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MaskController extends de.tichawa.cis.config.MaskController<VHCIS>
{
  public MaskController()
  {
    CIS_DATA = new VHCIS();
    LDSTD_DATA = new LDSTD();
  }

  @Override
  public List<CIS.Resolution> setupResolutions()
  {
    return Arrays.asList(
            new CIS.Resolution(1200,1200,true,16.0,0.02115),
            new CIS.Resolution(1200,1200,false,8.0,0.02115),
            new CIS.Resolution(600,600,false,6.0,0.0423),
            new CIS.Resolution(400,1200,false,4.0,0.0635),
            new CIS.Resolution(300,300,false,3.0,0.0847),
            new CIS.Resolution(200,600,false,2.0,0.125),
            new CIS.Resolution(150,300,false,1.5,0.167),
            new CIS.Resolution(100,300,false,1.0,0.25),
            new CIS.Resolution(75,300,false,1.0,0.339),
            new CIS.Resolution(50,300,false,0.5,0.5),
            new CIS.Resolution(25,300,false,0.25,1.0));
  }

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {
    CIS_DATA.setPhaseCount(1);
    CIS_DATA.setLightColor(CIS.LightColor.RED);
    CIS_DATA.setDiffuseLightSources(1);
    CIS_DATA.setCoaxLightSources(0);
    CIS_DATA.setSelectedResolution(getResolutions().get(0));
    CIS_DATA.setScanWidth(780);
    CIS_DATA.setExternalTrigger(false);
    CIS_DATA.setCooling(CIS.Cooling.LICO);

    Color.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      if(newValue.equals("RGB") && CIS_DATA.getLedLines() == 0)
      {
        Color.getSelectionModel().select(oldValue);
        return;
      }

      switch(newValue)
      {
        case "Monochrome":
        {
          CIS_DATA.setPhaseCount(1);
          break;
        }
        case "RGB":
        {
          CIS_DATA.setPhaseCount(3);
          break;
        }
      }

      InternalLightColor.setDisable(newValue.equals("RGB") || CIS_DATA.getLedLines() == 0);
      ExternalLightColor.setDisable(newValue.equals("RGB") || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);

      MaxLineRate.setText(CIS_DATA.getMaxLineRate() / 1000 + " kHz");
      SelLineRate.setMax(CIS_DATA.getMaxLineRate());
      SelLineRate.setValue(CIS_DATA.getMaxLineRate());

      CIS_DATA.setTransportSpeed((int) (CIS_DATA.getSelectedResolution().getPixelSize() * CIS_DATA.getSelectedLineRate()) * 1000);

      LDSTD_DATA.setPhaseCount(CIS_DATA.getPhaseCount());
    });
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
    {
      int sw = Integer.parseInt(newValue.substring(0, newValue.lastIndexOf(" ")).trim());

      CIS_DATA.setScanWidth(sw);
      LDSTD_DATA.setScanWidth(CIS_DATA.getScanWidth());
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
      if(CIS_DATA.getPhaseCount() == 3 && newValue.equals("None") && LDSTD_DATA.getLedLines() == 0)
      {
        InternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      switch(InternalLightSource.getSelectionModel().getSelectedIndex())
      {
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
      }
      InternalLightColor.setDisable(CIS_DATA.getPhaseCount() == 3 || CIS_DATA.getLedLines() == 0);
    });
    InternalLightColor.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS.LightColor.findByDescription(newValue)
                    .ifPresent(CIS_DATA::setLightColor));
    ExternalLightSource.valueProperty().addListener((observable, oldValue, newValue) ->
    {
      if(CIS_DATA.getPhaseCount() == 3 && CIS_DATA.getLedLines() == 0 && newValue.equals("None"))
      {
        ExternalLightSource.getSelectionModel().select(oldValue);
        return;
      }

      ExternalLightColor.setDisable(CIS_DATA.getPhaseCount() == 3 || ExternalLightSource.getSelectionModel().getSelectedIndex() == 0);
      switch(ExternalLightSource.getSelectionModel().getSelectedIndex())
      {
        case 0:
          LDSTD_DATA.setDiffuseLightSources(0);
          LDSTD_DATA.setCoaxLightSources(0);
          break;
        case 1:
          LDSTD_DATA.setDiffuseLightSources(1);
          LDSTD_DATA.setCoaxLightSources(0);
          break;
      }
    });
    ExternalLightColor.valueProperty().addListener((observable, oldValue, newValue) -> CIS.LightColor.findByDescription(newValue)
            .ifPresent(LDSTD_DATA::setLightColor));
    Interface.valueProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setGigeInterface(Interface.getSelectionModel().getSelectedIndex() == 1));
    Cooling.valueProperty().addListener((observable, oldValue, newValue) -> CIS.Cooling
            .findByDescription(newValue.split("\\(")[0].trim())
            .ifPresent(CIS_DATA::setCooling));
    Trigger.selectedProperty().addListener((observable, oldValue, newValue) ->
            CIS_DATA.setExternalTrigger(newValue));

    Color.getSelectionModel().selectFirst();
    Resolution.getSelectionModel().selectFirst();
    ScanWidth.getSelectionModel().selectFirst();
    InternalLightSource.getSelectionModel().select(1);
    ExternalLightSource.getSelectionModel().selectFirst();
    InternalLightColor.getSelectionModel().selectFirst();
    ExternalLightColor.getSelectionModel().selectFirst();
    Interface.getSelectionModel().selectFirst();
    Cooling.getSelectionModel().select(1);
    Trigger.setSelected(false);
  }
}
