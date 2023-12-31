package ru.bssg.lottabyte.core.model.reference;

public enum ReferenceType {
    INDICATOR_TO_ENTITY_ATTRIBUTE,
    INDICATOR_TO_DATA_ASSET,
    BUSINESS_ENTITY_TO_ENTITY,
    PRODUCT_TO_DATA_ENTITY_ATTRIBUTE,
    PRODUCT_TO_INDICATOR,
    PRODUCT_TO_PRODUCT_TYPE,
    PRODUCT_TO_PRODUCT_SUPPLY_VARIANT,
    PRODUCT_TO_DATA_ASSET,
    DATA_ENTITY_TO_BUSINESS_ENTITY,
    BUSINESS_ENTITY_TO_BUSINESS_ENTITY,
    BUSINESS_ENTITY_TO_BUSINESS_ENTITY_LINK,
    PRODUCT_TO_BUSINESS_ENTITY_LINK,
    INDICATOR_TO_BUSINESS_ENTITY_LINK,
    ENTITY_ATTRIBUTE_TO_ENTITY_ATTRIBUTE,
    BUSINESS_ENTITY_TO_ENTITY_ATTRIBUTE;

    private ReferenceType() {

    }
}
