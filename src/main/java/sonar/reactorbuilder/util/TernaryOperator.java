package sonar.reactorbuilder.util;

public interface TernaryOperator<V1, V2, V3, R> {
    R consume(V1 v1, V2 v2, V3 v3);
}
