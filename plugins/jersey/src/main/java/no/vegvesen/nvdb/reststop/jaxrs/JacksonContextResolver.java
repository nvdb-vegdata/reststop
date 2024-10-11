package no.vegvesen.nvdb.reststop.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces("application/json")
@Consumes("application/json")
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    /**
     * Creates a context resolver for Jackson JSON serialization and deserialization.
     * Note! Should be same implementation as no.vegvesen.vt.nvdb.apiskriv.it.support.JacksonContextResolver.
     */
    public JacksonContextResolver() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        JakartaXmlBindAnnotationIntrospector introspector = new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance());
        introspector.setNameUsedForXmlValue("verdi");
        mapper.setAnnotationIntrospector(introspector);
        mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
