package com.example.controltotal_proyecto.util;

import javafx.scene.control.Label;

/**
 * Fábrica de etiquetas (badges) reutilizables para la UI.
 * Replica los estilos .badge, .badge-service, etc. del diseño web.
 */
public class BadgeFactory {

    /**
     * Crea un badge de estado (Activo / Pasivo / Inactivo).
     */
    public static Label estadoBadge(String estado) {
        Label lbl = new Label(estado);
        lbl.getStyleClass().add("badge");
        if ("Activo".equalsIgnoreCase(estado)) {
            lbl.getStyleClass().add("badge-activo");
        } else {
            lbl.getStyleClass().add("badge-pasivo");
        }
        return lbl;
    }

    /**
     * Crea un badge de tipo de servicio (Asesoría, Auditoría, Concursal, Pericial).
     */
    public static Label servicioBadge(String servicio) {
        Label lbl = new Label(servicio != null ? servicio : "");
        lbl.getStyleClass().addAll("badge-service");
        if (servicio != null) {
            lbl.getStyleClass().add("badge-service-" + servicio.toLowerCase()
                .replace("í","i").replace("ó","o").replace("é","e"));
        }
        return lbl;
    }

    /**
     * Crea un badge de empresa (abreviatura) para la columna de personas.
     */
    public static Label empresaTag(String abreviatura) {
        Label lbl = new Label(abreviatura);
        lbl.getStyleClass().add("empresa-tag");
        return lbl;
    }
}
