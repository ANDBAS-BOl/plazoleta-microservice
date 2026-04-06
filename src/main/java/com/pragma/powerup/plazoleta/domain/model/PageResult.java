package com.pragma.powerup.plazoleta.domain.model;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mapped = content.stream().map(mapper).collect(Collectors.toList());
        return new PageResult<>(mapped, page, size, totalElements, totalPages);
    }
}
