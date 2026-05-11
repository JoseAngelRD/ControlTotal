package com.example.controltotal_proyecto.controller;

import com.example.controltotal_proyecto.bd.DatabaseManager;
import com.example.controltotal_proyecto.entities.Empresa;
import com.example.controltotal_proyecto.entities.Persona;
import com.example.controltotal_proyecto.service.EmpresaService;
import com.example.controltotal_proyecto.util.BadgeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la vista "Empresas".
 *
 * CAMBIOS respecto a la versión anterior:
 *  - Botón "Ver Contacto" (amarillo) a la derecha de "Abrir Directorios".
 *  - Panel inline de contacto que muestra Nombre, Apellidos, NIF, Móvil y Correo.
 *  - Monograma con fuente adaptativa (no trunca abreviaturas de hasta 6 chars).
 *  - Etiquetas de campo sin "..." (OverrunStyle → clip silencioso).
 *  - rightArea con ancho mínimo protegido para que los botones nunca se superpongan.
 */
public class EmpresasController implements Initializable,
        MainController.RefreshableController, MainController.ChildController {

    // ─── Vistas (StackPane layers) ────────────────────────────────────────────
    @FXML private VBox vistaLista;
    @FXML private VBox vistaFormulario;

    // ─── Contenedor de tarjetas ───────────────────────────────────────────────
    @FXML private VBox listContainer;
    @FXML private VBox lblEmpty;

    // ─── Filtros y búsqueda ───────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ToggleButton     chipTodos;
    @FXML private ToggleButton     chipActivo;
    @FXML private ToggleButton     chipPasivo;
    @FXML private ComboBox<String> comboServicio;
    @FXML private ComboBox<String> comboDelegacion;
    @FXML private ComboBox<String> comboAgente;

    // ─── Datos ────────────────────────────────────────────────────────────────
    private final EmpresaService          service    = new EmpresaService();
    private final ObservableList<Empresa> masterList = FXCollections.observableArrayList();
    private       FilteredList<Empresa>   filteredList;
    private       MainController          mainController;

    // ─── Init ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        vistaLista.setPrefWidth(Double.MAX_VALUE);
        vistaLista.setPrefHeight(Double.MAX_VALUE);
        vistaFormulario.setPrefWidth(Double.MAX_VALUE);
        vistaFormulario.setPrefHeight(Double.MAX_VALUE);

        configurarFiltros();
        cargarDatos();
    }

    @Override public void onViewActivated()                       { cargarDatos(); }
    @Override public void setMainController(MainController main)  { this.mainController = main; }

    // ─── Configurar filtros ───────────────────────────────────────────────────

    private void configurarFiltros() {
        ToggleGroup tg = new ToggleGroup();
        chipTodos.setToggleGroup(tg);
        chipActivo.setToggleGroup(tg);
        chipPasivo.setToggleGroup(tg);
        chipTodos.setSelected(true);
        tg.selectedToggleProperty().addListener((obs, o, n) -> renderCards());

        comboServicio.getItems().add("Todos");
        comboServicio.getItems().addAll(service.getServicios());
        comboServicio.setValue("Todos");
        comboServicio.setOnAction(e -> renderCards());

        comboDelegacion.getItems().add("Todas");
        comboDelegacion.getItems().addAll(service.getDelegaciones());
        comboDelegacion.setValue("Todas");
        comboDelegacion.setOnAction(e -> renderCards());

        comboAgente.getItems().add("Todos");
        comboAgente.getItems().addAll(service.getAgentes());
        comboAgente.setValue("Todos");
        comboAgente.setOnAction(e -> renderCards());

        searchField.textProperty().addListener((obs, o, n) -> renderCards());
    }

    // ─── Carga y renderizado ──────────────────────────────────────────────────

    private void cargarDatos() {
        masterList.setAll(service.obtenerTodas());
        filteredList = new FilteredList<>(masterList, p -> true);
        renderCards();
    }

    private void renderCards() {
        if (filteredList == null) return;
        aplicarFiltros();
        List<Empresa> visible = filteredList.stream().collect(Collectors.toList());

        listContainer.getChildren().clear();
        for (Empresa e : visible) {
            listContainer.getChildren().add(crearTarjeta(e));
        }
        actualizarEstadoVacio(visible.isEmpty());
    }

    private void aplicarFiltros() {
        filteredList.setPredicate(e -> {
            String q = searchField.getText().toLowerCase();
            boolean matchQ = q.isBlank()
                    || safe(e.getDenominacionSocial()).contains(q)
                    || safe(e.getNifCif()).contains(q)
                    || safe(e.getAbreviatura()).contains(q);

            ToggleButton sel = (ToggleButton) chipTodos.getToggleGroup().getSelectedToggle();
            boolean matchE = sel == null || sel == chipTodos
                    || (sel == chipActivo && e.isActivo())
                    || (sel == chipPasivo && !e.isActivo());

            String sv = comboServicio.getValue();
            boolean matchS = sv == null || sv.equals("Todos") || sv.equals(e.getServicio());

            String dv = comboDelegacion.getValue();
            boolean matchD = dv == null || dv.equals("Todas") || dv.equals(e.getDelegacion());

            String av = comboAgente.getValue();
            boolean matchA = av == null || av.equals("Todos") || av.equals(e.getAgenteContable());

            return matchQ && matchE && matchS && matchD && matchA;
        });
    }

    private void actualizarEstadoVacio(boolean vacio) {
        listContainer.setVisible(!vacio);
        lblEmpty.setVisible(vacio);
    }

    private String safe(String s) { return s != null ? s.toLowerCase() : ""; }

    // ─── Construcción de tarjeta ──────────────────────────────────────────────

    /**
     * Construye la tarjeta visual de una empresa.
     * [Monograma] [Denominación | Forma | NIF | Servicio | Delegación | Agente]
     *             [Estado] [Modificar] [Abrir Directorios] [Ver Contacto]
     */
    private Node crearTarjeta(Empresa e) {

        /* ── Monograma con fuente adaptativa (hasta 6 chars sin "...") ── */
        String abr = (e.getAbreviatura() != null && !e.getAbreviatura().isBlank())
                ? e.getAbreviatura().substring(0, Math.min(6, e.getAbreviatura().length()))
                : iniciales(e.getDenominacionSocial());

        Label monogram = new Label(abr);
        monogram.getStyleClass().add("card-monogram");
        monogram.setMinWidth(82);
        monogram.setPrefWidth(82);
        monogram.setMaxWidth(82);
        monogram.setMinHeight(64);
        monogram.setMaxHeight(64);
        monogram.setAlignment(Pos.CENTER);
        monogram.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);

        // Adaptar tamaño de fuente según longitud para que quepa sin truncar
        int len = abr.length();
        String fontSize = len <= 2 ? "26px" : len <= 4 ? "20px" : "15px";
        monogram.setStyle("-fx-font-size: " + fontSize + ";");

        /* ── Columnas de info ── */
        VBox colDenom = campoCard("Denominación Social",
                e.getDenominacionSocial(), "card-field-value-bold");
        colDenom.setMinWidth(130);

        VBox colFS = campoCard("Forma Social",
                nvl(e.getFormaSocial()), "card-field-value");
        colFS.setMinWidth(90);

        VBox colNif = campoCard("NIF/CIF",
                nvl(e.getNifCif()), "card-field-value-mono");
        colNif.setMinWidth(100);

        /* Servicio (badge) */
        Label lblSrvTitle = new Label("Servicio");
        lblSrvTitle.getStyleClass().add("card-field-title");
        Node srvBadge = BadgeFactory.servicioBadge(nvl(e.getServicio()));
        VBox colSrv = new VBox(4, lblSrvTitle, srvBadge);
        colSrv.setMinWidth(90);

        VBox colDel = campoCard("Delegación",
                nvl(e.getDelegacion()), "card-field-value");
        colDel.setMinWidth(100);

        VBox colAgt = campoCard("Agente Contable",
                nvl(e.getAgenteContable()), "card-field-value");
        HBox.setHgrow(colAgt, Priority.ALWAYS);

        /* ── Fila de campos ── */
        HBox fields = new HBox(14, colDenom, colFS, colNif, colSrv, colDel, colAgt);
        fields.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);

        /* ── Estado badge ── */
        Node estadoBadge = BadgeFactory.estadoBadge(e.isActivo() ? "Activo" : "Pasivo");

        /* ── Botones ── */
        Button btnMod = new Button("✏  Modificar");
        btnMod.getStyleClass().add("btn-card-edit");
        btnMod.setOnAction(ev -> abrirFormulario(e));

        Button btnCarp = new Button("📁  Abrir Directorios");
        btnCarp.getStyleClass().add("btn-card-folder");
        btnCarp.setOnAction(ev -> abrirCarpeta(e.getRutaDocumental()));

        Button btnContacto = new Button("👤  Ver Contacto");
        btnContacto.getStyleClass().add("btn-card-contact");
        btnContacto.setOnAction(ev -> verContacto(e));

        HBox btnRow = new HBox(8, btnMod, btnCarp, btnContacto);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        /* ── Área derecha: badge + botones (ancho mínimo protegido) ── */
        VBox rightArea = new VBox(10, estadoBadge, btnRow);
        rightArea.setAlignment(Pos.TOP_RIGHT);
        rightArea.setMinWidth(Region.USE_PREF_SIZE);
        rightArea.setMaxWidth(Region.USE_PREF_SIZE);

        /* ── Contenido central + derecha ── */
        HBox centerAndRight = new HBox(16, fields, rightArea);
        centerAndRight.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fields, Priority.ALWAYS);

        /* ── Tarjeta completa ── */
        HBox card = new HBox(16, monogram, centerAndRight);
        card.getStyleClass().add("empresa-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 14));
        HBox.setHgrow(centerAndRight, Priority.ALWAYS);

        return card;
    }

    /** Helper: crea un VBox con título gris + valor sin "...". */
    private VBox campoCard(String titulo, String valor, String valueStyleClass) {
        Label t = new Label(titulo);
        t.getStyleClass().add("card-field-title");

        Label v = new Label(valor);
        v.getStyleClass().add(valueStyleClass);
        // Sin puntos suspensivos: clip silencioso si el espacio es muy reducido
        v.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);

        return new VBox(4, t, v);
    }

    /** Genera iniciales a partir del nombre (máx. 2 letras). */
    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] partes = nombre.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isBlank()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.toString();
    }

    /** Devuelve "—" si el valor es nulo o vacío. */
    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    // ─── Formulario inline (Modificar / Nueva) ────────────────────────────────

    @FXML private void onNuevaEmpresa() { abrirFormulario(null); }

    private void abrirFormulario(Empresa empresaEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/example/controltotal_proyecto/fxml/dialogs/EmpresaFormDialog.fxml")
            );
            Node formRoot = loader.load();
            EmpresaFormController ctrl = loader.getController();

            Runnable volver = () -> {
                vistaFormulario.setVisible(false);
                vistaFormulario.getChildren().clear();
                vistaLista.setVisible(true);
                cargarDatos();
                if (mainController != null) mainController.refreshStats();
            };

            ctrl.setCloseAction(volver);
            ctrl.init(empresaEditar, result -> volver.run());

            if (formRoot instanceof Region region) {
                region.prefWidthProperty().bind(vistaFormulario.widthProperty());
                region.prefHeightProperty().bind(vistaFormulario.heightProperty());
            }

            VBox.setVgrow(formRoot, Priority.ALWAYS);
            vistaFormulario.getChildren().setAll(formRoot);
            vistaLista.setVisible(false);
            vistaFormulario.setVisible(true);

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo cargar el formulario de empresa.").show();
        }
    }

    // ─── Ver Contacto (inline) ────────────────────────────────────────────────

    /**
     * Muestra un panel inline con los datos de contacto de la empresa.
     * Nombre/Apellidos se extraen del campo contactoNombre (guardado combinado).
     * El NIF se obtiene del primer persona vinculada a la empresa en BD.
     */
    private void verContacto(Empresa empresa) {
        // Obtener datos
        String fullName   = nvlS(empresa.getContactoNombre());
        int spaceIdx      = fullName.indexOf(' ');
        String nombre     = spaceIdx > 0 ? fullName.substring(0, spaceIdx)        : fullName;
        String apellidos  = spaceIdx > 0 ? fullName.substring(spaceIdx + 1).trim() : "—";

        String movil  = nvlS(empresa.getContactoMovil());
        String correo = nvlS(empresa.getContactoMail());

        // NIF: tomar del primer persona vinculado (no se almacena directamente en empresa)
        List<Persona> vinculadas = DatabaseManager.obtenerPersonasDeEmpresa(empresa.getNifCif());
        String nif = vinculadas.isEmpty() ? "—" : vinculadas.get(0).getNif();

        // ── Cabecera ──
        VBox iconBox = new VBox();
        iconBox.getStyleClass().addAll("dialog-icon", "dialog-icon-blue");
        Label iconLbl = new Label("👤");
        iconLbl.getStyleClass().add("dialog-icon-text");
        iconBox.getChildren().add(iconLbl);

        Label titulo  = new Label("Datos de Contacto");
        titulo.getStyleClass().add("dialog-title");
        Label subtitu = new Label(nvlS(empresa.getDenominacionSocial()));
        subtitu.getStyleClass().add("dialog-sub");
        VBox tituloBox = new VBox(2, titulo, subtitu);
        HBox.setHgrow(tituloBox, Priority.ALWAYS);

        // ── Botón volver (estilo botón robusto gris) ──
        Button btnVolver = new Button("← Volver");
        btnVolver.getStyleClass().add("btn-card-volver");
        
        Runnable volver = () -> {
            vistaFormulario.setVisible(false);
            vistaFormulario.getChildren().clear();
            vistaLista.setVisible(true);
        };
        btnVolver.setOnAction(ev -> volver.run());

        HBox header = new HBox(12, iconBox, tituloBox, btnVolver);
        header.getStyleClass().add("dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));

        Separator sep1 = new Separator();
        sep1.getStyleClass().add("dialog-separator");

        // ── Filas de datos ──
        VBox content = new VBox(0);
        content.setPadding(new Insets(28, 40, 28, 40));
        content.setSpacing(0);

        content.getChildren().addAll(
                filaContacto("Nombre",    nombre,   "👤"),
                filaContacto("Apellidos", apellidos, "📛"),
                filaContacto("NIF",       nif,       "🖈"),
                filaContacto("Móvil",     movil,     "✆"),
                filaContacto("Correo",    correo,    "✉")
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("dialog-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Raíz del panel ──
        VBox panel = new VBox(header, sep1, scroll);
        panel.getStyleClass().add("form-inline-root");
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(panel, Priority.ALWAYS);

        panel.prefWidthProperty().bind(vistaFormulario.widthProperty());
        panel.prefHeightProperty().bind(vistaFormulario.heightProperty());

        vistaFormulario.getChildren().setAll(panel);
        vistaLista.setVisible(false);
        vistaFormulario.setVisible(true);
    }

    /**
     * Crea una fila de dato de contacto con icono, etiqueta y valor.
     */
    private HBox filaContacto(String etiqueta, String valor, String icono) {
        Label lblIcono = new Label(icono.isBlank() ? "    " : icono);
        lblIcono.setStyle("-fx-font-size: 18px; -fx-min-width: 30px; -fx-alignment: center; -fx-text-fill: white;");

        Label lblEtiq = new Label(etiqueta);
        lblEtiq.setStyle(
            "-fx-text-fill: #8b9bb4; -fx-font-size: 13px; -fx-font-weight: bold; " +
            "-fx-min-width: 100px;"
        );

        Label lblVal = new Label(valor);
        lblVal.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 15px;");
        lblVal.setWrapText(true);
        HBox.setHgrow(lblVal, Priority.ALWAYS);

        HBox fila = new HBox(14, lblIcono, lblEtiq, lblVal);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(14, 0, 14, 0));
        fila.setStyle("-fx-border-color: transparent transparent #2a3050 transparent; -fx-border-width: 0 0 1 0;");
        return fila;
    }

    /** Versión de nvl que devuelve "—" para uso interno (no toca la versión original). */
    private String nvlS(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    // ─── Abrir carpeta ────────────────────────────────────────────────────────

    private void abrirCarpeta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Esta empresa no tiene ruta asignada.").show();
            return;
        }
        try {
            new ProcessBuilder("explorer.exe", ruta).start();
        } catch (IOException ex) {
            new Alert(Alert.AlertType.INFORMATION, "Ruta: " + ruta).show();
        }
    }
}
