package es.labjc.regalos3d.encargo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Enums guardados como VARCHAR. Con un conversor explícito Hibernate valida la columna como texto
 * (con @Enumerated, en MariaDB esperaría un tipo ENUM nativo y fallaría la validación del esquema).
 */
public final class Conversores {

    private Conversores() {
    }

    @Converter
    public static class Tipo implements AttributeConverter<TipoEncargo, String> {
        @Override
        public String convertToDatabaseColumn(TipoEncargo v) {
            return v == null ? null : v.name();
        }

        @Override
        public TipoEncargo convertToEntityAttribute(String v) {
            return v == null ? null : TipoEncargo.valueOf(v);
        }
    }

    @Converter
    public static class Estado implements AttributeConverter<EstadoEncargo, String> {
        @Override
        public String convertToDatabaseColumn(EstadoEncargo v) {
            return v == null ? null : v.name();
        }

        @Override
        public EstadoEncargo convertToEntityAttribute(String v) {
            return v == null ? null : EstadoEncargo.valueOf(v);
        }
    }
}
