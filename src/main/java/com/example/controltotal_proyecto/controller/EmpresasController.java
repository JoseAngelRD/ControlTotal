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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controlador de la vista "Empresas".
 *
 * CAMBIOS:
 *  - Botones en rejilla 2×2: [Modificar | Abrir Directorios] / [Ver Contacto | Ver Personas]
 *  - Badge Activo/Pasivo encima de la rejilla de botones.
 *  - Panel inline "Ver Personas" que muestra tarjetas de cada persona asociada.
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
     *
     * Área derecha:
     *   [ Badge Activo/Pasivo ]
     *   [ ✏ Modificar  ] [ 📁 Abrir Directorios ]
     *   [ 👤 Ver Contacto ] [ 👥 Ver Personas     ]
     */
    private Node crearTarjeta(Empresa e) {

        /* ── Monograma ── */
        String abr = (e.getAbreviatura() != null && !e.getAbreviatura().isBlank())
                ? e.getAbreviatura()
                : iniciales(e.getDenominacionSocial());

        Label monogram = new Label(abr);
        monogram.getStyleClass().add("card-monogram");

        int len = abr.length();
        double monoWidth = Math.max(70, (len * 16.0) + 24);
        monogram.setMinWidth(monoWidth);
        monogram.setPrefWidth(monoWidth);
        monogram.setMaxWidth(monoWidth);
        monogram.setMinHeight(64);
        monogram.setMaxHeight(64);
        monogram.setAlignment(Pos.CENTER);
        monogram.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        monogram.setPadding(new Insets(0, 12, 0, 12));
        monogram.setStyle("-fx-font-size: 24px;");

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

        /* ── Badge de estado ── */
        Node estadoBadge = BadgeFactory.estadoBadge(e.isActivo() ? "Activo" : "Pasivo");

        /* ── Botones ── */
        double btnW = 158;

        Button btnMod = new Button("✏  Modificar");
        btnMod.getStyleClass().add("btn-card-edit");
        btnMod.setOnAction(ev -> abrirFormulario(e));
        btnMod.setMinWidth(btnW); btnMod.setPrefWidth(btnW);

        Button btnCarp = new Button("📁  Directorios");
        btnCarp.getStyleClass().add("btn-card-folder");
        btnCarp.setOnAction(ev -> abrirCarpeta(e.getRutaDocumental()));
        btnCarp.setMinWidth(btnW); btnCarp.setPrefWidth(btnW);

        Button btnContacto = new Button("👤  Ver Contacto");
        btnContacto.getStyleClass().add("btn-card-contact");
        btnContacto.setOnAction(ev -> verContacto(e));
        btnContacto.setMinWidth(btnW); btnContacto.setPrefWidth(btnW);

        Button btnPersonas = new Button("👥  Ver Personas");
        btnPersonas.getStyleClass().add("btn-card-personas");
        btnPersonas.setOnAction(ev -> verPersonas(e));
        btnPersonas.setMinWidth(btnW); btnPersonas.setPrefWidth(btnW);

        /* ── Filas de botones ── */
        // Fila 1: [● Activo/Pasivo]  [✏ Modificar]  [📁 Directorios]
        HBox btnRow1 = new HBox(8, estadoBadge, btnMod, btnCarp);
        btnRow1.setAlignment(Pos.CENTER_RIGHT);

        // Fila 2: [👤 Ver Contacto]  [👥 Ver Personas]
        HBox btnRow2 = new HBox(8, btnContacto, btnPersonas);
        btnRow2.setAlignment(Pos.CENTER_RIGHT);

        VBox btnGrid = new VBox(8, btnRow1, btnRow2);
        btnGrid.setAlignment(Pos.CENTER_RIGHT);

        /* ── Área derecha ── */
        VBox rightArea = new VBox(0, btnGrid);
        rightArea.setAlignment(Pos.CENTER_RIGHT);
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

    private void verContacto(Empresa empresa) {

        String nombre = nvlS(empresa.getContactoNombre());
        String movil  = nvlS(empresa.getContactoMovil());
        String correo = nvlS(empresa.getContactoMail());
        String nif    = nvlS(empresa.getContactoDNI());

        VBox pageHeader = new VBox();
        pageHeader.getStyleClass().add("page-header");
        pageHeader.setPadding(new Insets(24, 28, 0, 28));
        Label pageTitle = new Label("Datos de Contacto");
        pageTitle.getStyleClass().add("page-title");
        pageHeader.getChildren().add(pageTitle);

        ImageView imgView = new ImageView();
        try {
            Image img = new Image(
                    getClass().getResourceAsStream(
                            "/com/example/controltotal_proyecto/images/persona_icon.png"),
                    90, 90, true, true
            );
            imgView.setImage(img);
        } catch (Exception ignored) { }
        imgView.setFitWidth(56);
        imgView.setFitHeight(56);
        imgView.setPreserveRatio(true);
        Circle clip = new Circle(36, 36, 36);
        imgView.setClip(clip);
        StackPane avatarPane = new StackPane(imgView);

        Label lblFullName = new Label(nombre.toUpperCase());
        lblFullName.setStyle(
                "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Century Gothic', serif;"
        );
        lblFullName.setWrapText(true);

        Label lblEmpresa = new Label(" | " + empresa.getDenominacionSocial().toUpperCase());
        lblEmpresa.setStyle(
                "-fx-text-fill: #FC7E65;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Century Gothic', serif;"
        );
        lblEmpresa.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVolver = new Button("← Volver");
        btnVolver.getStyleClass().add("btn-card-volver");
        btnVolver.setOnAction(ev -> {
            vistaFormulario.setVisible(false);
            vistaFormulario.getChildren().clear();
            vistaLista.setVisible(true);
        });

        HBox headerCard = new HBox(20, avatarPane, lblFullName, lblEmpresa, spacer, btnVolver);
        headerCard.setAlignment(Pos.CENTER_LEFT);
        headerCard.setPadding(new Insets(22, 24, 22, 24));
        headerCard.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2a3347;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        HBox headerWrapper = new HBox(headerCard);
        headerWrapper.setPadding(new Insets(16, 28, 0, 28));
        HBox.setHgrow(headerCard, Priority.ALWAYS);
        headerCard.setMaxWidth(Double.MAX_VALUE);

        VBox rowsCard = new VBox(0);
        rowsCard.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #2a3347;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 0 24 0 24;"
        );
        rowsCard.getChildren().addAll(
                filaContacto("Nombre", nombre, "\uD83D\uDC64"),
                filaContacto("NIF",    nif,    "\uD83D\uDCCB"),
                filaContacto("Móvil",  movil,  "\uD83D\uDCF1"),
                filaContacto("Correo", correo, "\u2709")
        );

        VBox rowsWrapper = new VBox(rowsCard);
        rowsWrapper.setPadding(new Insets(16, 28, 28, 28));
        VBox.setVgrow(rowsWrapper, Priority.ALWAYS);

        VBox panel = new VBox(pageHeader, headerWrapper, rowsWrapper);
        panel.setStyle("-fx-background-color: #0f1117;");
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(rowsWrapper, Priority.ALWAYS);

        panel.prefWidthProperty().bind(vistaFormulario.widthProperty());
        panel.prefHeightProperty().bind(vistaFormulario.heightProperty());

        vistaFormulario.getChildren().setAll(panel);
        vistaLista.setVisible(false);
        vistaFormulario.setVisible(true);
    }

    private HBox filaContacto(String etiqueta, String valor, String icono) {
        Label lblIcono = new Label(icono);
        lblIcono.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-min-width: 34px;" +
                        "-fx-max-width: 34px;" +
                        "-fx-alignment: center;" +
                        "-fx-text-fill: #8b9bb4;"
        );
        Label lblEtiq = new Label(etiqueta + ":");
        lblEtiq.setStyle(
                "-fx-text-fill: #6b7a99;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 110px;" +
                        "-fx-max-width: 110px;"
        );
        Label lblVal = new Label(valor);
        lblVal.setStyle("-fx-text-fill: #e8ecf4; -fx-font-size: 15px;");
        lblVal.setWrapText(true);
        HBox.setHgrow(lblVal, Priority.ALWAYS);

        HBox fila = new HBox(12, lblIcono, lblEtiq, lblVal);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(16, 0, 16, 0));
        fila.setStyle("-fx-border-color: transparent transparent #252d47 transparent; -fx-border-width: 0 0 1 0;");
        return fila;
    }

    private String nvlS(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    // ─── Ver Personas (inline) ────────────────────────────────────────────────

    /**
     * Muestra un panel inline con las personas asociadas a la empresa,
     * con tarjetas idénticas en estilo a las del PersonasController.
     */
    private void verPersonas(Empresa empresa) {

        // ── Cabecera de página ────────────────────────────────────────────────
        VBox pageHeader = new VBox();
        pageHeader.getStyleClass().add("page-header");
        pageHeader.setPadding(new Insets(24, 28, 0, 28));
        Label pageTitle = new Label("Personas Asociadas");
        pageTitle.getStyleClass().add("page-title");
        pageHeader.getChildren().add(pageTitle);

        // ── Icono 👥 + nombre empresa ─────────────────────────────────────────
        Label lblIcono = new Label("👥");
        lblIcono.setStyle("-fx-font-size: 32px;");

        Label lblEmpresaNombre = new Label(empresa.getDenominacionSocial().toUpperCase());
        lblEmpresaNombre.setStyle(
                "-fx-text-fill: #a78bfa;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Century Gothic', serif;"
        );
        lblEmpresaNombre.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ── Botón Volver ──────────────────────────────────────────────────────
        Button btnVolver = new Button("← Volver");
        btnVolver.getStyleClass().add("btn-card-volver");
        btnVolver.setOnAction(ev -> {
            vistaFormulario.setVisible(false);
            vistaFormulario.getChildren().clear();
            vistaLista.setVisible(true);
        });

        // ── Tarjeta de cabecera ───────────────────────────────────────────────
        HBox headerCard = new HBox(16, lblIcono, lblEmpresaNombre, spacer, btnVolver);
        headerCard.setAlignment(Pos.CENTER_LEFT);
        headerCard.setPadding(new Insets(18, 24, 18, 24));
        headerCard.setStyle(
                "-fx-background-color: #161b2e;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #3b2060;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        headerCard.setMaxWidth(Double.MAX_VALUE);

        HBox headerWrapper = new HBox(headerCard);
        headerWrapper.setPadding(new Insets(16, 28, 0, 28));
        HBox.setHgrow(headerCard, Priority.ALWAYS);

        // ── Lista de personas ─────────────────────────────────────────────────
        List<Persona> personas = service.getPersonas(empresa.getNifCif());

        VBox personasContainer = new VBox(10);
        personasContainer.setPadding(new Insets(16, 28, 28, 28));

        if (personas.isEmpty()) {
            Label sinPersonas = new Label("No hay personas asociadas a esta empresa.");
            sinPersonas.getStyleClass().add("personas-empty-hint");
            personasContainer.getChildren().add(sinPersonas);
        } else {
            // Contador
            Label contador = new Label(personas.size() + " persona" + (personas.size() == 1 ? "" : "s") + " asociada" + (personas.size() == 1 ? "" : "s"));
            contador.getStyleClass().add("personas-contador");
            personasContainer.getChildren().add(contador);

            for (Persona p : personas) {
                personasContainer.getChildren().add(crearTarjetaPersona(p));
            }
        }

        ScrollPane scroll = new ScrollPane(personasContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("card-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Panel raíz ────────────────────────────────────────────────────────
        VBox panel = new VBox(pageHeader, headerWrapper, scroll);
        panel.setStyle("-fx-background-color: #0f1117;");
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        panel.prefWidthProperty().bind(vistaFormulario.widthProperty());
        panel.prefHeightProperty().bind(vistaFormulario.heightProperty());

        vistaFormulario.getChildren().setAll(panel);
        vistaLista.setVisible(false);
        vistaFormulario.setVisible(true);
    }

    /**
     * Construye una tarjeta de persona al estilo PersonasController.
     * Columnas: Nombre | Apellidos | NIF | Móvil | Email  +  badge estado
     */
    private Node crearTarjetaPersona(Persona p) {

        // ── Monograma con tono morado (diferencia visual de empresa) ──────────
        String abr = iniciales(p.getNombreCompleto());
        Label monogram = new Label(abr);
        monogram.getStyleClass().add("card-monogram");
        monogram.setMinWidth(64);  monogram.setPrefWidth(64);  monogram.setMaxWidth(64);
        monogram.setMinHeight(64); monogram.setMaxHeight(64);
        monogram.setAlignment(Pos.CENTER);
        monogram.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
        // Fondo morado oscuro para distinguirlo del monograma dorado de empresa
        monogram.setStyle("-fx-font-size: 22px; -fx-background-color: #2a1f47;");

        // ── Columnas de datos ─────────────────────────────────────────────────
        VBox colNombre    = campoCard("Nombre",    nvl(p.getNombre()),        "card-field-value-bold");
        colNombre.setMinWidth(100);

        VBox colApellidos = campoCard("Apellidos", nvl(p.getApellidos()),     "card-field-value");
        colApellidos.setMinWidth(120);

        VBox colNif       = campoCard("NIF",       nvl(p.getNif()),           "card-field-value-mono");
        colNif.setMinWidth(100);

        VBox colMovil     = campoCard("Móvil",     nvl(p.getContactoMovil()), "card-field-value");
        colMovil.setMinWidth(110);

        VBox colEmail     = campoCard("Email",     nvl(p.getContactoMail()),  "card-field-value");
        HBox.setHgrow(colEmail, Priority.ALWAYS);

        HBox campos = new HBox(20, colNombre, colApellidos, colNif, colMovil, colEmail);
        campos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campos, Priority.ALWAYS);

        // ── Badge estado + botón Carpeta en una misma fila ───────────────────
        Node estadoBadge = BadgeFactory.estadoBadge(p.isActivo() ? "Activo" : "Pasivo");

        Button btnCarpeta = new Button("📁  Carpeta");
        btnCarpeta.getStyleClass().add("btn-card-folder");
        btnCarpeta.setOnAction(ev -> abrirCarpeta(p.getRutaDocumental()));

        // Badge a la izquierda del botón Carpeta, ambos centrados verticalmente
        HBox actionRow = new HBox(10, estadoBadge, btnCarpeta);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        VBox rightArea = new VBox(0, actionRow);
        rightArea.setAlignment(Pos.CENTER_RIGHT);
        rightArea.setMinWidth(Region.USE_PREF_SIZE);
        rightArea.setMaxWidth(Region.USE_PREF_SIZE);

        // ── Contenido central + derecha ───────────────────────────────────────
        HBox centerAndRight = new HBox(16, campos, rightArea);
        centerAndRight.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campos, Priority.ALWAYS);

        // ── Tarjeta ───────────────────────────────────────────────────────────
        HBox card = new HBox(16, monogram, centerAndRight);
        card.getStyleClass().addAll("empresa-card", "persona-card-en-empresa");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 14));
        HBox.setHgrow(centerAndRight, Priority.ALWAYS);

        return card;
    }

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
