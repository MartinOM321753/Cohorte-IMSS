package imss.gob.mx.cohorte.audit.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EmbeddedId;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Serializador dedicado para el sistema de auditoría.
 *
 * <p>Garantiza que todo objeto se convierta a JSON legible sin importar
 * su complejidad (proxies Hibernate, ciclos, lazy collections, tipos
 * no serializables). Estrategia de fallback multinivel:</p>
 *
 * <ol>
 *   <li><b>Tipos simples / DTOs</b> — Jackson directo (funciona sin problemas).</li>
 *   <li><b>Entidades JPA</b> — Extracción reflectiva con resolución de proxies
 *       y profundidad controlada (máx. 2 niveles de relaciones).</li>
 *   <li><b>Fallback final</b> — tipo + toString, nunca devuelve null ni excepción.</li>
 * </ol>
 */
@Component
public class AuditSerializer {

    private static final int MAX_DEPTH = 2;

    private static final Set<Class<?>> SKIP_TYPES = Set.of(
            InputStream.class,
            byte[].class
    );

    private final ObjectMapper simpleMapper;

    public AuditSerializer() {
        this.simpleMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public String serialize(Object obj) {
        if (obj == null) return null;
        if (isSkippable(obj)) return omitted(obj);

        Object unwrapped = unwrapProxy(obj);
        if (unwrapped == null) return null;

        if (isJpaEntity(unwrapped)) {
            return serializeEntity(unwrapped);
        }

        try {
            return simpleMapper.writeValueAsString(unwrapped);
        } catch (JsonProcessingException e) {
            return serializeEntity(unwrapped);
        }
    }

    public String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return null;

        Object[] filtered = Arrays.stream(args)
                .filter(a -> a != null && !isSkippable(a))
                .toArray();

        if (filtered.length == 0) return null;
        if (filtered.length == 1) return serialize(filtered[0]);

        List<Object> parts = new ArrayList<>();
        for (Object arg : filtered) {
            Object unwrapped = unwrapProxy(arg);
            if (unwrapped == null) continue;
            if (isJpaEntity(unwrapped)) {
                parts.add(extractFields(unwrapped, 0, new IdentityHashMap<>()));
            } else {
                parts.add(unwrapped);
            }
        }

        try {
            return simpleMapper.writeValueAsString(parts);
        } catch (JsonProcessingException e) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(serialize(parts.get(i)));
            }
            return sb.append("]").toString();
        }
    }

    // ── Serialización de entidades ───────────────────────────────────────────

    private String serializeEntity(Object obj) {
        Map<String, Object> map = extractFields(obj, 0, new IdentityHashMap<>());
        try {
            return simpleMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{\"_tipo\":\"" + obj.getClass().getSimpleName()
                    + "\",\"_toString\":\"" + sanitize(obj.toString()) + "\"}";
        }
    }

    private Map<String, Object> extractFields(Object obj, int depth, IdentityHashMap<Object, Boolean> visited) {
        if (obj == null) return null;

        Object unwrapped = unwrapProxy(obj);
        if (unwrapped == null) return null;

        if (visited.containsKey(unwrapped)) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("_ref", extractId(unwrapped));
            return ref;
        }
        visited.put(unwrapped, Boolean.TRUE);

        Map<String, Object> map = new LinkedHashMap<>();
        Class<?> clazz = unwrapped.getClass();
        map.put("_tipo", clazz.getSimpleName());

        for (Field field : getAllFields(clazz)) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isTransient(field.getModifiers())) continue;

            field.setAccessible(true);
            Object value;
            try {
                value = field.get(unwrapped);
            } catch (Exception e) {
                continue;
            }

            if (value == null) {
                map.put(field.getName(), null);
                continue;
            }

            Class<?> ft = field.getType();

            if (isSimpleType(ft)) {
                map.put(field.getName(), value.toString());
                continue;
            }

            if (depth >= MAX_DEPTH) {
                if (isJpaEntity(value) || isHibernateProxy(value)) {
                    map.put(field.getName(), extractIdAsMap(value));
                }
                continue;
            }

            if (isHibernateProxy(value)) {
                Object proxyUnwrapped = unwrapProxy(value);
                if (proxyUnwrapped == null) {
                    map.put(field.getName(), extractIdFromProxy(value));
                    continue;
                }
                map.put(field.getName(), extractFields(proxyUnwrapped, depth + 1, visited));
                continue;
            }

            if (isJpaEntity(value)) {
                map.put(field.getName(), extractFields(value, depth + 1, visited));
                continue;
            }

            if (Collection.class.isAssignableFrom(ft)) {
                try {
                    Collection<?> col = (Collection<?>) value;
                    if (isLazyCollection(col)) {
                        map.put(field.getName(), "[lazy, no cargada]");
                    } else {
                        map.put(field.getName(), col.size() + " elementos");
                    }
                } catch (Exception e) {
                    map.put(field.getName(), "[colección no accesible]");
                }
                continue;
            }

            try {
                simpleMapper.writeValueAsString(value);
                map.put(field.getName(), value);
            } catch (Exception e) {
                map.put(field.getName(), value.toString());
            }
        }

        return map;
    }

    // ── Utilidades de Hibernate ──────────────────────────────────────────────

    private Object unwrapProxy(Object obj) {
        if (obj instanceof HibernateProxy proxy) {
            LazyInitializer init = proxy.getHibernateLazyInitializer();
            if (init.isUninitialized()) {
                return null;
            }
            return init.getImplementation();
        }
        return obj;
    }

    private boolean isHibernateProxy(Object obj) {
        return obj instanceof HibernateProxy;
    }

    private boolean isLazyCollection(Collection<?> col) {
        if (col instanceof org.hibernate.collection.spi.PersistentCollection<?> pc) {
            return !pc.wasInitialized();
        }
        return false;
    }

    private Object extractIdFromProxy(Object proxy) {
        if (proxy instanceof HibernateProxy hp) {
            Object id = hp.getHibernateLazyInitializer().getIdentifier();
            if (id != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("_id", id.toString());
                return m;
            }
        }
        return null;
    }

    // ── Utilidades de JPA ────────────────────────────────────────────────────

    private boolean isJpaEntity(Object obj) {
        if (obj == null) return false;
        Class<?> clazz = obj instanceof HibernateProxy hp
                ? hp.getHibernateLazyInitializer().getPersistentClass()
                : obj.getClass();
        return clazz.isAnnotationPresent(Entity.class);
    }

    private Object extractId(Object entity) {
        if (entity == null) return null;
        for (Field f : getAllFields(entity.getClass())) {
            if (f.isAnnotationPresent(Id.class) || f.isAnnotationPresent(EmbeddedId.class)) {
                f.setAccessible(true);
                try {
                    Object id = f.get(entity);
                    return id != null ? id.toString() : null;
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Map<String, Object> extractIdAsMap(Object entity) {
        Object id = null;
        if (entity instanceof HibernateProxy hp) {
            id = hp.getHibernateLazyInitializer().getIdentifier();
        } else {
            id = extractId(entity);
        }
        if (id == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", id.toString());
        return m;
    }

    // ── Utilidades generales ─────────────────────────────────────────────────

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || type == String.class
                || type == Boolean.class
                || type == Character.class
                || type.isEnum()
                || Temporal.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type)
                || type == UUID.class;
    }

    private boolean isSkippable(Object obj) {
        if (obj == null) return true;
        Class<?> clazz = obj.getClass();
        for (Class<?> t : SKIP_TYPES) {
            if (t.isAssignableFrom(clazz)) return true;
        }
        return clazz.getName().startsWith("org.springframework.data.domain");
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(List.of(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ").replace("\r", "");
    }

    private String omitted(Object obj) {
        return "{\"_tipo\":\"" + obj.getClass().getSimpleName() + "\",\"_omitido\":true}";
    }
}
