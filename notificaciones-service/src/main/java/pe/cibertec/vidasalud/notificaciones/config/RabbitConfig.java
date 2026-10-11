package pe.cibertec.vidasalud.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.cibertec.vidasalud.notificaciones.listener.EventoIntegracion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "hospital.eventos";
    public static final String QUEUE = "notificaciones.eventos";

    /**
     * Eventos que generan un aviso al paciente/personal.
     */
    public static final List<String> ROUTING_KEYS = List.of(
            "paciente.registrado",
            "cita.creada",
            "cita.confirmada",
            "cita.cancelada",
            "cita.reprogramada",
            "cita.atendida",
            "cita.ausente",
            "atencion.registrada",
            "atencion.actualizada",
            "laboratorio.actualizado",
            "pago.registrado",
            "pago.anulado");

    @Bean
    public TopicExchange hospitalEventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificacionesQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Declarables notificacionesBindings(Queue notificacionesQueue, TopicExchange hospitalEventosExchange) {
        List<Binding> bindings = ROUTING_KEYS.stream()
                .map(key -> BindingBuilder.bind(notificacionesQueue).to(hospitalEventosExchange).with(key))
                .map(b -> (Binding) b)
                .toList();
        return new Declarables(bindings);
    }

    /**
     * Los productores publican con su propia clase EventoIntegracion; el
     * encabezado __TypeId__ contiene el nombre de clase del productor. Se
     * mapean todos los nombres conocidos a la copia local del contrato.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("pe.cibertec.vidasalud");
        Map<String, Class<?>> mappings = new HashMap<>();
        for (String servicio : List.of("pacientes", "citas", "atenciones", "laboratorio", "pagos")) {
            mappings.put("pe.cibertec.vidasalud." + servicio + ".events.EventoIntegracion",
                    EventoIntegracion.class);
        }
        typeMapper.setIdClassMapping(mappings);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
