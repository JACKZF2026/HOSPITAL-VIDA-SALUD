package pe.cibertec.vidasalud.auditoria.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.cibertec.vidasalud.auditoria.listener.EventoIntegracion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "hospital.eventos";
    public static final String QUEUE = "auditoria.eventos";

    @Bean
    public TopicExchange hospitalEventosExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue auditoriaQueue() {
        return new Queue(QUEUE, true);
    }

    /**
     * Auditoria recibe TODOS los eventos del exchange topic (binding "#").
     */
    @Bean
    public Binding auditoriaBinding(Queue auditoriaQueue, TopicExchange hospitalEventosExchange) {
        return BindingBuilder.bind(auditoriaQueue).to(hospitalEventosExchange).with("#");
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
