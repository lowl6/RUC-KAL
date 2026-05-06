package cn.edu.ruc.kal.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;

    public static <T> PageResult<T> of(Page<T> p) {
        return new PageResult<>(p.getContent(), p.getTotalElements(), p.getNumber() + 1, p.getSize());
    }
    public static <T, R> PageResult<R> of(Page<T> p, java.util.function.Function<T, R> mapper) {
        return new PageResult<>(p.getContent().stream().map(mapper).toList(), p.getTotalElements(), p.getNumber() + 1, p.getSize());
    }
}
