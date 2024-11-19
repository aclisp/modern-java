module mj.util {
    requires org.slf4j;
    requires transitive java.logging;
    requires io.prometheus.metrics.core;
    requires io.prometheus.metrics.model;

    exports com.mycompany.util;
}
