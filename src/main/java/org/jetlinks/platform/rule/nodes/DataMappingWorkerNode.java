package org.jetlinks.platform.rule.nodes;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hswebframework.web.bean.Converter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutableRuleNode;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Component
public class DataMappingWorkerNode extends CommonExecutableRuleNodeFactoryStrategy<DataMappingWorkerNode.Config> {

    public static Converter converter = FastBeanCopier.DEFAULT_CONVERT;

    @Override
    public String getSupportType() {
        return "data-mapping";
    }

    @Override
    protected ExecutableRuleNode doCreate(Config config) {


        return super.doCreate(config);
    }

    @Override
    public Function<RuleData, Publisher<Object>> createExecutor(ExecutionContext context, Config config) {

        return ruleData -> Mono.just(config.mapping(convertObject(ruleData.getData())));
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        private List<Mapping> mappings = new ArrayList<>();

        private boolean keepSourceData = false;

        private NodeType nodeType;

        private Map<String, Object> toMap(Object source) {
            return FastBeanCopier.copy(source, HashMap::new);
        }

        @SuppressWarnings("all")
        private Object mapping(Object data) {
            if (data instanceof Map) {
                return doMapping(((Map) data));
            }
            if (data instanceof Collection) {
                Collection<Object> source = ((Collection) data);
                return source
                        .stream()
                        .map(this::toMap)
                        .map(this::doMapping)
                        .collect(Collectors.toList());
            }
            return data;
        }

        private Object doMapping(Map<String, Object> object) {
            Map<String, Object> newData = new HashMap<>();
            if (keepSourceData) {
                newData.putAll(object);
            }
            for (Mapping mapping : mappings) {
                Object data = mapping.getData(object);
                if (data != null) {
                    newData.put(mapping.target, data);
                }
            }
            return newData;
        }
    }

    @Getter
    @Setter
    public static class Mapping {
        private String target;

        private String source;

        private String type;

        private transient Class typeClass;

        public Mapping() {

        }

        public Mapping(String target, String source) {
            this.target = target;
            this.source = source;
        }

        public Mapping(String target, String source, String type) {
            this.target = target;
            this.source = source;
            this.type = type;
        }

        @SneakyThrows
        public Class<?> getTypeClass() {
            if (typeClass == null && type != null) {
                String lowerType = type.toLowerCase();
                switch (lowerType) {
                    case "int":
                    case "integer":
                        return typeClass = Integer.class;
                    case "string":
                        return typeClass = String.class;
                    case "double":
                        return typeClass = Double.class;
                    case "decimal":
                        return typeClass = BigDecimal.class;
                    case "boolean":
                        return typeClass = Boolean.class;
                    case "date":
                        return typeClass = Date.class;
                    default:
                        return typeClass = Class.forName(type);
                }
            }
            if (typeClass == Void.class) {
                return null;
            }
            return typeClass;
        }

        @SneakyThrows
        public Object getData(Map<String, Object> sourceData) {
            Object data = sourceData.get(this.source);
//            if (data == null) {
//                data = ExpressionUtils.analytical(this.source, sourceData, "spel");
//            }
            if (data == null) {
                return null;
            }
            return getTypeClass() != null ? converter.convert(data, getTypeClass(), null) : data;
        }

    }
}
