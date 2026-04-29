package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.ReferralHit;
import service.ReferralHitService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReferralHitController implements Initializable {

    @FXML private TableColumn<ReferralHit, LocalDateTime> colDate;
    @FXML private TableColumn<ReferralHit, String> colInfluencer;
    @FXML private TableColumn<ReferralHit, String> colReferred;
    @FXML private TableColumn<ReferralHit, String> colSession;
    @FXML private TableView<ReferralHit> tableHits;

    @FXML private Label lblConversions;
    @FXML private Label lblTotalHits;
    @FXML private Label lblGlobalCTR;
    @FXML private BarChart<String, Number> hitChart;
    @FXML private PieChart typePieChart;
    @FXML private TextField txtSearch;

    private final ReferralHitService referralHitService = new ReferralHitService();
    private final ObservableList<ReferralHit> hitsList = FXCollections.observableArrayList();
    private FilteredList<ReferralHit> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colInfluencer.setCellValueFactory(new PropertyValueFactory<>("collaborationTitle"));
        colSession.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitedAt"));
        colReferred.setCellValueFactory(new PropertyValueFactory<>("referredUserEmail"));

        // Setup FilteredList
        filteredList = new FilteredList<>(hitsList, p -> true);
        tableHits.setItems(filteredList);

        // Search logic
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredList.setPredicate(hit -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String low = newValue.toLowerCase();
                return hit.getCollaborationTitle().toLowerCase().contains(low) ||
                       hit.getSessionId().toLowerCase().contains(low) ||
                       (hit.getReferredUserEmail() != null && hit.getReferredUserEmail().toLowerCase().contains(low));
            });
            updateStats();
        });

        // Custom cell factory for Type coloring
        colSession.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    if ("click".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #03DAC6; -fx-font-weight: bold;");
                    } else if ("impression".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #BB86FC;");
                    } else {
                        setStyle("-fx-text-fill: #6366F1;");
                    }
                }
            }
        });

        loadData();
    }

    @FXML
    void handleRefresh() {
        loadData();
    }

    private void loadData() {
        hitsList.setAll(referralHitService.recuperer());
        updateStats();
        updateCharts();
    }

    private void updateStats() {
        // Use filteredList for dynamic stats during search
        int total = filteredList.size();
        lblTotalHits.setText(String.valueOf(total));
        
        long conversions = filteredList.stream()
                .filter(hit -> hit.getReferredUserId() != null && hit.getReferredUserId() > 0)
                .count();
        lblConversions.setText(String.valueOf(conversions));

        long clicks = filteredList.stream().filter(h -> "click".equalsIgnoreCase(h.getSessionId())).count();
        long impressions = filteredList.stream().filter(h -> "impression".equalsIgnoreCase(h.getSessionId())).count();
        
        double ctr = (impressions > 0) ? ((double) clicks / impressions) * 100.0 : 0.0;
        lblGlobalCTR.setText(String.format("%.2f%%", ctr));
    }

    private void updateCharts() {
        updateBarChart();
        updatePieChart();
    }

    private void updateBarChart() {
        hitChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Hits per Day");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        
        Map<java.time.LocalDate, Long> counts = hitsList.stream()
                .filter(h -> h.getVisitedAt() != null)
                .collect(Collectors.groupingBy(
                        h -> h.getVisitedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.counting()
                ));

        counts.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date.format(fmt), count)));
        hitChart.getData().add(series);
    }

    private void updatePieChart() {
        typePieChart.getData().clear();
        long clicks = hitsList.stream().filter(h -> "click".equalsIgnoreCase(h.getSessionId())).count();
        long impressions = hitsList.stream().filter(h -> "impression".equalsIgnoreCase(h.getSessionId())).count();
        long others = hitsList.stream().filter(h -> !"click".equalsIgnoreCase(h.getSessionId()) && !"impression".equalsIgnoreCase(h.getSessionId())).count();

        if (impressions > 0) typePieChart.getData().add(new PieChart.Data("Impressions (" + impressions + ")", impressions));
        if (clicks > 0) typePieChart.getData().add(new PieChart.Data("Clicks (" + clicks + ")", clicks));
        if (others > 0) typePieChart.getData().add(new PieChart.Data("Other (" + others + ")", others));
    }
}
