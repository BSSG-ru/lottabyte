package ru.bssg.lottabyte.core.model;

public enum ArtifactType {
    dq_rule("dq_rule"),
    domain("domain"),
    steward("steward"),
    system("system"),
    system_folder("system_folder"),
    entity("entity"),
    entity_attribute("entity_attribute"),
    entity_folder("entity_folder"),
    entity_query("entity_query"),
    entity_sample("entity_sample"),
    entity_sample_property("entity_sample_property"),
    entity_sample_to_dq_rule("entity_sample_to_dq_rule"),
    data_asset("data_asset"),
    custom_attribute_defelement("custom_attribute_defelement"),
    custom_attribute_definition("custom_attribute_definition"),
    tag("tag"),
    tag_category("tag_category"),
    comment("comment"),
    connector("connector"),
    connector_param("connector_param"),
    system_connection("system_connection"),
    system_connection_param("system_connection_param"),
    task("task"),
    qualityTask("qualityTask"),
    qualityAssertionTask("qualityAssertionTask"),
    qualityRuleTask("qualityRuleTask"),
    qualityTaskRun("qualityTaskRun"),
    task_run("task_run"),
    workflow_task_action("workflow_task_action"),
    system_type("system_type"),
    backup_run("backup_run"),
    indicator("indicator"),
    business_entity("business_entity"),
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
    product("product"),
    product_type("product_type"),
    tech_spec("tech_spec"),
    product_supply_variant("product_supply_variant");

    private String text;

    ArtifactType(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
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
