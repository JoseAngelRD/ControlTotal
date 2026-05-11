package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private Label badgeEmpresas;
    @FXML private Label badgePersonas;

    @FXML private HBox navDashboard;
    @FXML private HBox navEmpresas;
    @FXML private HBox navPersonas;
    @FXML private HBox navBuscador;

    @FXML private StackPane contentArea;

    @FXML private ImageView topbarLogo;
    @FXML private ImageView sidebarLogo;

    private final Map<String, Node>   viewCache       = new HashMap<>();
    private final Map<String, Object> controllerCache = new HashMap<>();
    private       HBox                currentNavItem  = null;
    private       boolean             isAnimating     = false;

    private static final Map<String, String> VIEW_FXML = Map.of(
        "dashboard", "/com/example/controltotal_proyecto/fxml/views/DashboardView.fxml",
        "empresas",  "/com/example/controltotal_proyecto/fxml/views/EmpresasView.fxml",
        "personas",  "/com/example/controltotal_proyecto/fxml/views/PersonasView.fxml",
        "buscador",  "/com/example/controltotal_proyecto/fxml/views/BuscadorView.fxml"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Cargar logo programáticamente (evita problemas de classpath en FXML)
        try {
            InputStream is = getClass().getResourceAsStream(
                "/com/example/controltotal_proyecto/images/uninc_logo.png"
            );
            if (is != null) {
                Image logo = new Image(is);
                topbarLogo.setImage(logo);
                sidebarLogo.setImage(logo);
            } else {
                System.err.println("Logo no encontrado en el classpath");
            }
        } catch (Exception e) {
            System.err.println("Error cargando logo: " + e.getMessage());
        }

        navigateTo("dashboard", navDashboard);
        refreshStats();
    }

    @FXML private void onNavDashboard() { navigateTo("dashboard", navDashboard); }
    @FXML private void onNavEmpresas()  { navigateTo("empresas",  navEmpresas);  }
    @FXML private void onNavPersonas()  { navigateTo("personas",  navPersonas);  }
    @FXML private void onNavBuscador()  { navigateTo("buscador",  navBuscador);  }

    private void navigateTo(String viewKey, HBox navItem) {
        if (isAnimating) return;
        if (navItem == currentNavItem) return;

        if (currentNavItem != null) currentNavItem.getStyleClass().remove("nav-item-active");
        navItem.getStyleClass().add("nav-item-active");
        currentNavItem = navItem;

        Node newView = viewCache.computeIfAbsent(viewKey, k -> loadView(k));
        if (newView == null) return;

        Object ctrl = controllerCache.get(viewKey);
        if (ctrl instanceof RefreshableController rc) {
            rc.onViewActivated();
        }

        if (contentArea.getChildren().isEmpty()) {
            newView.setOpacity(0);
            contentArea.getChildren().setAll(newView);
            FadeTransition ft = new FadeTransition(Duration.millis(300), newView);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        } else {
            Node oldView = contentArea.getChildren().get(0);
            isAnimating = true;

            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), oldView);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(150), oldView);
            slideOut.setFromX(0);
            slideOut.setToX(-15);

            ParallelTransition out = new ParallelTransition(fadeOut, slideOut);
            out.setOnFinished(e -> {
                newView.setOpacity(0);
                newView.setTranslateX(15);
                contentArea.getChildren().setAll(newView);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), newView);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), newView);
                slideIn.setFromX(15);
                slideIn.setToX(0);
                slideIn.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition in = new ParallelTransition(fadeIn, slideIn);
                in.setOnFinished(ev -> isAnimating = false);
                in.play();
            });

            out.play();
        }

        refreshStats();
    }

    private Node loadView(String key) {
        String fxmlPath = VIEW_FXML.get(key);
        if (fxmlPath == null) return null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();

            if (node instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }

            Object ctrl = loader.getController();
            controllerCache.put(key, ctrl);

            if (ctrl instanceof ChildController cc) {
                cc.setMainController(this);
            }
            return node;
        } catch (IOException e) {
            System.err.println("❌ No se pudo cargar la vista: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    public void refreshStats() {
        List<Empresa> emps = DatabaseManager.obtenerTodasLasEmpresas();
        List<Persona> pers = DatabaseManager.obtenerTodasLasPersonas();
        badgeEmpresas.setText(String.valueOf(emps.size()));
        badgePersonas.setText(String.valueOf(pers.size()));
    }

    public interface RefreshableController {
        void onViewActivated();
    }

    public interface ChildController {
        void setMainController(MainController main);
    }
}
