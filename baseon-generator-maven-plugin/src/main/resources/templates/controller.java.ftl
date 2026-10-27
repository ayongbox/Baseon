package ${controllerPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 接口，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
@RestController
@RequestMapping("/${requestPath}")
public class ${entityName}Controller {

    private final ${entityName}Service service;

    public ${entityName}Controller(${entityName}Service service) {
        this.service = service;
    }

    /** 创建 */
    @PostMapping
    public R<${entityName}Resp> create(@RequestBody ${entityName}Req req) {
        return R.ok(service.create(req));
    }

    /** 按主键更新 */
    @PutMapping("/{id}")
    public R<${entityName}Resp> update(@PathVariable Long id, @RequestBody ${entityName}Req req) {
        return R.ok(service.update(id, req));
    }

    /** 按主键删除 */
    @DeleteMapping("/{id}")
    public R<Void> deleteById(@PathVariable Long id) {
        service.deleteById(id);
        return R.ok();
    }

    /** 按主键查询详情 */
    @GetMapping("/{id}")
    public R<${entityName}Resp> getById(@PathVariable Long id) {
        return R.ok(service.getById(id));
    }

    /** 全量列表 */
    @GetMapping
    public R<List<${entityName}VO>> list() {
        return R.ok(service.list());
    }

    /** 分页查询 */
    @GetMapping("/page")
    public R<PageResult<${entityName}VO>> page(PageQuery query) {
        return R.ok(service.page(query));
    }
}
