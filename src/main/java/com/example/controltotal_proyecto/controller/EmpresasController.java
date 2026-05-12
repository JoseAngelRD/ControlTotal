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
 * CAMBIOS respecto a la versión anterior:
 *  - Botón "Ver Contacto" (amarillo) a la derecha de "Abrir Directorios".
 *  - Panel inline de contacto que muestra Nombre, Apellidos, NIF, Móvil y Correo.
 *  - Monograma con fuente adaptativa que muestra la abreviatura completa.
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

        /* ── Monograma con fuente adaptativa (sin recorte de longitud) ── */
        String abr = (e.getAbreviatura() != null && !e.getAbreviatura().isBlank())
                ? e.getAbreviatura()
                : iniciales(e.getDenominacionSocial());

        Label monogram = new Label(abr);
        monogram.getStyleClass().add("card-monogram");

        // Ancho adaptativo para que SIEMPRE quepan todas las letras.
        // Se usa un factor mayor para acomodar el tamaño de letra grande y fijo.
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

        // Tamaño de letra fijo y grande (24px) como solicitado
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
     * Diseño renovado: avatar circular naranja + nombre en mayúsculas +
     * botón Volver alineado a la derecha; filas con icono, etiqueta y valor.
     */
    private void verContacto(Empresa empresa) {

        // ── Extraer datos ──────────────────────────────────────────────────────
        String nombre    = nvlS(empresa.getContactoNombre());
        String movil     = nvlS(empresa.getContactoMovil());
        String correo    = nvlS(empresa.getContactoMail());
        String nif       = nvlS(empresa.getContactoDNI());

        // ── Cabecera superior: "Datos de Contacto" ────────────────────────────
        VBox pageHeader = new VBox();
        pageHeader.getStyleClass().add("page-header");
        pageHeader.setPadding(new Insets(24, 28, 0, 28));
        Label pageTitle = new Label("Datos de Contacto");
        pageTitle.getStyleClass().add("page-title");
        pageHeader.getChildren().add(pageTitle);

        // ── Avatar circular con imagen persona_icon.png ───────────────────────
        ImageView imgView = new ImageView();
        try {
            Image img = new Image(
                    getClass().getResourceAsStream(
                            "/com/example/controltotal_proyecto/images/persona_icon.png"),
                    90, 90, true, true
            );
            imgView.setImage(img);
        } catch (Exception ignored) { /* Imagen no disponible: avatar vacío */ }
        imgView.setFitWidth(56);
        imgView.setFitHeight(56);
        imgView.setPreserveRatio(true);

        Circle clip = new Circle(36, 36, 36);
        imgView.setClip(clip);

        StackPane avatarPane = new StackPane(imgView);

        // ── Nombre completo en mayúsculas ─────────────────────────────────────
        Label lblFullName = new Label(nombre.toUpperCase());
        lblFullName.setStyle(
                "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Century Gothic', serif;"
        );
        lblFullName.setWrapText(true);

        // ── Nombre de la empresa (Naranja) ────────────────────────────────────
        Label lblEmpresa = new Label(" | " + empresa.getDenominacionSocial().toUpperCase());
        lblEmpresa.setStyle(
                "-fx-text-fill: #FC7E65;" +
                        "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Century Gothic', serif;"
        );
        lblEmpresa.setWrapText(true);

        // Spacer para empujar el botón Volver a la derecha
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

        // ── Tarjeta de cabecera (avatar + nombre + volver) ────────────────────
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

        // ── Tarjeta de filas de datos ─────────────────────────────────────────
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
                filaContacto("Nombre",   nombre,   "\uD83D\uDC64"),   // 👤
                filaContacto("NIF",       nif,       "\uD83D\uDCCB"),   // 📋
                filaContacto("Móvil",     movil,     "\uD83D\uDCF1"),   // 📱
                filaContacto("Correo",    correo,    "\u2709")          // ✉
        );

        VBox rowsWrapper = new VBox(rowsCard);
        rowsWrapper.setPadding(new Insets(16, 28, 28, 28));
        VBox.setVgrow(rowsWrapper, Priority.ALWAYS);

        // ── Panel raíz ────────────────────────────────────────────────────────
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

    /**
     * Crea una fila de dato de contacto: icono gris + etiqueta gris + valor blanco.
     * Cada fila lleva separador inferior excepto la última (se aplica igual a todas
     * por simplicidad; el CSS del último hijo puede quitarlo si se desea).
     */
    private HBox filaContacto(String etiqueta, String valor, String icono) {

        // Icono
        Label lblIcono = new Label(icono);
        lblIcono.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-min-width: 34px;" +
                        "-fx-max-width: 34px;" +
                        "-fx-alignment: center;" +
                        "-fx-text-fill: #8b9bb4;"
        );

        // Etiqueta (con ":")
        Label lblEtiq = new Label(etiqueta + ":");
        lblEtiq.setStyle(
                "-fx-text-fill: #6b7a99;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 110px;" +
                        "-fx-max-width: 110px;"
        );

        // Valor
        Label lblVal = new Label(valor);
        lblVal.setStyle("-fx-text-fill: #e8ecf4; -fx-font-size: 15px;");
        lblVal.setWrapText(true);
        HBox.setHgrow(lblVal, Priority.ALWAYS);

        HBox fila = new HBox(12, lblIcono, lblEtiq, lblVal);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(16, 0, 16, 0));
        fila.setStyle(
                "-fx-border-color: transparent transparent #252d47 transparent;" +
                        "-fx-border-width: 0 0 1 0;"
        );
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
