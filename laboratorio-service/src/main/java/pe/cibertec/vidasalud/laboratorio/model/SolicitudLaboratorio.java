package pe.cibertec.vidasalud.laboratorio.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("solicitud_laboratorio")
public class SolicitudLaboratorio {

    @Id
    private Long id;
    private String codigo;
    private Long pacienteId;
    private Long atencionId;
    private String tipoExamen;
    private String estado;
    private String resultadoFicticio;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaActualizacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public Long getAtencionId() {
        return atencionId;
    }

    public void setAtencionId(Long atencionId) {
        this.atencionId = atencionId;
    }

    public String getTipoExamen() {
        return tipoExamen;
    }

    public void setTipoExamen(String tipoExamen) {
        this.tipoExamen = tipoExamen;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getResultadoFicticio() {
        return resultadoFicticio;
    }

    public void setResultadoFicticio(String resultadoFicticio) {
        this.resultadoFicticio = resultadoFicticio;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
