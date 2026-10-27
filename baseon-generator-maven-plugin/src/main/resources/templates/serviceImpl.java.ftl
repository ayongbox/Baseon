package ${serviceImplPackage};

<#list imports as imp>
import ${imp};
</#list>

/**
 * ${entityName} 服务实现，由 baseon-generator 生成的骨架，可按业务需要修改。
 */
@Service
public class ${entityName}ServiceImpl implements ${entityName}Service {

    private final ${entityName}Mapper mapper;
    private final ${entityName}Converter converter;

    public ${entityName}ServiceImpl(${entityName}Mapper mapper, ${entityName}Converter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public ${entityName}Resp create(${entityName}Req req) {
        ${entityName} entity = converter.toEntity(req);
        mapper.insert(entity);
        return converter.toResp(entity);
    }

    @Override
    public ${entityName}Resp update(Long id, ${entityName}Req req) {
        ${entityName} entity = converter.toEntity(req);
        entity.setId(id);
        mapper.updateById(entity);
        return converter.toResp(mapper.selectById(id));
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public ${entityName}Resp getById(Long id) {
        return converter.toResp(mapper.selectById(id));
    }

    @Override
    public List<${entityName}VO> list() {
        return converter.toVOList(mapper.selectList(null));
    }

    @Override
    public PageResult<${entityName}VO> page(PageQuery query) {
        Page<${entityName}> page = mapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), null);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                converter.toVOList(page.getRecords()));
    }
}
