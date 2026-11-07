-- 修复已有数据卷中仍指向无 kind 参数目录页的校区和学年菜单。
-- 按路由名称匹配，不依赖不同环境的菜单主键；允许重复执行。
UPDATE sys_menu SET component = '/school/campuses' WHERE route_name = 'CampusManagement';
UPDATE sys_menu SET component = '/school/academic-years' WHERE route_name = 'AcademicYearManagement';
UPDATE sys_menu SET component = '/school/subjects' WHERE route_name = 'SubjectManagement';
