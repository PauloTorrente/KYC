package com.donjuan.kyc.dto.kyc;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /api/public/solicitudes, enviado pelo site publico (sitio-web).
 * So aceita os campos que o proprio cliente pode informar; plataforma e estado
 * do KYC sao definidos no servidor (SITE / PENDENTE) e revisados pelo operador.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SolicitudPublicaRequest {

    @NotBlank(message = "nombre es obligatorio")
    @Size(max = 120, message = "nombre demasiado largo")
    private String nombre;

    @Pattern(regexp = "CI|PASAPORTE|CPF|RNE|OTRO", message = "tipo_documento invalido")
    private String tipoDocumento;

    @Size(max = 40, message = "numero_documento demasiado largo")
    private String numeroDocumento;

    @Size(max = 60)
    private String nacionalidad;

    @NotBlank(message = "telefono es obligatorio")
    @Size(max = 30, message = "telefono demasiado largo")
    private String telefono;

    @Email(message = "email invalido")
    @Size(max = 120)
    private String email;

    @Size(max = 80)
    private String ciudad;

    @Size(max = 60)
    private String pais;

    /** Operacion de interes: COMPRA_USDT, VENTA_USDT, BOB_BRL, BRL_BOB. */
    @Pattern(regexp = "COMPRA_USDT|VENTA_USDT|BOB_BRL|BRL_BOB", message = "operacion invalida")
    private String operacion;

    @Size(max = 40)
    private String monto;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }
}
