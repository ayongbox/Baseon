CREATE TABLE IF NOT EXISTS t_data_scope_demo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dept_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL
);

-- 三个部门各准备两条数据，便于断言过滤结果
INSERT INTO t_data_scope_demo (dept_id, name) VALUES
    (10, 'dept10-a'),
    (10, 'dept10-b'),
    (20, 'dept20-a'),
    (20, 'dept20-b'),
    (30, 'dept30-a'),
    (30, 'dept30-b');
