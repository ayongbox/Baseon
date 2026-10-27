package ${package}.controller;

import io.github.ayongbox.baseon.core.model.PageQuery;
import io.github.ayongbox.baseon.core.model.PageResult;
import io.github.ayongbox.baseon.core.model.R;
import ${package}.service.ExampleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例接口：演示统一返回体 R、统一分页 PageQuery/PageResult 与业务异常抛出。
 * 接入真实业务后可整体删除本类与 ExampleService。
 */
@RestController
@RequestMapping("/example")
public class ExampleController {

    private final ExampleService exampleService;

    public ExampleController(ExampleService exampleService) {
        this.exampleService = exampleService;
    }

    /** 统一返回体：curl http://localhost:8080/example/hello */
    @GetMapping("/hello")
    public R<String> hello() {
        return R.ok("hello baseon");
    }

    /** 统一分页：curl "http://localhost:8080/example/page?current=2&size=10" */
    @GetMapping("/page")
    public R<PageResult<String>> page(PageQuery query) {
        return R.ok(exampleService.page(query));
    }

    /** 业务异常：curl http://localhost:8080/example/999 → 全局异常处理器收敛为统一 R */
    @GetMapping("/{id}")
    public R<String> detail(@PathVariable long id) {
        return R.ok(exampleService.detail(id));
    }
}
