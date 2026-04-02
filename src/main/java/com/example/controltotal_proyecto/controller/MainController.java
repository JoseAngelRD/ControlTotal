package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador raíz del MainLayout.fxml.
 * Gestiona la navegación entre vistas (Dashboard, Empresas, Personas, Buscador)
 * y mantiene las referencias a los subcontroladores para compartir datos.
 */
public class MainController implements Initializable {

    // ─── fx:id de TopBar ──────────────────────────────────────────────────────
    @FXML private Label badgeEmpresas;
    @FXML private Label badgePersonas;

    // ─── fx:id de Sidebar — stats ─────────────────────────────────────────────
    @FXML private Label statActivas;
    @FXML private Label statPersonas;
    @FXML private Label statRelaciones;

    // ─── fx:id de Sidebar — nav items ────────────────────────────────────────
    @FXML private HBox navDashboard;
    @FXML private HBox navEmpresas;
    @FXML private HBox navPersonas;
    @FXML private HBox navBuscador;

    // ─── Contenedor central ───────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    // ─── Cache de vistas ya cargadas ─────────────────────────────────────────
    private final Map<String, Node>       viewCache       = new HashMap<>();
    private final Map<String, Object>     controllerCache = new HashMap<>();
    private       HBox                    currentNavItem  = null;

    // ─── Views disponibles ───────────────────────────────────────────────────
    private static final Map<String, String> VIEW_FXML = Map.of(
        "dashboard", "/com/example/controltotal_proyecto/fxml/views/DashboardView.fxml",
        "empresas",  "/com/example/controltotal_proyecto/fxml/views/EmpresasView.fxml",
        "personas",  "/com/example/controltotal_proyecto/fxml/views/PersonasView.fxml",
        "buscador",  "/com/example/controltotal_proyecto/fxml/views/BuscadorView.fxml"
    );

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navigateTo("dashboard", navDashboard);
        refreshStats();
    }

    // ─── Navegación ───────────────────────────────────────────────────────────

    @FXML private void onNavDashboard() { navigateTo("dashboard", navDashboard); }
    @FXML private void onNavEmpresas()  { navigateTo("empresas",  navEmpresas);  }
    @FXML private void onNavPersonas()  { navigateTo("personas",  navPersonas);  }
    @FXML private void onNavBuscador()  { navigateTo("buscador",  navBuscador);  }

    private void navigateTo(String viewKey, HBox navItem) {
        // Actualizar estado visual del ítem activo
        if (currentNavItem != null) currentNavItem.getStyleClass().remove("nav-item-active");
        navItem.getStyleClass().add("nav-item-active");
        currentNavItem = navItem;

        // Cargar vista (desde caché si ya existe)
        Node view = viewCache.computeIfAbsent(viewKey, k -> loadView(k));
        if (view == null) return;

        // Mostrar la vista en el contenedor central
        contentArea.getChildren().setAll(view);

        // Notificar al controlador de la vista que ha sido activada
        Object ctrl = controllerCache.get(viewKey);
        if (ctrl instanceof RefreshableController rc) {
            rc.onViewActivated();
        }

        refreshStats();
    }

    private Node loadView(String key) {
        String fxmlPath = VIEW_FXML.get(key);
        if (fxmlPath == null) return null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            Object ctrl = loader.getController();
            controllerCache.put(key, ctrl);

            // Inyectar referencia al MainController para que los sub-controladores
            // puedan llamar a refreshStats()
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

    // ─── Estadísticas ─────────────────────────────────────────────────────────

    public void refreshStats() {
        List<Empresa>  emps  = DatabaseManager.obtenerTodasLasEmpresas();
        List<Persona>  pers  = DatabaseManager.obtenerTodasLasPersonas();

        long activas    = emps.stream().filter(Empresa::isActivo).count();
        long relaciones = emps.stream()
            .flatMap(e -> DatabaseManager.obtenerPersonasDeEmpresa(e.getNifCif()).stream())
            .distinct().count();

        badgeEmpresas.setText(String.valueOf(emps.size()));
        badgePersonas.setText(String.valueOf(pers.size()));
        statActivas.setText(String.valueOf(activas));
        statPersonas.setText(String.valueOf(pers.size()));
        statRelaciones.setText(String.valueOf(relaciones));
    }

    // ─── Interfaces para subcontroladores ────────────────────────────────────

    /** Los controladores de vista implementan esta interfaz para recibir notificaciones. */
    public interface RefreshableController {
        void onViewActivated();
    }

    /** Los controladores hijo implementan esta interfaz para acceder al MainController. */
    public interface ChildController {
        void setMainController(MainController main);
    }
}
