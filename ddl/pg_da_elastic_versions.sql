INSERT INTO da.elastic_versions ("schema", version) VALUES
	 ('
	 {
        "mappings": {
            "properties": {
                "alt_names": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "artifact_id": {
                    "type": "keyword"
                },
                "artifact_type": {
                    "type": "keyword"
                },
                "attribute_descriptions": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "attribute_names": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "attribute_type": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "business_entity_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "calc_code": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "connector_id": {
                    "type": "keyword"
                },
                "consumer": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "custom_attributes": {
                    "type": "nested",
                    "properties": {
                        "attribute_name": {
                            "type": "keyword"
                        },
                        "attribute_value": {
                            "type": "text",
                            "fields": {
                                "keyword": {
                                    "type": "keyword",
                                    "normalizer": "alpha_sort_normalizer"
                                },
                                "standard": {
                                    "type": "text"
                                }
                            },
                            "analyzer": "ngram_analyzer",
                            "search_analyzer": "search_trunc_analyzer"
                        }
                    }
                },
                "data_entity_attribute": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "data_size": {
                    "type": "long"
                },
                "definition": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "description": {
                    "type": "text",
                    "fields": {
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "domain_id": {
                    "type": "keyword"
                },
                "domain_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "domains": {
                    "type": "keyword"
                },
                "dq_checks": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "effective_end_date": {
                    "type": "date"
                },
                "effective_start_date": {
                    "type": "date"
                },
                "entity_attribute_ids": {
                    "type": "keyword"
                },
                "entity_folder_id": {
                    "type": "keyword"
                },
                "entity_id": {
                    "type": "keyword"
                },
                "entity_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "entity_query_id": {
                    "type": "keyword"
                },
                "entity_sample_id": {
                    "type": "keyword"
                },
                "examples": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "finance_source": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "formula": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "has_query": {
                    "type": "boolean"
                },
                "has_sample": {
                    "type": "boolean"
                },
                "has_statistics": {
                    "type": "boolean"
                },
                "id": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "indicator_ids": {
                    "type": "keyword"
                },
                "indicator_type_id": {
                    "type": "keyword"
                },
                "indicator_type_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "is_main": {
                    "type": "boolean"
                },
                "limits": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "mapped_attribute_ids": {
                    "type": "keyword"
                },
                "mapped_sample_property_ids": {
                    "type": "keyword"
                },
                "modified_at": {
                    "type": "date"
                },
                "modified_by": {
                    "type": "keyword"
                },
                "modified_on": {
                    "type": "date"
                },
                "name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "normalizer": "alpha_sort_normalizer"
                        },
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "parent_id": {
                    "type": "keyword"
                },
                "path": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword"
                        },
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "path_type": {
                    "type": "keyword"
                },
                "problem": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "product_supply_variant_ids": {
                    "type": "keyword"
                },
                "product_type_ids": {
                    "type": "keyword"
                },
                "query_text": {
                    "type": "text",
                    "fields": {
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "regulation": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "roles": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "rows_count": {
                    "type": "long"
                },
                "rule_type_id": {
                    "type": "keyword"
                },
                "rule_type_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "sample_body": {
                    "type": "text",
                    "fields": {
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "sample_type": {
                    "type": "keyword"
                },
                "stewards": {
                    "type": "keyword"
                },
                "synonym_ids": {
                    "type": "keyword"
                },
                "system_folder_id": {
                    "type": "keyword"
                },
                "system_id": {
                    "type": "keyword"
                },
                "system_ids": {
                    "type": "keyword"
                },
                "system_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "system_names": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "system_type": {
                    "type": "keyword"
                },
                "tag_category_id": {
                    "type": "keyword"
                },
                "tags": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "normalizer": "alpha_sort_normalizer"
                        },
                        "standard": {
                            "type": "text"
                        }
                    },
                    "analyzer": "ngram_analyzer",
                    "search_analyzer": "search_trunc_analyzer"
                },
                "tech_name": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "user_id": {
                    "type": "keyword"
                },
                "value": {
                    "type": "text",
                    "fields": {
                        "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                        }
                    }
                },
                "version_id": {
                    "type": "long"
                }
            }
        }
    }
	 ', 1);
