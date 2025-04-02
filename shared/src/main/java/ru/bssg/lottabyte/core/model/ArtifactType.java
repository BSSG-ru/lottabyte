package ru.bssg.lottabyte.core.model;

public enum ArtifactType {
    dq_rule("dq_rule", "Правило проверки качества"),
    domain("domain", "Домен"),
    steward("steward"),
    system("system", "Система"),
    system_folder("system_folder"),
    entity("entity", "Модель"),
    entity_attribute("entity_attribute"),
    entity_folder("entity_folder"),
    entity_query("entity_query", "Запрос"),
    entity_sample("entity_sample", "Сэмпл"),
    entity_sample_property("entity_sample_property"),
    entity_sample_to_dq_rule("entity_sample_to_dq_rule"),
    data_asset("data_asset", "Актив"),
    custom_attribute_defelement("custom_attribute_defelement"),
    custom_attribute_definition("custom_attribute_definition"),
    tag("tag", "Тэг"),
    tag_category("tag_category"),
    comment("comment"),
    connector("connector"),
    connector_param("connector_param"),
    system_connection("system_connection", "Подключение"),
    system_connection_param("system_connection_param"),
    task("task", "Задача"),
    task_schedule("task_schedule"),
    qualityTask("qualityTask"),
    qualityAssertionTask("qualityAssertionTask"),
    qualityRuleTask("qualityRuleTask"),
    qualityTaskRun("qualityTaskRun"),
    task_run("task_run"),
    workflow_task_action("workflow_task_action"),
    system_type("system_type"),
    backup_run("backup_run"),
    indicator("indicator", "Показатель"),
    business_entity("business_entity", "Глоссарий"),
    datatype("datatype"),
    reference("reference"),
    enumeration("enumeration"),
    tenant("tenant"),
    token("token"),
    black_list_token("black_list_token"),
    ldap_properties("ldap_properties"),
    external_groups("external_groups"),
    workflow_task("workflow_task"),
    workflow("workflow"),
    product("product", "Продукт"),
    product_type("product_type"),
    tech_spec("tech_spec"),
    product_supply_variant("product_supply_variant"),
    dq_rule_task("dq_rule_task"),
    meta_object("meta_object", "Метаобъект"),
    meta_column("meta_column", "Метаколонка"),
    meta_database("meta_database", "Метаданные"),
    etl("etl", "Трансформация");

    private String text;
    private String displayName;

    ArtifactType(String text) {
        this.text = text;
        this.displayName = text;
    }
    ArtifactType(String text, String displayName) {
        this.text = text;
        this.displayName = displayName;
    }

    public String getText() {
        return this.text;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static ArtifactType fromString(String text) {
        for (ArtifactType b : ArtifactType.values()) {
            if (b.text.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
