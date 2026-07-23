UPDATE health_assessment
SET disclaimer = '该结果仅用于健康管理参考，不构成医学诊断。'
WHERE disclaimer = '该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。';
