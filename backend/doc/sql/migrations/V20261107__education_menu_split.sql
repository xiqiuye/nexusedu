-- 教育菜单拆分：初始化数据中的同名语句用于新库，本迁移用于已有数据卷。
-- 语句保持幂等，可由发布流程在备份后执行。
UPDATE sys_menu
SET parent_id = 1720, path = 'subjects', component = '/school/subjects',
    title = '学科管理', status = 'ENABLED', visible = 1
WHERE id = 1731;

UPDATE sys_menu
SET parent_id = 1720, path = 'assignments', component = '/school/assignments',
    title = '班主任/任课关系', status = 'ENABLED', visible = 1
WHERE id = 1723;

UPDATE sys_menu
SET parent_id = 1700, path = 'roster', component = '/student',
    title = '学生档案', status = 'ENABLED', visible = 1
WHERE id = 1701;

UPDATE sys_menu
SET status = 'DISABLED', visible = 0
WHERE id = 1713;

DELETE FROM sys_role_menu WHERE menu_id = 1713;
