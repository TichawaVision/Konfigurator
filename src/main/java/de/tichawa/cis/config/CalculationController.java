package de.tichawa.cis.config;

import java.net.*;
import java.text.*;
import java.util.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.print.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.transform.*;
import javafx.stage.*;

public class CalculationController implements Initializable
{
  
  protected CIS CIS_DATA;
  
  @FXML
  private Label Header;
  @FXML
  private TableView<CalcLine> Electronics;
  @FXML
  private TableView<CalcLine> ElectSubTotals;
  @FXML
  private TableView<CalcLine> Mechanics;
  @FXML
  private TableView<CalcLine> MechaSubTotals;
  @FXML
  private TableView<PriceLine> Totals;
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
  
  @SuppressWarnings("unchecked")
  private void load()
  {
    try
    {
      Lang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("lang"));
      SwitchLang.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("switchlang"));
      
      File.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("File"));
      Print.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("Print"));
      
      NumberFormat f = NumberFormat.getInstance(CIS_DATA.getLocale());
      String[] calcText = CIS_DATA.createCalculation().split("\n\t\n");
      
      Header.setText(calcText[0]);
      
      String electData = calcText[1].substring(calcText[1].indexOf("\n\n") + 2);
      String[] headers = electData.split("\n")[0].split("\t");
      TableColumn<CalcLine, String> electNameCol = new TableColumn<>(headers[0]);
      electNameCol.setMinWidth(300.0);
      electNameCol.setMaxWidth(300.0);
      electNameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
      TableColumn<CalcLine, String> electIdCol = new TableColumn<>(headers[1]);
      electIdCol.setCellValueFactory(cellData -> cellData.getValue().getId());
      electIdCol.setMinWidth(150.0);
      electIdCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Integer> electAmountCol = new TableColumn<>(headers[2]);
      electAmountCol.setCellValueFactory(cellData -> cellData.getValue().getAmount().asObject());
      electAmountCol.setCellFactory(c -> new CustomIntCell());
      electAmountCol.setMinWidth(75.0);
      electAmountCol.setMaxWidth(75.0);
      TableColumn<CalcLine, Double> electPriceCol = new TableColumn<>(headers[3]);
      electPriceCol.setCellValueFactory(cellData -> cellData.getValue().getPrice().asObject());
      electPriceCol.setCellFactory(c -> new CustomDoubleCell());
      electPriceCol.setMinWidth(150.0);
      electPriceCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> electWeightCol = new TableColumn<>(headers[4]);
      electWeightCol.setCellValueFactory(cellData -> cellData.getValue().getWeight().asObject());
      electWeightCol.setCellFactory(c -> new CustomDoubleCell());
      electWeightCol.setMinWidth(200.0);
      electWeightCol.setMaxWidth(200.0);
      TableColumn<CalcLine, Double> electTimeCol = new TableColumn<>(headers[5]);
      electTimeCol.setCellValueFactory(cellData -> cellData.getValue().getTime().asObject());
      electTimeCol.setCellFactory(c -> new CustomDoubleCell());
      electTimeCol.setMinWidth(150.0);
      electTimeCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> electPowerCol = new TableColumn<>(headers[6]);
      electPowerCol.setCellValueFactory(cellData -> cellData.getValue().getPower().asObject());
      electPowerCol.setCellFactory(c -> new CustomDoubleCell());
      electPowerCol.setMinWidth(150.0);
      electPowerCol.setMaxWidth(150.0);
      
      ObservableList<CalcLine> electDataList = FXCollections.observableArrayList();
      
      electData = electData.substring(electData.indexOf("\n") + 1);
      for(String row : electData.split("\n"))
      {
        String[] data = row.split("\t");
        electDataList.add(new CalcLine(data[0], data[1], f.parse(data[2]).intValue(), f.parse(data[3]).doubleValue(), f.parse(data[4]).doubleValue(), f.parse(data[5]).doubleValue(), f.parse(data[6]).doubleValue()));
      }
      
      Electronics.setItems(electDataList);
      Electronics.getColumns().clear();
      Electronics.getColumns().addAll(electNameCol, electIdCol, electAmountCol, electPriceCol, electWeightCol, electTimeCol, electPowerCol);
      
      String eTotalData = calcText[2];
      TableColumn<CalcLine, String> eTotalNameCol = new TableColumn<>(headers[0]);
      eTotalNameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
      eTotalNameCol.setMinWidth(300.0);
      eTotalNameCol.setMaxWidth(300.0);
      TableColumn<CalcLine, String> eTotalIdCol = new TableColumn<>(headers[1]);
      eTotalIdCol.setCellValueFactory(cellData -> cellData.getValue().getId());
      eTotalIdCol.setMinWidth(150.0);
      eTotalIdCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Integer> eTotalAmountCol = new TableColumn<>(headers[2]);
      eTotalAmountCol.setCellValueFactory(cellData -> cellData.getValue().getAmount().asObject());
      eTotalAmountCol.setMinWidth(75.0);
      eTotalAmountCol.setMaxWidth(75.0);
      TableColumn<CalcLine, Double> eTotalPriceCol = new TableColumn<>(headers[3]);
      eTotalPriceCol.setCellValueFactory(cellData -> cellData.getValue().getPrice().asObject());
      eTotalPriceCol.setCellFactory(c -> new CustomDoubleCell());
      eTotalPriceCol.setMinWidth(150.0);
      eTotalPriceCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> eTotalWeightCol = new TableColumn<>(headers[4]);
      eTotalWeightCol.setCellValueFactory(cellData -> cellData.getValue().getWeight().asObject());
      eTotalWeightCol.setCellFactory(c -> new CustomDoubleCell());
      eTotalWeightCol.setMinWidth(200.0);
      eTotalWeightCol.setMaxWidth(200.0);
      TableColumn<CalcLine, Double> eTotalTimeCol = new TableColumn<>(headers[5]);
      eTotalTimeCol.setCellValueFactory(cellData -> cellData.getValue().getTime().asObject());
      eTotalTimeCol.setCellFactory(c -> new CustomDoubleCell());
      eTotalTimeCol.setMinWidth(150.0);
      eTotalTimeCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> eTotalPowerCol = new TableColumn<>(headers[6]);
      eTotalPowerCol.setCellValueFactory(cellData -> cellData.getValue().getPower().asObject());
      eTotalPowerCol.setCellFactory(c -> new CustomDoubleCell());
      eTotalPowerCol.setMinWidth(150.0);
      eTotalPowerCol.setMaxWidth(150.0);
      
      String[] eData = eTotalData.split("\t");
      ElectSubTotals.getItems().clear();
      ElectSubTotals.getItems().add(new CalcLine(eData[0], eData[1], f.parse(eData[2]).intValue(), f.parse(eData[3]).doubleValue(), f.parse(eData[4]).doubleValue(), f.parse(eData[5]).doubleValue(), f.parse(eData[6]).doubleValue()));
      ElectSubTotals.getColumns().clear();
      ElectSubTotals.getColumns().addAll(eTotalNameCol, eTotalIdCol, eTotalAmountCol, eTotalPriceCol, eTotalWeightCol, eTotalTimeCol, eTotalPowerCol);
      
      String mechaData = calcText[3].substring(calcText[3].indexOf("\n\n") + 2);
      TableColumn<CalcLine, String> mechaNameCol = new TableColumn<>(headers[0]);
      mechaNameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
      mechaNameCol.setMinWidth(300.0);
      mechaNameCol.setMaxWidth(300.0);
      TableColumn<CalcLine, String> mechaIdCol = new TableColumn<>(headers[1]);
      mechaIdCol.setCellValueFactory(cellData -> cellData.getValue().getId());
      mechaIdCol.setMinWidth(150.0);
      mechaIdCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Integer> mechaAmountCol = new TableColumn<>(headers[2]);
      mechaAmountCol.setCellValueFactory(cellData -> cellData.getValue().getAmount().asObject());
      mechaAmountCol.setCellFactory(c -> new CustomIntCell());
      mechaAmountCol.setMinWidth(75.0);
      mechaAmountCol.setMaxWidth(75.0);
      TableColumn<CalcLine, Double> mechaPriceCol = new TableColumn<>(headers[3]);
      mechaPriceCol.setCellValueFactory(cellData -> cellData.getValue().getPrice().asObject());
      mechaPriceCol.setCellFactory(c -> new CustomDoubleCell());
      mechaPriceCol.setMinWidth(150.0);
      mechaPriceCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> mechaWeightCol = new TableColumn<>(headers[4]);
      mechaWeightCol.setCellValueFactory(cellData -> cellData.getValue().getWeight().asObject());
      mechaWeightCol.setCellFactory(c -> new CustomDoubleCell());
      mechaWeightCol.setMinWidth(200.0);
      mechaWeightCol.setMaxWidth(200.0);
      
      ObservableList<CalcLine> mechaDataList = FXCollections.observableArrayList();
      
      mechaData = mechaData.substring(mechaData.indexOf("\n") + 1);
      for(String row : mechaData.split("\n"))
      {
        String[] data = row.split("\t");
        mechaDataList.add(new CalcLine(data[0], data[1], f.parse(data[2]).intValue(), f.parse(data[3]).doubleValue(), f.parse(data[4]).doubleValue(), 0.0, 0.0));
      }
      
      Mechanics.setItems(mechaDataList);
      Mechanics.getColumns().clear();
      Mechanics.getColumns().addAll(mechaNameCol, mechaIdCol, mechaAmountCol, mechaPriceCol, mechaWeightCol);
      
      String mTotalData = calcText[4];
      TableColumn<CalcLine, String> mTotalNameCol = new TableColumn<>(headers[0]);
      mTotalNameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
      mTotalNameCol.setMinWidth(300.0);
      mTotalNameCol.setMaxWidth(300.0);
      TableColumn<CalcLine, String> mTotalIdCol = new TableColumn<>(headers[1]);
      mTotalIdCol.setCellValueFactory(cellData -> cellData.getValue().getId());
      mTotalIdCol.setMinWidth(150.0);
      mTotalIdCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Integer> mTotalAmountCol = new TableColumn<>(headers[2]);
      mTotalAmountCol.setCellValueFactory(cellData -> cellData.getValue().getAmount().asObject());
      mTotalAmountCol.setMinWidth(75.0);
      mTotalAmountCol.setMaxWidth(75.0);
      TableColumn<CalcLine, Double> mTotalPriceCol = new TableColumn<>(headers[3]);
      mTotalPriceCol.setCellValueFactory(cellData -> cellData.getValue().getPrice().asObject());
      mTotalPriceCol.setCellFactory(c -> new CustomDoubleCell());
      mTotalPriceCol.setMinWidth(150.0);
      mTotalPriceCol.setMaxWidth(150.0);
      TableColumn<CalcLine, Double> mTotalWeightCol = new TableColumn<>(headers[4]);
      mTotalWeightCol.setCellValueFactory(cellData -> cellData.getValue().getWeight().asObject());
      mTotalWeightCol.setCellFactory(c -> new CustomDoubleCell());
      mTotalWeightCol.setMinWidth(200.0);
      mTotalWeightCol.setMaxWidth(200.0);
      
      String[] mData = mTotalData.split("\t");
      MechaSubTotals.getItems().clear();
      MechaSubTotals.getItems().add(new CalcLine(mData[0], mData[1], f.parse(mData[2]).intValue(), f.parse(mData[3]).doubleValue(), f.parse(mData[4]).doubleValue(), 0.0, 0.0));
      MechaSubTotals.getColumns().clear();
      MechaSubTotals.getColumns().addAll(mTotalNameCol, mTotalIdCol, mTotalAmountCol, mTotalPriceCol, mTotalWeightCol);
      
      String totalData = calcText[5].substring(calcText[5].indexOf("\n\n") + 2);
      TableColumn<PriceLine, String> totalNameCol = new TableColumn<>(headers[0]);
      totalNameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
      totalNameCol.setMinWidth(300.0);
      totalNameCol.setMaxWidth(300.0);
      TableColumn<PriceLine, String> totalIdCol = new TableColumn<>(headers[1]);
      totalIdCol.setCellValueFactory(cellData -> cellData.getValue().getId());
      totalIdCol.setMinWidth(150.0);
      totalIdCol.setMaxWidth(150.0);
      TableColumn<PriceLine, String> totalAmountCol = new TableColumn<>(headers[2]);
      totalAmountCol.setCellValueFactory(cellData -> cellData.getValue().getAmount());
      totalAmountCol.setMinWidth(75.0);
      totalAmountCol.setMaxWidth(75.0);
      TableColumn<PriceLine, String> totalPriceCol = new TableColumn<>(headers[3]);
      totalPriceCol.setCellValueFactory(cellData -> cellData.getValue().getPrice());
      totalPriceCol.setMinWidth(150.0);
      totalPriceCol.setMaxWidth(150.0);
      TableColumn<PriceLine, String> totalWeightCol = new TableColumn<>(headers[4]);
      totalWeightCol.setCellValueFactory(cellData -> cellData.getValue().getWeight());
      totalWeightCol.setMinWidth(200.0);
      totalWeightCol.setMaxWidth(200.0);
      
      ObservableList<PriceLine> totalDataList = FXCollections.observableArrayList();
      
      totalData = totalData.substring(totalData.indexOf("\n") + 1);
      for(String row : totalData.split("\n"))
      {
        String[] data = row.split("\t");
        totalDataList.add(new PriceLine(data[0], data[1], data[2], data[3], data[4]));
      }
      
      Totals.setRowFactory(row -> new TableRow<PriceLine>()
      {
        @Override
        public void updateIndex(int index)
        {
          super.updateIndex(index);
          if(index == Totals.getItems().size() - 1)
          {
            getStyleClass().add("table-bold");
          }
          else
          {
            getStyleClass().remove("table-bold");
          }
        }
      });
      Totals.setItems(totalDataList);
      Totals.getColumns().clear();
      Totals.getColumns().addAll(totalNameCol, totalIdCol, totalAmountCol, totalPriceCol, totalWeightCol);
      
      ElectSubTotals.getStyleClass().add("table-view-hidden");
      ElectSubTotals.getStyleClass().add("table-bold");
      ElectSubTotals.fixedCellSizeProperty().set(20.0);
      MechaSubTotals.getStyleClass().add("table-view-hidden");
      MechaSubTotals.getStyleClass().add("table-bold");
      MechaSubTotals.fixedCellSizeProperty().set(20.0);
      Totals.getStyleClass().add("table-view-hidden");
      
      Pane eHeader = (Pane) ElectSubTotals.lookup("TableHeaderRow");
      eHeader.setVisible(false);
      ElectSubTotals.setLayoutY(-eHeader.getHeight());
      ElectSubTotals.autosize();
      ElectSubTotals.scrollTo(0);
      
      Pane mHeader = (Pane) MechaSubTotals.lookup("TableHeaderRow");
      mHeader.setVisible(false);
      MechaSubTotals.setLayoutY(-mHeader.getHeight());
      MechaSubTotals.autosize();
      MechaSubTotals.scrollTo(0);
      
      Pane tHeader = (Pane) Totals.lookup("TableHeaderRow");
      tHeader.setVisible(false);
      Totals.setLayoutY(-tHeader.getHeight());
      Totals.autosize();
    }
    catch(CISException e)
    {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setHeaderText(e.getMessage());
      alert.show();
    }
    catch(ParseException | NullPointerException e)
    {
    }
  }
  
  public void print()
  {
    String calc = CIS_DATA.getVersionHeader() + "\n\t\n" + CIS_DATA.createCalculation();
    Stage printStage = new Stage();
    GridPane printPane = new GridPane();
    printPane.getStylesheets().add("/de/tichawa/cis/config/style.css");
    printPane.getStyleClass().add("white");
    Scene printScene = new Scene(printPane);
    
    int x = 0;
    for(String row : calc.split("\n"))
    {
      int y = 0;
      
      if(row.isEmpty()) {
        continue;
      }
      
      if(row.equals("\t"))
      {
        row = " ";
      }
      
      for(String data : row.split("\t"))
      {
        if(!data.isEmpty())
        {
          StackPane p = new StackPane();
          p.setMinHeight(20.0);
          p.setMaxHeight(20.0);
          p.setPadding(new Insets(2, 2, 2, 2));
          p.setAlignment(Pos.CENTER_LEFT);
          p.getChildren().add(new Label(data));
          p.getStyleClass().add("bolder");
          if(x % 2 == 0)
          {
            p.getStyleClass().add("pale-blue");
          }
          else
          {
            p.getStyleClass().add("white");
          }
          
          printPane.add(p, y, x);
          y++;
        }
      }
      x++;
    }
    
    printStage.setScene(printScene);
    printStage.show();
    
    PrinterJob p = PrinterJob.createPrinterJob();
    
    if(p.showPrintDialog(null))
    {
      p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.EQUAL_OPPOSITES));
      double scaleX = p.getJobSettings().getPageLayout().getPrintableWidth() / printPane.getWidth();
      double scaleY = p.getJobSettings().getPageLayout().getPrintableHeight() / (getRowCount(printPane) * 20.0);

      printPane.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));
      
      if(p.printPage(printPane))
      {
        printPane.getTransforms().clear();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("printsuccess"));
        alert.show();
        p.endJob();
      }
      else
      {
        printPane.getTransforms().clear();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", CIS_DATA.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings."));
        alert.show();
      }
    }
    
    printStage.close();
  }
  
  public static class CalcLine
  {
    private final SimpleStringProperty name;
    private final SimpleStringProperty id;
    private final SimpleIntegerProperty amount;
    private final SimpleDoubleProperty price;
    private final SimpleDoubleProperty weight;
    private final SimpleDoubleProperty time;
    private final SimpleDoubleProperty power;
    
    public CalcLine(String name, String id, int amount, double price, double weight, double time, double power)
    {
      this.name = new SimpleStringProperty(name);
      this.id = new SimpleStringProperty(id);
      this.amount = new SimpleIntegerProperty(amount);
      this.price = new SimpleDoubleProperty(price);
      this.weight = new SimpleDoubleProperty(weight);
      this.time = new SimpleDoubleProperty(time);
      this.power = new SimpleDoubleProperty(power);
    }
    
    public SimpleStringProperty getName()
    {
      return name;
    }
    
    public SimpleStringProperty getId()
    {
      return id;
    }
    
    public SimpleIntegerProperty getAmount()
    {
      return amount;
    }
    
    public SimpleDoubleProperty getPrice()
    {
      return price;
    }
    
    public SimpleDoubleProperty getWeight()
    {
      return weight;
    }
    
    public SimpleDoubleProperty getTime()
    {
      return time;
    }
    
    public SimpleDoubleProperty getPower()
    {
      return power;
    }
    
  }
  
  public static class PriceLine
  {
    private final SimpleStringProperty name;
    private final SimpleStringProperty id;
    private final SimpleStringProperty amount;
    private final SimpleStringProperty price;
    private final SimpleStringProperty weight;
    
    public PriceLine(String name, String id, String amount, String price, String weight)
    {
      this.name = new SimpleStringProperty(name);
      this.id = new SimpleStringProperty(id);
      this.amount = new SimpleStringProperty(amount);
      this.price = new SimpleStringProperty(price);
      this.weight = new SimpleStringProperty(weight);
    }
    
    public SimpleStringProperty getName()
    {
      return name;
    }
    
    public SimpleStringProperty getId()
    {
      return id;
    }
    
    public SimpleStringProperty getAmount()
    {
      return amount;
    }
    
    public SimpleStringProperty getPrice()
    {
      return price;
    }
    
    public SimpleStringProperty getWeight()
    {
      return weight;
    }
  }
  
  private class CustomIntCell extends TableCell<CalcLine, Integer>
  {
    @Override
    public void updateItem(final Integer item, boolean empty)
    {
      if(item != null)
      {
        setText(String.format(CIS_DATA.getLocale(), "%d", item));
      }
    }
  }
  
  private class CustomDoubleCell extends TableCell<CalcLine, Double>
  {
    @Override
    public void updateItem(final Double item, boolean empty)
    {
      if(item != null)
      {
        setText(String.format(CIS_DATA.getLocale(), "%.2f", item));
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
  
  private int getRowCount(GridPane pane)
  {
    int numRows = pane.getRowConstraints().size();
    for(int i = 0; i < pane.getChildren().size(); i++)
    {
      Node child = pane.getChildren().get(i);
      if (child.isManaged())
      {
        Integer rowIndex = GridPane.getRowIndex(child);
        if(rowIndex != null)
        {
          numRows = Math.max(numRows, rowIndex + 1);
        }
      }
    }
    return numRows;
  }
}
