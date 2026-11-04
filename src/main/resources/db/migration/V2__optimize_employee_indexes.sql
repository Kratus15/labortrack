
-- optimize the default employee list, sorted alphabetically. from ("/admin/employees") (Default)
CREATE INDEX idx_employees_company_name
    ON employees(company_id, last_name, first_name);

-- optimize employee lists filtered by status and sorted alphabetically. from ("/admin/employees") (custom)
CREATE INDEX idx_employees_company_status_name
    ON employees(company_id, status, last_name, first_name);
