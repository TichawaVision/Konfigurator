package de.tichawa.cis.config;

import java.net.*;
import java.util.*;
import javafx.fxml.*;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.scene.transform.*;
import javafx.stage.*;

public class DataSheetController implements Initializable
{
  protected CIS CIS_DATA;

  @FXML
  private TextArea Header;
  @FXML
  private TextArea Specs;
  @FXML
  private TextArea CLConfig;
  @FXML
  private ImageView ProductPic;
  @FXML
  private ImageView ProfilePic;
  @FXML
  private Menu File;
  @FXML
  private MenuItem Print;
  @FXML
  private Menu Lang;
  @FXML
  private MenuItem SwitchLang;

  @Override
  public void initialize(URL url, ResourceBundle rb)
  {

  }

  public void passData(CIS data)
  {
    this.CIS_DATA = data;

    load();
  }

  public TextArea getHeader()
  {
    return Header;
  }

  public TextArea getSpecs()
  {
    return Specs;
  }

  public TextArea getCLConfig()
  {
    return CLConfig;
  }

  private void load()
  {
    try
    {
      Lang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("lang"));
      SwitchLang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("switchlang"));

      File.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("File"));
      Print.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("Print"));

      String[] dataSheetText = CIS_DATA.createPrntOut().split("\n\t\n");
      String key = CIS_DATA.getTiViKey();

      Header.setText(dataSheetText[0]);
      Header.setEditable(false);
      Specs.setText(dataSheetText[1]);
      Specs.setEditable(false);
      Specs.setMinHeight((dataSheetText[1].length() - dataSheetText[1].replace("\n", "").length()) * 20);
      CLConfig.setText(dataSheetText[2]);
      CLConfig.setEditable(false);
      CLConfig.setMinHeight((dataSheetText[2].length() - dataSheetText[2].replace("\n", "").length()) * 20);

      ProductPic.setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Product.jpg")));

      if(key.contains("MXCIS"))
      {
        String append = "";

        if(key.split("_")[5].endsWith("C"))
        {
          append += "_coax";
        }

        if(key.split("_")[5].startsWith("2"))
        {
          append += "_L2";
        }
        else
        {
          append += "_L1";
        }

        ProfilePic.setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile" + append + ".jpg")));
      }
      else if(key.split("_")[4].endsWith("C") && getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg") != null)
      {
        ProfilePic.setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile_coax.jpg")));
      }
      else
      {
        ProfilePic.setImage(new Image(getClass().getResourceAsStream("/de/tichawa/cis/config/" + key.toLowerCase().split("_")[1] + "/Profile.jpg")));
      }
    }
    catch(CISException e)
    {
      ((Stage) Header.getScene().getWindow()).close();

      Alert a = new Alert(AlertType.ERROR, e.getMessage());
      a.show();
    }
    catch(NullPointerException e)
    {
      e.printStackTrace();
    }
  }

  public void print()
  {
    PrinterJob p = PrinterJob.createPrinterJob();
    Pane printable = (Pane) Header.getScene().getRoot();

    if(p.showPrintDialog(null))
    {
      p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(p.getJobSettings().getPageLayout().getPaper(), PageOrientation.PORTRAIT, 0.0, 0.0, 0.0, 0.0));
      double scaleX = p.getJobSettings().getPageLayout().getPrintableHeight() / printable.getHeight();
      double scaleY = p.getJobSettings().getPageLayout().getPrintableWidth() / printable.getWidth();

      printable.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));

      if(p.printPage(printable))
      {
        printable.getTransforms().clear();
        new Alert(AlertType.INFORMATION, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("printsuccess")).show();
        p.endJob();
      }
      else
      {
        printable.getTransforms().clear();
        new Alert(AlertType.ERROR, ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings.")).show();
      }
    }
  }

  public void switchLang()
  {
    if(CIS_DATA.getLocale().toString().equals("de_DE"))
    {
      CIS_DATA.setLocale(new Locale("en", "US"));
    }
    else if(CIS_DATA.getLocale().toString().equals("en_US"))
    {
      CIS_DATA.setLocale(new Locale("de", "DE"));
    }

    load();
  }

  public ImageView getProfilePic()
  {
    return ProfilePic;
  }
}
