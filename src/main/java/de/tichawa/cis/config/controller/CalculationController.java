package de.tichawa.cis.config.controller;

import de.tichawa.cis.config.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.print.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.net.URL;
import java.text.*;
import java.util.ResourceBundle;

public class CalculationController implements Initializable {

    protected CIS CIS_DATA;

    @FXML
    private Label headerLabel;
    @FXML
    private TableView<CalcLine> electronicsTableView;
    @FXML
    private TableView<CalcLine> electronicsSubTotalsTableView;
    @FXML
    private TableView<CalcLine> mechanicsTableView;
    @FXML
    private TableView<CalcLine> mechanicsSubTotalsTableView;
    @FXML
    private TableView<PriceLine> totalsTableView;
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem printMenuItem;
    @FXML
    private Menu languageMenu;
    @FXML
    private MenuItem switchLanguageMenuItem;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void passData(CIS data) {
        this.CIS_DATA = data.copy();
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        try {
            languageMenu.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("lang"));
            switchLanguageMenuItem.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("switchlang"));

            fileMenu.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("File"));
            printMenuItem.setText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("Print"));

            NumberFormat f = NumberFormat.getInstance(Util.getLocale());
            String[] calcText = CIS_DATA.createCalculation().split("\n\t\n");

            headerLabel.setText(calcText[0]);

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
            for (String row : electData.split("\n")) {
                String[] data = row.split("\t");
                electDataList.add(new CalcLine(data[0], data[1], f.parse(data[2]).intValue(), f.parse(data[3]).doubleValue(), f.parse(data[4]).doubleValue(), f.parse(data[5]).doubleValue(), f.parse(data[6]).doubleValue()));
            }

            electronicsTableView.setItems(electDataList);
            electronicsTableView.getColumns().clear();
            electronicsTableView.getColumns().addAll(electNameCol, electIdCol, electAmountCol, electPriceCol, electWeightCol, electTimeCol, electPowerCol);
            electronicsTableView.getSortOrder().add(electNameCol);

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
            electronicsSubTotalsTableView.getItems().clear();
            electronicsSubTotalsTableView.getItems().add(new CalcLine(eData[0], eData[1], f.parse(eData[2]).intValue(), f.parse(eData[3]).doubleValue(), f.parse(eData[4]).doubleValue(), f.parse(eData[5]).doubleValue(), f.parse(eData[6]).doubleValue()));
            electronicsSubTotalsTableView.getColumns().clear();
            electronicsSubTotalsTableView.getColumns().addAll(eTotalNameCol, eTotalIdCol, eTotalAmountCol, eTotalPriceCol, eTotalWeightCol, eTotalTimeCol, eTotalPowerCol);

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
            for (String row : mechaData.split("\n")) {
                String[] data = row.split("\t");
                mechaDataList.add(new CalcLine(data[0], data[1], f.parse(data[2]).intValue(), f.parse(data[3]).doubleValue(), f.parse(data[4]).doubleValue(), 0.0, 0.0));
            }

            mechanicsTableView.setItems(mechaDataList);
            mechanicsTableView.getColumns().clear();
            mechanicsTableView.getColumns().addAll(mechaNameCol, mechaIdCol, mechaAmountCol, mechaPriceCol, mechaWeightCol);
            mechanicsTableView.getSortOrder().add(mechaNameCol);

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
            mechanicsSubTotalsTableView.getItems().clear();
            mechanicsSubTotalsTableView.getItems().add(new CalcLine(mData[0], mData[1], f.parse(mData[2]).intValue(), f.parse(mData[3]).doubleValue(), f.parse(mData[4]).doubleValue(), 0.0, 0.0));
            mechanicsSubTotalsTableView.getColumns().clear();
            mechanicsSubTotalsTableView.getColumns().addAll(mTotalNameCol, mTotalIdCol, mTotalAmountCol, mTotalPriceCol, mTotalWeightCol);

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
            for (String row : totalData.split("\n")) {
                String[] data = row.split("\t");
                totalDataList.add(new PriceLine(data[0], data[1], data[2], data[3], data[4]));
            }

            totalsTableView.setRowFactory(row -> new TableRow<PriceLine>() {
                @Override
                public void updateIndex(int index) {
                    super.updateIndex(index);
                    if (index == totalsTableView.getItems().size() - 1) {
                        getStyleClass().add("table-bold");
                    } else {
                        getStyleClass().remove("table-bold");
                    }
                }
            });
            totalsTableView.setItems(totalDataList);
            totalsTableView.getColumns().clear();
            totalsTableView.getColumns().addAll(totalNameCol, totalIdCol, totalAmountCol, totalPriceCol, totalWeightCol);

            electronicsSubTotalsTableView.getStyleClass().add("table-view-hidden");
            electronicsSubTotalsTableView.getStyleClass().add("table-bold");
            electronicsSubTotalsTableView.fixedCellSizeProperty().set(20.0);
            mechanicsSubTotalsTableView.getStyleClass().add("table-view-hidden");
            mechanicsSubTotalsTableView.getStyleClass().add("table-bold");
            mechanicsSubTotalsTableView.fixedCellSizeProperty().set(20.0);
            totalsTableView.getStyleClass().add("table-view-hidden");

            Pane eHeader = (Pane) electronicsSubTotalsTableView.lookup("TableHeaderRow");
            eHeader.setVisible(false);
            electronicsSubTotalsTableView.setLayoutY(-eHeader.getHeight());
            electronicsSubTotalsTableView.autosize();
            electronicsSubTotalsTableView.scrollTo(0);

            Pane mHeader = (Pane) mechanicsSubTotalsTableView.lookup("TableHeaderRow");
            mHeader.setVisible(false);
            mechanicsSubTotalsTableView.setLayoutY(-mHeader.getHeight());
            mechanicsSubTotalsTableView.autosize();
            mechanicsSubTotalsTableView.scrollTo(0);

            Pane tHeader = (Pane) totalsTableView.lookup("TableHeaderRow");
            tHeader.setVisible(false);
            totalsTableView.setLayoutY(-tHeader.getHeight());
            totalsTableView.autosize();
        } catch (CISException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(e.getMessage());
            alert.show();
        } catch (ParseException | NullPointerException ignored) {
        }
    }

    public void print() {
        String calc = CIS_DATA.getVersionHeader() + "\n\t\n" + CIS_DATA.createCalculation();
        Stage printStage = new Stage();
        GridPane printPane = new GridPane();
        printPane.getStylesheets().add("/de/tichawa/cis/config/style.css");
        printPane.getStyleClass().add("white");
        Scene printScene = new Scene(printPane);

        int x = 0;
        for (String row : calc.split("\n")) {
            int y = 0;

            if (row.isEmpty()) {
                continue;
            }

            if (row.equals("\t")) {
                row = " ";
            }

            for (String data : row.split("\t")) {
                if (!data.isEmpty()) {
                    StackPane p = new StackPane();
                    p.setMinHeight(20.0);
                    p.setMaxHeight(20.0);
                    p.setPadding(new Insets(2, 2, 2, 2));
                    p.setAlignment(Pos.CENTER_LEFT);
                    p.getChildren().add(new Label(data));
                    p.getStyleClass().add("bolder");
                    if (x % 2 == 0) {
                        p.getStyleClass().add("pale-blue");
                    } else {
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

        if (p.showPrintDialog(null)) {
            p.getJobSettings().setPageLayout(p.getPrinter().createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.EQUAL_OPPOSITES));
            double scaleX = p.getJobSettings().getPageLayout().getPrintableWidth() / printPane.getWidth();
            double scaleY = p.getJobSettings().getPageLayout().getPrintableHeight() / (getRowCount(printPane) * 20.0);

            printPane.getTransforms().add(new Scale(Math.min(scaleX, scaleY), Math.min(scaleX, scaleY)));

            if (p.printPage(printPane)) {
                printPane.getTransforms().clear();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("printsuccess"));
                alert.show();
                p.endJob();
            } else {
                printPane.getTransforms().clear();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText(ResourceBundle.getBundle("de.tichawa.cis.config.Bundle", Util.getLocale()).getString("A fatal error occurred during the printing attempt.Please control your print settings."));
                alert.show();
            }
        }

        printStage.close();
    }

    public static class CalcLine {
        private final SimpleStringProperty name;
        private final SimpleStringProperty id;
        private final SimpleIntegerProperty amount;
        private final SimpleDoubleProperty price;
        private final SimpleDoubleProperty weight;
        private final SimpleDoubleProperty time;
        private final SimpleDoubleProperty power;

        public CalcLine(String name, String id, int amount, double price, double weight, double time, double power) {
            this.name = new SimpleStringProperty(name);
            this.id = new SimpleStringProperty(id);
            this.amount = new SimpleIntegerProperty(amount);
            this.price = new SimpleDoubleProperty(price);
            this.weight = new SimpleDoubleProperty(weight);
            this.time = new SimpleDoubleProperty(time);
            this.power = new SimpleDoubleProperty(power);
        }

        public SimpleStringProperty getName() {
            return name;
        }

        public SimpleStringProperty getId() {
            return id;
        }

        public SimpleIntegerProperty getAmount() {
            return amount;
        }

        public SimpleDoubleProperty getPrice() {
            return price;
        }

        public SimpleDoubleProperty getWeight() {
            return weight;
        }

        public SimpleDoubleProperty getTime() {
            return time;
        }

        public SimpleDoubleProperty getPower() {
            return power;
        }

    }

    public static class PriceLine {
        private final SimpleStringProperty name;
        private final SimpleStringProperty id;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty price;
        private final SimpleStringProperty weight;

        public PriceLine(String name, String id, String amount, String price, String weight) {
            this.name = new SimpleStringProperty(name);
            this.id = new SimpleStringProperty(id);
            this.amount = new SimpleStringProperty(amount);
            this.price = new SimpleStringProperty(price);
            this.weight = new SimpleStringProperty(weight);
        }

        public SimpleStringProperty getName() {
            return name;
        }

        public SimpleStringProperty getId() {
            return id;
        }

        public SimpleStringProperty getAmount() {
            return amount;
        }

        public SimpleStringProperty getPrice() {
            return price;
        }

        public SimpleStringProperty getWeight() {
            return weight;
        }
    }

    private static class CustomIntCell extends TableCell<CalcLine, Integer> {
        @Override
        public void updateItem(final Integer item, boolean empty) {
            if (item != null) {
                setText(String.format(Util.getLocale(), "%d", item));
            }
        }
    }

    private static class CustomDoubleCell extends TableCell<CalcLine, Double> {
        @Override
        public void updateItem(final Double item, boolean empty) {
            if (item != null) {
                setText(String.format(Util.getLocale(), "%.2f", item));
            }
        }
    }

    public void switchLang() {
        Util.switchLanguage();
        load();
    }

    private int getRowCount(GridPane pane) {
        int numRows = pane.getRowConstraints().size();
        for (int i = 0; i < pane.getChildren().size(); i++) {
            Node child = pane.getChildren().get(i);
            if (child.isManaged()) {
                Integer rowIndex = GridPane.getRowIndex(child);
                if (rowIndex != null) {
                    numRows = Math.max(numRows, rowIndex + 1);
                }
            }
        }
        return numRows;
    }
}