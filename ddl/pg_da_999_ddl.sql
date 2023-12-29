CREATE SCHEMA da_999;

CREATE TABLE da_999.artifact_type (
	id uuid NOT NULL,
	code text NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT artifact_type_pk PRIMARY KEY (id)
);

INSERT INTO da_999.artifact_type (id,code,"name") VALUES
	 ('a953ed2b-acbb-4746-b710-7d9ca0472d5b','domain','Домен'),
	 ('7bcf0682-f123-4e25-8276-a63ae44f50f0','dq_rule','Правило проверки качества'),
	 ('4182cb2a-03cd-4c12-86f7-85a9af64545d','steward','Стюард'),
	 ('3416eb7e-41cb-4e6e-a8af-29943f5f7560','system','Система'),
	 ('fd389e0c-08ca-4d9d-b22b-70260bb36458','system_folder','Папка системы'),
	 ('89e82c96-71f0-4817-bb33-3c3df8a00912','entity','Логический объект'),
	 ('1eb3044c-a3d1-477c-879a-17047b6249e6','entity_attribute','Атрибут'),
	 ('2e79d6e5-e8bd-4b73-963b-a3b4ec2ca26e','entity_folder','Папка ЛО'),
	 ('9e28d2a2-321f-4176-8e85-6ec4b8774948','entity_query','Запрос'),
	 ('0fb06cdb-49b2-48a9-8bba-837dfd8150a2','entity_sample','Сэмпл');
INSERT INTO da_999.artifact_type (id,code,"name") VALUES
	 ('5f638919-0da3-40e6-96d5-857087ce3037','entity_sample_property','Свойство сэмпла'),
	 ('28c2379a-d42f-4437-9879-04424a317b71','data_asset','Актив'),
	 ('7a7f3a20-f12c-4568-be12-f1cf9792dff9','system_connection','Подключение'),
	 ('34db5026-daf3-426f-96b7-03f432f5499c','system_connection_param','Параметр подключения'),
	 ('3c8fb43c-fd7c-46dc-b014-dcec3b19b8e4','task','Задача'),
	 ('c397238f-6949-4ce6-a132-54e346da41dc','indicator','Показатель'),
	 ('f29d11f9-fe90-47c4-b426-c6581124cb29','business_entity','Бизнес-сущность'),
	 ('693621d7-89e9-422f-a8f8-f0bd94116d13','workflow','Воркфлоу'),
	 ('a6440252-46d6-47dc-a9e9-bac7aa08b2aa','product','Продукт');

CREATE TABLE da_999.business_entity (
	id uuid NOT NULL,
	"name" text NOT NULL,
	tech_name text NULL,
	definition text NULL,
	regulation text NULL,
	alt_names _text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	description text NULL,
	domain_id uuid NULL,
	parent_id uuid NULL,
	roles text NULL,
	formula text NULL,
	examples text NULL,
	link text NULL,
	datatype_id text NULL,
	limits text NULL,
	CONSTRAINT business_entity_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.business_entity_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	tech_name text NULL,
	definition text NULL,
	regulation text NULL,
	alt_names _text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	description text NULL,
	domain_id uuid NULL,
	parent_id uuid NULL,
	formula text NULL,
	examples text NULL,
	link text NULL,
	datatype_id text NULL,
	limits text NULL,
	roles text NULL
);

CREATE TABLE da_999."comment" (
	id uuid NOT NULL,
	comment_text text NOT NULL,
	parent_comment_id uuid NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT comment_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.comment_hist (
	id uuid NOT NULL,
	comment_text text NOT NULL,
	parent_comment_id uuid NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.custom_attribute (
	id uuid NOT NULL,
	definition_id uuid NOT NULL,
	object_id uuid NOT NULL,
	object_type text NOT NULL,
	date_value timestamp NULL,
	number_value numeric NULL,
	text_value text NULL,
	def_element_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT custom_attribute_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.custom_attribute_defelement (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	definition_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT custom_attribute_defelement_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.custom_attribute_defelement_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	definition_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.custom_attribute_definition (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	"type" text NOT NULL,
	multiple_values bool NOT NULL,
	default_value text NULL,
	placeholder text NULL,
	minimum numeric NULL,
	maximum numeric NULL,
	min_length int4 NULL,
	max_length int4 NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	artifact_types _text NULL,
	required bool NOT NULL DEFAULT false,
	CONSTRAINT custom_attribute_definition_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.custom_attribute_definition_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	"type" text NOT NULL,
	multiple_values bool NOT NULL,
	default_value text NULL,
	placeholder text NULL,
	minimum numeric NULL,
	maximum numeric NULL,
	min_length int4 NULL,
	max_length int4 NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	artifact_types _text NULL
);

CREATE TABLE da_999.custom_attribute_hist (
	id uuid NOT NULL,
	definition_id uuid NOT NULL,
	object_id uuid NOT NULL,
	object_type text NOT NULL,
	date_value timestamp NULL,
	number_value numeric NULL,
	text_value text NULL,
	def_element_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.data_asset (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	system_id uuid NULL,
	domain_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	entity_id uuid NULL,
	rows_count int8 NULL,
	data_size int8 NULL,
	state text NULL,
	history_id int4 NULL DEFAULT 0,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	roles text NULL,
	CONSTRAINT data_asset_pk PRIMARY KEY (id)
);
CREATE INDEX data_asset_name_idx ON da_999.data_asset USING btree (name);

CREATE TABLE da_999.data_asset_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	system_id uuid NULL,
	domain_id uuid NULL,
	entity_id uuid NULL,
	rows_count int8 NULL,
	data_size int8 NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	ancestor_draft_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	version_id int4 NULL DEFAULT 0,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	roles text NULL
);

CREATE TABLE da_999."datatype" (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	CONSTRAINT datatype_pk PRIMARY KEY (id)
);

CREATE TABLE da_999."domain" (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	state text NOT NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	version_id int4 NOT NULL DEFAULT 0,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	CONSTRAINT domain_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.domain_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	state text NOT NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	version_id int4 NOT NULL DEFAULT 0,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL
);

CREATE TABLE da_999.dq_log (
	id serial4 NOT NULL,
	"date" timestamp NOT NULL,
	status int4 NOT NULL,
	description text NULL,
	rule_id text NOT NULL,
	tenant text NULL,
	CONSTRAINT dq_log_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.dq_rule (
	id uuid NOT NULL,
	"name" text NOT NULL,
	rule_ref text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	description text NULL,
	settings text NULL,
	rule_type_id uuid NULL,
	CONSTRAINT dq_rule_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.dq_rule_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	rule_ref text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	description text NULL,
	settings text NULL,
	rule_type_id uuid NULL
);

CREATE TABLE da_999.dq_tasks (
	id uuid NOT NULL,
	"time" timestamp NULL,
	rule_id uuid NULL,
	state int4 NULL,
	CONSTRAINT dq_tasks_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_folder_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	history_id int4 NULL DEFAULT 0,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	loc varchar(200) NULL,
	roles text NULL,
	CONSTRAINT entity_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_attribute (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_id uuid NOT NULL,
	attribute_type text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	enumeration_id uuid NULL,
	attribute_id uuid NULL,
	CONSTRAINT entity_attribute_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_attribute_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_id uuid NOT NULL,
	attribute_type text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	enumeration_id uuid NULL,
	attribute_id uuid NULL
);

CREATE TABLE da_999.entity_attribute_to_sample_property (
	id uuid NOT NULL,
	entity_attribute_id uuid NOT NULL,
	entity_sample_property_id uuid NOT NULL,
	"name" text NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT entity_attribute_to_sample_property_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_attribute_type (
	id varchar(200) NOT NULL,
	"name" text NOT NULL,
	CONSTRAINT entity_attribute_type_pk PRIMARY KEY (id)
);

INSERT INTO da_999.entity_attribute_type (id,"name") VALUES
	 ('TEXT','Текстовый'),
	 ('INTEGER','Целочисленный'),
	 ('NUMERIC','С плавающей точкой'),
	 ('BOOLEAN','Логический'),
	 ('DATE','Дата');


CREATE TABLE da_999.entity_folder (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	parent_id uuid NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT entity_folder_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_folder_id uuid NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	ancestor_draft_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	version_id int4 NULL DEFAULT 0,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	loc varchar(200) NULL,
	roles text NULL
);

CREATE TABLE da_999.entity_query (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	query_text text NULL,
	entity_id uuid NOT NULL,
	system_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	CONSTRAINT entity_query_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_query_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	query_text text NULL,
	entity_id uuid NOT NULL,
	system_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	published_version_id int4 NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	ancestor_draft_id uuid NULL
);

CREATE TABLE da_999.entity_sample (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_id uuid NOT NULL,
	system_id uuid NULL,
	entity_query_id uuid NULL,
	sample_type text NOT NULL,
	last_updated timestamp NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	is_main bool NULL DEFAULT false,
	entity_query_version_id int4 NULL,
	roles text NULL,
	CONSTRAINT entity_sample_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_sample_body (
	entity_sample_id uuid NOT NULL,
	sample_body text NOT NULL,
	CONSTRAINT entity_sample_body_pk PRIMARY KEY (entity_sample_id)
);

CREATE TABLE da_999.entity_sample_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	entity_id uuid NOT NULL,
	system_id uuid NULL,
	entity_query_id uuid NULL,
	sample_type text NOT NULL,
	last_updated timestamp NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	is_main bool NULL,
	entity_query_version_id int4 NULL,
	roles text NULL
);

CREATE TABLE da_999.entity_sample_property (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	path_type text NULL,
	"path" text NULL,
	entity_sample_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	property_type text NOT NULL,
	CONSTRAINT entity_sample_property_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_sample_property_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	path_type text NULL,
	"path" text NULL,
	entity_sample_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.entity_sample_to_dq_rule (
	id uuid NOT NULL,
	entity_sample_id uuid NULL,
	dq_rule_id uuid NOT NULL,
	settings text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	disabled bool NULL DEFAULT false,
	indicator_id uuid NULL,
	product_id uuid NULL,
	send_mail bool NULL DEFAULT true,
	asset_id uuid NULL,
	CONSTRAINT entity_sample_to_dq_rule_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.entity_to_system (
	id uuid NOT NULL,
	system_id uuid NOT NULL,
	entity_id uuid NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT entity_to_system_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.enumeration (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	variants _text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL,
	created timestamp NULL,
	creator text NULL,
	modified timestamp NULL,
	modifier text NULL,
	CONSTRAINT enumeration_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.exceptions_log (
	id bigserial NOT NULL,
	"date" timestamp NOT NULL,
	message text NOT NULL,
	"type" text NULL,
	CONSTRAINT exceptions_log_pk PRIMARY KEY (id)
);

CREATE TABLE da_999."indicator" (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	calc_code text NULL,
	dq_checks _text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	formula text NULL,
	domain_id uuid NULL,
	indicator_type_id uuid NULL,
	examples text NULL,
	link text NULL,
	datatype_id text NULL,
	limits text NULL,
	limits_internal text NULL,
	roles text NULL,
	CONSTRAINT indicator_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.indicator_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	calc_code text NULL,
	dq_checks _text NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	history_id int4 NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	formula text NULL,
	domain_id uuid NULL,
	indicator_type_id uuid NULL,
	examples text NULL,
	link text NULL,
	datatype_id text NULL,
	limits text NULL,
	limits_internal text NULL,
	roles text NULL
);

CREATE TABLE da_999.indicator_type (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	CONSTRAINT indicator_type_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.model_links (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	from_node_id uuid NOT NULL,
	to_node_id uuid NOT NULL,
	points text NULL,
	CONSTRAINT model_links_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.model_nodes (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	node_id uuid NOT NULL,
	parent_node_id uuid NULL,
	node_artifact_type varchar(100) NULL,
	loc varchar(200) NULL,
	artifact_type varchar(100) NULL,
	CONSTRAINT model_nodes_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.openlinage_log (
	id uuid NOT NULL,
	event_time timestamp NULL,
	event_type varchar NULL,
	producer varchar NULL,
	run_id uuid NULL,
	parent_run_id uuid NULL,
	nominal_end_time timestamp NULL,
	nominal_start_time timestamp NULL,
	CONSTRAINT openlinage_log_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.openlinage_log_assertions (
	"assertion" varchar NULL,
	"column" varchar NULL,
	success bool NULL,
	id uuid NOT NULL,
	ol_id uuid NULL,
	state varchar NULL,
	msg varchar NULL,
	rule_id uuid NULL,
	CONSTRAINT openlinage_log_assertions_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.openlinage_log_inputs (
	"name" varchar NULL,
	ns varchar NULL,
	id uuid NOT NULL,
	ol_id uuid NULL,
	CONSTRAINT openlinage_log_inputs_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.openlinage_log_outputs (
	"name" varchar NULL,
	ns varchar NULL,
	id uuid NOT NULL,
	ol_id uuid NULL,
	CONSTRAINT openlinage_log_outputs_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.product (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	history_id int4 NULL DEFAULT 0,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	domain_id uuid NULL,
	problem text NULL,
	consumer text NULL,
	value text NULL,
	finance_source text NULL,
	link text NULL,
	limits text NULL,
	limits_internal text NULL,
	roles text NULL,
	CONSTRAINT product_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.product_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	ancestor_draft_id uuid NULL,
	published_id uuid NULL,
	published_version_id int4 NULL,
	version_id int4 NULL DEFAULT 0,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	domain_id uuid NULL,
	problem text NULL,
	consumer text NULL,
	value text NULL,
	finance_source text NULL,
	link text NULL,
	limits text NULL,
	limits_internal text NULL,
	roles text NULL
);

CREATE TABLE da_999.product_supply_variant (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	CONSTRAINT product_supply_variant_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.product_type (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	CONSTRAINT product_type_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.rating (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	user_id text NOT NULL,
	rating int2 NOT NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	CONSTRAINT rating_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.recent_view (
	id uuid NOT NULL,
	user_id text NOT NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	viewed_time timestamp NOT NULL,
	CONSTRAINT recent_view_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.reference (
	id uuid NOT NULL,
	source_id uuid NOT NULL,
	source_artifact_type text NOT NULL,
	target_id uuid NOT NULL,
	target_artifact_type text NOT NULL,
	reference_type text NOT NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	published_id uuid NULL,
	points text NULL,
	CONSTRAINT reference_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.reference_hist (
	id uuid NOT NULL,
	source_id uuid NOT NULL,
	source_artifact_type text NOT NULL,
	target_id uuid NOT NULL,
	target_artifact_type text NOT NULL,
	reference_type text NOT NULL,
	history_start timestamp NOT NULL,
	history_end timestamp NOT NULL,
	version_id int4 NOT NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	published_id uuid NULL,
	points text NULL
);

CREATE TABLE da_999.rule_type (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	CONSTRAINT rule_type_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.steward (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	user_id int4 NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT steward_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.steward_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	user_id int4 NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.steward_to_domain (
	id uuid NOT NULL,
	domain_id uuid NOT NULL,
	steward_id uuid NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT steward_domain_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999."system" (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	system_type text NOT NULL,
	connector_id uuid NULL,
	system_folder_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	version_id int4 NULL DEFAULT 0,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	business_owner text NULL,
	it_owner text NULL,
	architector text NULL,
	reliability text NULL,
	CONSTRAINT system_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.system_connection (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	connector_id uuid NOT NULL,
	system_id uuid NOT NULL,
	enabled bool NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_connection_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.system_connection_param (
	id uuid NOT NULL,
	system_connection_id uuid NOT NULL,
	connector_param_id uuid NOT NULL,
	param_value text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_connection_param_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.system_folder (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	parent_id uuid NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_folder_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.system_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	system_type text NOT NULL,
	connector_id uuid NULL,
	system_folder_id uuid NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	history_id int4 NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	state text NULL,
	workflow_task_id uuid NULL,
	published_id uuid NULL,
	version_id int4 NULL,
	ancestor_draft_id uuid NULL,
	published_version_id int4 NULL,
	business_owner text NULL,
	it_owner text NULL,
	architector text NULL,
	reliability text NULL
);

CREATE TABLE da_999.system_to_domain (
	id uuid NOT NULL,
	domain_id uuid NOT NULL,
	system_id uuid NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_to_domain_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.system_type (
	id text NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_type_pk PRIMARY KEY (id)
);

INSERT INTO da_999.system_type (id,"name",description,created,creator,modified,modifier) VALUES
	 ('relational_database','relational_database','relational_database','2023-05-26 08:34:55.015','1000000','2023-05-26 08:34:55.015','1000000'),
	 ('rest_api','rest_api','rest_api','2023-05-26 08:34:55.019','1000000','2023-05-26 08:34:55.019','1000000');

CREATE TABLE da_999.tag (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	tag_category_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT tag_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.tag_category (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT tag_category_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.tag_category_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.tag_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	tag_category_id uuid NOT NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.tag_to_artifact (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	tag_id uuid NOT NULL,
	description text NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT tag_to_artifact_pk PRIMARY KEY (id)
);
CREATE INDEX idx_tag_to_artifact_hist_dates_1001 ON da_999.tag_to_artifact USING btree (history_start, history_end);
CREATE INDEX idx_tag_to_artifact_id_1001 ON da_999.tag_to_artifact USING btree (id);

CREATE TABLE da_999.tag_to_artifact_hist (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	tag_id uuid NOT NULL,
	description text NULL,
	history_start timestamp NULL,
	history_end timestamp NULL,
	version_id int4 NULL DEFAULT 0,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL
);

CREATE TABLE da_999.task (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	system_connection_id uuid NOT NULL,
	query_id uuid NOT NULL,
	enabled bool NOT NULL,
	schedule_type text NOT NULL,
	schedule_params text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT task_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.task_run (
	id uuid NOT NULL,
	task_id uuid NOT NULL,
	result_sample_id uuid NULL,
	result_sample_version_id int4 NULL,
	result_msg text NULL,
	stared_by text NOT NULL,
	start_mode text NULL,
	task_start timestamp NOT NULL,
	task_end timestamp NULL,
	task_state text NULL,
	last_updated timestamp NOT NULL,
	CONSTRAINT task_run_pkey PRIMARY KEY (id)
);

CREATE TABLE da_999.user_to_domain (
	id uuid NOT NULL,
	user_id int4 NOT NULL,
	domain_id uuid NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT user_to_domain_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.workflow (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	workflow_type text NULL,
	CONSTRAINT workflow_pk PRIMARY KEY (id)
);

INSERT INTO da_999.workflow (id,"name",description,created,creator,modified,modifier,workflow_type) VALUES
	 ('8f3d72b6-c3bb-4825-93b5-03ea1d86fffa','One step publish','Desc','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000','PUBLISH'),
	 ('7b234cc3-254a-4450-99b4-9b0b0bf2bbed','One step removal','One step artifact removal','2023-02-19 00:00:00','1000000','2023-02-19 00:00:00','1000000','REMOVE');


CREATE TABLE da_999.workflow_settings (
	artifact_type text NOT NULL,
	artifact_action text NOT NULL,
	process_definition_key text NOT NULL,
	description text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	id uuid NULL,
	CONSTRAINT workflow_settings_pk PRIMARY KEY (artifact_type, artifact_action)
);

INSERT INTO da_999.workflow_settings (artifact_type,artifact_action,process_definition_key,description,created,creator,modified,modifier,id) VALUES
	 ('business_entity','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','e97decb0-6e62-4957-a915-573bf8fb21e4'),
	 ('business_entity','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','be906f0b-81f3-43cd-bf97-b0c96d7d884b'),
	 ('business_entity','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','27b73453-f02f-4e72-82fa-552433d1fb3a'),
	 ('domain','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','a803f6ad-2d8f-473f-8d9b-5693329cc66f'),
	 ('system','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','ee4ffa94-ab4d-4367-9a14-f36d19d5b210'),
	 ('entity','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','e78aaf57-3bba-483c-bfef-e16013d68db2'),
	 ('entity_query','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','428a52bf-244e-41c9-af5d-d666d2e95899'),
	 ('data_asset','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','31a0b453-d43e-4bb6-8a0b-30f3bbe43218'),
	 ('indicator','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','9d198e7e-efda-461c-a593-3098da21862c'),
	 ('product','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','ed7b2e29-a89d-4e4c-8b70-6806d73ca89e');
INSERT INTO da_999.workflow_settings (artifact_type,artifact_action,process_definition_key,description,created,creator,modified,modifier,id) VALUES
	 ('dq_rule','CREATE','lottabyteOneStepApproval','создание','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','75248d20-ecc5-430d-9183-21257c98dc0b'),
	 ('domain','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','a03c2c06-8bc0-44e8-985b-06ce1d95df82'),
	 ('system','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','8733f7ae-a242-4417-abb5-45d763735b00'),
	 ('entity','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','49e9c854-194f-41cd-aefb-d1b8059cee51'),
	 ('entity_query','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','faaafa59-07ff-41db-b42a-1dd481b7052c'),
	 ('data_asset','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','95c5afe8-a407-4860-9a71-f20ad9f3b76e'),
	 ('indicator','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','f1c5922a-4940-430b-8198-09220f7f8c19'),
	 ('product','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','56e83521-d2ce-43a4-bb08-06a2914f0abb'),
	 ('dq_rule','UPDATE','lottabyteOneStepApproval','изменение','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','36d859fc-42e2-4e57-9b6d-ca9173baa26e'),
	 ('domain','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','36af0775-3bcd-40d8-a66e-ed00825e4c21');
INSERT INTO da_999.workflow_settings (artifact_type,artifact_action,process_definition_key,description,created,creator,modified,modifier,id) VALUES
	 ('system','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','9ed94ee3-196b-41fd-9a84-9ea1f3d01c64'),
	 ('entity','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','ee9e9119-0caf-44db-9f5a-87fc59cf3a6f'),
	 ('entity_query','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','2b3a4969-8d85-491d-94a6-40d70881bf75'),
	 ('data_asset','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','50e7a27e-d329-4032-b479-93b9751c7749'),
	 ('indicator','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','f30ba3ee-19e7-47da-b3bc-e04c8ad83a7a'),
	 ('product','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','7e89d5c7-8546-4dc6-816b-b13572d8d804'),
	 ('dq_rule','REMOVE','lottabyteOneStepRemoval','удаление','2023-11-20 08:12:49.636','1000000','2023-11-20 08:12:49.636','1000000','58d6a807-3da9-45a4-a5ac-72ff482fd2ce');


CREATE TABLE da_999.workflow_state (
	state text NOT NULL,
	"name" text NOT NULL,
	user_name text NOT NULL,
	CONSTRAINT workflow_state_pk PRIMARY KEY (state)
);

INSERT INTO da_999.workflow_state (state,"name",user_name) VALUES
	 ('Approve artifact','Публикация ','Владелец домена'),
	 ('Review artifact','Проверка корректности. Согласование.','Владелец домена'),
	 ('Send artifact to Review','Создание.','Аналитик данных'),
	 ('NOT_STARTED','Создание.','Владелец домена'),
	 ('MARKED_FOR_REMOVAL','Удаление','Владелец домена'),
	 ('Review artifact data_asset','Проверка корректности. Согласование.','Анна Сергеева'),
	 ('Approve data_asset','Публикация ','Николай Петров');


CREATE TABLE da_999.workflow_task (
	id uuid NOT NULL,
	artifact_id uuid NOT NULL,
	artifact_type text NOT NULL,
	workflow_id uuid NOT NULL,
	workflow_state text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT workflow_task_pk PRIMARY KEY (id)
);

CREATE TABLE da_999.workflow_task_action (
	id uuid NOT NULL,
	workflow_task_id uuid NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	workflow_action_id text NOT NULL,
	CONSTRAINT workflow_task_action_log PRIMARY KEY (id)
);

CREATE TABLE da_999.workflow_task_action_param (
	id uuid NOT NULL,
	workflow_task_action_id uuid NOT NULL,
	param_id uuid NOT NULL,
	param_name text NOT NULL,
	param_type text NOT NULL,
	param_value text NOT NULL
);

CREATE OR REPLACE VIEW da_999."Tasks, queries and samples"
AS SELECT DISTINCT es.name AS "Entity Sample Name",
    eq.name AS "Entity Query Name",
    eq.query_text AS "Query Text",
    s.name AS "System Name",
    t.name AS "Task Name",
    t.schedule_type AS "Task Type",
    t.schedule_params AS "Task Schedule",
    t.enabled AS "Task enabled?",
    ( SELECT tr.task_state
           FROM da_999.task_run tr
          WHERE tr.task_id = t.id
          ORDER BY tr.last_updated DESC
         LIMIT 1) AS "Task Last run status",
    ( SELECT tr.result_msg
           FROM da_999.task_run tr
          WHERE tr.task_id = t.id
          ORDER BY tr.last_updated DESC
         LIMIT 1) AS "Task Last run message",
    es.id AS "Entity Sample ID",
    eq.id AS "Entity Query ID",
    s.id AS "System ID",
    t.id AS "Task ID",
    '<______________>'::text AS "<______________>",
    sc.name AS "Connection Name",
    sc.description AS "Connection Description",
    con.name AS "Connection Type",
    cp.display_name AS "Connection Parameter",
    scp.param_value AS "Connection Parameter value",
    sc.enabled AS "Connection enabled?",
    sc.id AS "Connection ID"
   FROM da_999.task t
     LEFT JOIN da_999.system_connection sc ON t.system_connection_id = sc.id
     LEFT JOIN da_999.system s ON sc.system_id = s.id
     LEFT JOIN da_999.system_connection_param scp ON sc.id = scp.system_connection_id
     LEFT JOIN da.connector con ON s.connector_id = con.id
     LEFT JOIN da.connector_param cp ON scp.connector_param_id = cp.id
     LEFT JOIN da_999.entity_query eq ON eq.id = t.query_id
     LEFT JOIN da_999.entity_sample es ON eq.id = es.entity_query_id
  WHERE es.id IS NOT NULL
  ORDER BY t.name, t.id, cp.display_name;

CREATE OR REPLACE VIEW da_999.connector_params
AS SELECT sc.name AS connection_name,
    cp.name AS param_name,
    scp.param_value,
    scp.system_connection_id
   FROM da_999.system_connection_param scp
     LEFT JOIN da.connector_param cp ON cp.id = scp.connector_param_id
     LEFT JOIN da_999.system_connection sc ON sc.id = scp.system_connection_id;

CREATE OR REPLACE VIEW da_999.dq_rule_tasks
AS SELECT r.id,
    s.id AS system_id,
    s.name AS system_name,
    dr.name AS rule_name,
    dr.id AS rule_id,
    dr.rule_ref,
    r.settings AS rule_settings,
    eq.name AS query_name,
    r.id AS entity_sample_to_dq_rule_id,
    e.name AS entity_name,
    p.name AS product_name,
    i.name AS indicator_name,
    da.name AS data_asset_name,
    p.id AS product_id,
    i.id AS indicator_id,
    da.id AS data_asset_id,
    es.id AS entity_sample_id,
    es.name AS entity_sample_name,
        CASE
            WHEN r.settings ~~ '%crontab%'::text THEN 1
            ELSE 0
        END AS is_crontab
   FROM da_999.entity_sample_to_dq_rule r
     LEFT JOIN da_999.dq_rule dr ON r.dq_rule_id = dr.id
     LEFT JOIN da_999.entity_sample es ON r.entity_sample_id = es.id
     LEFT JOIN da_999.entity_query eq ON es.entity_query_id = eq.id
     LEFT JOIN da_999.entity e ON e.id = eq.entity_id
     LEFT JOIN da_999.task t ON t.query_id = eq.id
     LEFT JOIN da_999.indicator i ON i.id = r.indicator_id
     LEFT JOIN da_999.product p ON p.id = r.product_id
     LEFT JOIN da_999.system s ON s.id = eq.system_id
     LEFT JOIN da_999.data_asset da ON r.asset_id = da.id
  WHERE (r.disabled = false OR r.disabled IS NULL) AND (p.state = 'PUBLISHED'::text OR p.state IS NULL) AND (i.state = 'PUBLISHED'::text OR i.state IS NULL) AND (da.state = 'PUBLISHED'::text OR da.state IS NULL) AND dr.rule_ref IS NOT NULL;

 
 CREATE OR REPLACE FUNCTION da_999.provide_parent_name(parent_id uuid)
 RETURNS text
 LANGUAGE plpgsql
AS $function$
declare
   rec record; 
   res text;
   last_id uuid;
   last_prev_id uuid;
  
begin
   	select  concat(edai."name",' (',sdai."name" ,')') system_producer, edai."name" input_name  into rec    
  	from da_999.openlinage_log dq
	left join da_999.openlinage_log_inputs oli on oli.ol_id = dq.id 
	left join da_999.openlinage_log_outputs olo  on olo.ol_id = dq.id
	left join da_999.data_asset dai on   dai.id = uuid(oli.ns)
	left join da_999.system sdai on   sdai.id = dai.system_id 
	left join da_999.entity edai on   edai.id = dai.entity_id
	left join da_999.data_asset dao on dao.id = uuid(olo.ns)
	left join da_999.system sdao on   sdao.id = dao.system_id 
   	where dq.run_id  = parent_id and event_type = 'START' limit 1;
   	res = rec.system_producer;
   	if rec.input_name is null then res = ''; end if;
   	
   	last_id = parent_id;
   	while last_id is not null loop
      last_prev_id = last_id;
	  select  dq.run_id  into rec from da_999.openlinage_log dq 
	 where dq.parent_run_id = last_id and event_type = 'START'  and  dq.producer <> 'DQ tool' limit 1;
      last_id = rec.run_id ;
	  
   	end loop;
   	
   	select  concat(edao."name",' (',sdao."name" ,')') system_producer, edao."name" output_name  into rec    
  	from da_999.openlinage_log dq
	left join da_999.openlinage_log_inputs oli on oli.ol_id = dq.id 
	left join da_999.openlinage_log_outputs olo  on olo.ol_id = dq.id
	left join da_999.data_asset dai on   dai.id = uuid(oli.ns)
	left join da_999.system sdai on   sdai.id = dai.system_id 
	left join da_999.entity edai on   edai.id = dai.entity_id
	left join da_999.data_asset dao on dao.id = uuid(olo.ns)
	left join da_999.system sdao on   sdao.id = dao.system_id 
	left join da_999.entity edao on   edao.id = dao.entity_id
   	where dq.run_id  = last_prev_id and event_type = 'START' and dq.producer <> 'DQ tool'  limit 1;
   
   	if rec.output_name is null then 
   		return res ;  
   	else
   		return concat(res ,' - ',rec.system_producer) ; 
   	end if;
end;
$function$
;
 
CREATE OR REPLACE VIEW da_999.openlinage_log_assertions_monitor
AS SELECT
        CASE
            WHEN t.state::text = '0'::text THEN 'Ошибка'::text
            WHEN t.state::text = '1'::text THEN 'Предупреждение'::text
            ELSE 'Успешно'::text
        END AS state_name,
    dr.name AS rule_name,
    t."column",
    t.msg,
    t.rule_id,
    t.ol_id,
    t.id,
    ol.run_id,
    t.assertion,
    t.state
   FROM da_999.openlinage_log_assertions t
     LEFT JOIN da_999.entity_sample_to_dq_rule estdr ON estdr.id = t.rule_id
     LEFT JOIN da_999.dq_rule dr ON dr.id = estdr.dq_rule_id
     LEFT JOIN da_999.openlinage_log ol ON ol.id = t.ol_id;

CREATE OR REPLACE VIEW da_999.openlinage_log_monitor
AS SELECT
        CASE
            WHEN (EXISTS ( SELECT 1
               FROM da_999.openlinage_log ol1
              WHERE ol1.event_type::text = 'FAIL'::text AND ol1.run_id = t.run_id)) THEN 'Ошибка выполнения'::text
            WHEN NOT (EXISTS ( SELECT 1
               FROM da_999.openlinage_log ol1
              WHERE ol1.event_type::text = 'COMPLETE'::text AND ol1.run_id = t.run_id)) THEN 'Незавершен'::text
            WHEN t.state = '0'::text THEN 'Ошибка'::text
            WHEN t.state = '1'::text THEN 'Предупреждение'::text
            ELSE 'Успешно'::text
        END AS state_name,
        CASE
            WHEN (EXISTS ( SELECT 1
               FROM da_999.openlinage_log ol1
              WHERE ol1.event_type::text = 'FAIL'::text AND ol1.run_id = t.run_id)) THEN 'Ошибка выполнения'::text
            WHEN NOT (EXISTS ( SELECT 1
               FROM da_999.openlinage_log ol1
              WHERE ol1.event_type::text = 'COMPLETE'::text AND ol1.run_id = t.run_id)) THEN 'Незавершен'::text
            WHEN t.state_local = '0'::text THEN 'Ошибка'::text
            WHEN t.state_local = '1'::text THEN 'Предупреждение'::text
            ELSE 'Успешно'::text
        END AS state_name_local,
    ( SELECT a.msg
           FROM da_999.openlinage_log_assertions a
          WHERE t.state = a.state::text AND a.msg IS NOT NULL AND a.ol_id = t.id OR (a.ol_id IN ( SELECT dq1.id
                   FROM da_999.openlinage_log dq1
                  WHERE dq1.parent_run_id = t.run_id))
         LIMIT 1) AS assertion_msg,
    t.state,
    t.state_local,
    t.id,
    t.run_id,
    t.parent_run_id,
    t.event_type,
    t.event_time,
    t.full_name,
    t.system_producer,
    t.input_asset_domain_id,
    t.output_asset_domain_id,
    t.output_asset_domain_name,
    t.input_asset_domain_name,
    t.system_producer_parent,
    t.input_name,
    t.input_ns,
    t.input_asset_name,
    t.input_id,
    t.input_system_id,
    t.input_system_name,
    t.output_name,
    t.output_ns,
    t.output_asset_name,
    t.output_id,
    t.output_system_id,
    t.output_system_name,
    t.producer,
    t.output_asset_id,
    t.input_asset_id
   FROM ( SELECT ( SELECT min(a.state::text) AS min
                   FROM da_999.openlinage_log_assertions a
                  WHERE a.ol_id = dq.id OR (a.ol_id IN ( SELECT dq1.id
                           FROM da_999.openlinage_log dq1
                          WHERE dq1.parent_run_id = dq.run_id))) AS state,
            ( SELECT min(a.state::text) AS min
                   FROM da_999.openlinage_log_assertions a
                  WHERE (a.ol_id IN ( SELECT dq1.id
                           FROM da_999.openlinage_log dq1
                          WHERE dq1.run_id = dq.run_id))) AS state_local,
            dq.id,
            dq.run_id,
            dq.parent_run_id,
            dq.event_type,
            dq.event_time,
            da_999.provide_parent_name(dq.run_id) AS full_name,
            sdai.name AS system_producer,
            dai.domain_id AS input_asset_domain_id,
            dao.domain_id AS output_asset_domain_id,
            daod.name AS output_asset_domain_name,
            daid.name AS input_asset_domain_name,
            da_999.provide_parent_name(dq.run_id) AS system_producer_parent,
            oli.name AS input_name,
            oli.ns AS input_ns,
            dai.name AS input_asset_name,
            oli.id AS input_id,
            sdai.id AS input_system_id,
            sdai.name AS input_system_name,
            olo.name AS output_name,
            olo.ns AS output_ns,
            dao.name AS output_asset_name,
            olo.id AS output_id,
            sdao.id AS output_system_id,
            sdao.name AS output_system_name,
            dq.producer,
            dao.id AS output_asset_id,
            dai.id AS input_asset_id
           FROM da_999.openlinage_log dq
             LEFT JOIN da_999.openlinage_log_inputs oli ON oli.ol_id = dq.id
             LEFT JOIN da_999.openlinage_log_outputs olo ON olo.ol_id = dq.id
             LEFT JOIN da_999.data_asset dai ON dai.id = oli.ns::uuid
             LEFT JOIN da_999.system sdai ON sdai.id = dai.system_id
             LEFT JOIN da_999.data_asset dao ON dao.id = olo.ns::uuid
             LEFT JOIN da_999.system sdao ON sdao.id = dao.system_id
             LEFT JOIN da_999.domain daod ON daod.id = dao.domain_id
             LEFT JOIN da_999.domain daid ON daid.id = dai.domain_id
          WHERE dq.event_type::text = 'START'::text
          ORDER BY dq.run_id, dq.event_time) t;

CREATE OR REPLACE VIEW da_999.openlinage_log_monitor_draft
AS SELECT t.state_name,
    t.state_name_local,
    t.assertion_msg,
    t.state,
    t.state_local,
    t.id,
    t.run_id,
    t.parent_run_id,
    t.event_type,
    t.event_time,
    t.full_name,
    t.system_producer,
    t.input_asset_domain_id,
    t.output_asset_domain_id,
    t.output_asset_domain_name,
    t.input_asset_domain_name,
    t.system_producer_parent,
    t.input_name,
    t.input_ns,
    t.input_asset_name,
    t.input_id,
    t.input_system_id,
    t.input_system_name,
    t.output_name,
    t.output_ns,
    t.output_asset_name,
    t.output_id,
    t.output_system_id,
    t.output_system_name,
    t.producer,
    t.output_asset_id,
    t.input_asset_id
   FROM da_999.openlinage_log_monitor t
  WHERE t.parent_run_id IS NULL;

CREATE OR REPLACE VIEW da_999."Атрибуты в активах"
AS SELECT asset.name AS "Имя актива",
    def.name AS "Имя атрибута",
    def.type AS "Тип атрибута",
    defelem.name AS "Значение если Enumerated",
    atr.date_value AS "Значение Дата",
    atr.number_value AS "Значение Число",
    atr.text_value AS "Значение Текст",
    atr.version_id,
    atr.created,
    atr.creator,
    atr.modified,
    atr.modifier,
    atr.id
   FROM da_999.custom_attribute atr
     LEFT JOIN da_999.data_asset asset ON asset.id = atr.object_id
     LEFT JOIN da_999.custom_attribute_definition def ON atr.definition_id = def.id
     LEFT JOIN da_999.custom_attribute_defelement defelem ON atr.def_element_id = defelem.id
  WHERE atr.object_type = 'data_asset'::text;

CREATE OR REPLACE VIEW da_999."Таски и коннекшены"
AS SELECT DISTINCT t.id,
    t.name AS "Название таска",
    t.description AS "Описание таска",
    eq.query_text AS "Запрос",
    s.name AS "Название системы",
    s.description AS "Описание системы",
    t.schedule_type AS "Тип таска",
    t.schedule_params AS "Параметры расписания таска",
    t.enabled AS "Таск включен?",
    ( SELECT tr.task_state
           FROM da_999.task_run tr
          WHERE tr.task_id = t.id
          ORDER BY tr.last_updated DESC
         LIMIT 1) AS "Статус последнего запуска",
    ( SELECT tr.result_msg
           FROM da_999.task_run tr
          WHERE tr.task_id = t.id
          ORDER BY tr.last_updated DESC
         LIMIT 1) AS "Сообщение последнего запуска",
    '|'::text AS "|",
    sc.name AS "Название коннекшена",
    sc.description AS "Описание коннекшена",
    con.name AS "Тип коннектора",
    cp.display_name AS "Параметр",
    scp.param_value AS "Значение параметра",
    sc.enabled AS "Коннекшен включен?"
   FROM da_999.task t
     LEFT JOIN da_999.system_connection sc ON t.system_connection_id = sc.id
     LEFT JOIN da_999.system s ON sc.system_id = s.id
     LEFT JOIN da_999.system_connection_param scp ON sc.id = scp.system_connection_id
     LEFT JOIN da.connector con ON s.connector_id = con.id
     LEFT JOIN da.connector_param cp ON scp.connector_param_id = cp.id
     LEFT JOIN da_999.entity_query eq ON eq.id = t.query_id
  ORDER BY t.name, t.id, cp.display_name;

CREATE OR REPLACE FUNCTION da_999.business_entity_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin
   if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.business_entity be where id = new.id;
		select max(history_id) into max_history_id from da_999.business_entity where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.business_entity_hist
        (id, name, tech_name, definition, regulation, alt_names, 
        state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id, domain_id)
        values(old.id, old.name, old.tech_name, old.definition, old.regulation, old.alt_names, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id, old.domain_id);
        return new;
   elseif (TG_OP = 'DELETE') then
        insert into da_999.business_entity_hist
        (id, name, tech_name, definition, regulation, alt_names, 
        state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id, domain_id)
        VALUES(old.id, old.name, old.tech_name, old.definition, old.regulation, old.alt_names, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id, old.domain_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        
$function$
;

CREATE OR REPLACE FUNCTION da_999.comment_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.comment where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.comment_hist
        (id, comment_text, parent_comment_id, artifact_id, artifact_type, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.comment_text, old.parent_comment_id, old.artifact_id, old.artifact_type, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.comment_hist
        (id, comment_text, parent_comment_id, artifact_id, artifact_type, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.comment_text, old.parent_comment_id, old.artifact_id, old.artifact_type, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.custom_attribute_defelement_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.custom_attribute_defelement where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.custom_attribute_defelement_hist
        (id, name, description, definition_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.name, old.description, old.definition_id, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.custom_attribute_defelement_hist
        (id, name, description, definition_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.name, old.description, old.definition_id, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.custom_attribute_definition_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.custom_attribute_definition where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.custom_attribute_definition_hist
        (id, name, description, "type", multiple_values, default_value, placeholder, minimum, maximum, min_length, max_length, history_start, history_end, version_id,
        created, creator, modified, modifier, artifact_types)
        values(old.id, old.name, old.description, old."type", old.multiple_values, old.default_value, old.placeholder, old.minimum, old.maximum, old.min_length, old.max_length, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.artifact_types);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.custom_attribute_definition_hist
        (id, name, description, "type", multiple_values, default_value, placeholder, minimum, maximum, min_length, max_length, history_start, history_end, version_id,
        created, creator, modified, modifier, artifact_types)
        VALUES(old.id, old.name, old.description, old."type", old.multiple_values, old.default_value, old.placeholder, old.minimum, old.maximum, old.min_length, old.max_length, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.artifact_types);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.custom_attribute_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.custom_attribute where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.custom_attribute_hist
        (id, definition_id, object_id, object_type, date_value, number_value, text_value, def_element_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.definition_id, old.object_id, old.object_type, old.date_value, old.number_value, old.text_value, old.def_element_id,  old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.custom_attribute_hist
        (id, definition_id, object_id, object_type, date_value, number_value, text_value, def_element_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.definition_id, old.object_id, old.object_type, old.date_value, old.number_value, old.text_value, old.def_element_id, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.data_asset_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin
   if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.data_asset where id = new.id;
		select max(history_id) into max_history_id from da_999.data_asset where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.data_asset_hist
        (id, name, description, system_id, domain_id, entity_id, rows_count, data_size, 
		state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.description, old.system_id, old.domain_id, old.entity_id, old.rows_count, old.data_size,
		old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return new;
  elseif (TG_OP = 'DELETE') then
        insert into da_999.data_asset_hist
        (id, name, description, system_id, domain_id, entity_id, rows_count, data_size, 
        state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        VALUES(old.id, old.name, old.description, old.system_id, old.domain_id, old.entity_id, old.rows_count, old.data_size,
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.domain_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin 
	if (TG_OP = 'UPDATE') then
		select max(version_id) into max_version_id from da_999.domain where id = new.id;
		select max(history_id) into max_history_id from da_999.domain where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;
	
		insert into da_999.domain_hist 
		(id, name, description, state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
		values(old.id, old.name, old.description, old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		 old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
		return new;
	elseif (TG_OP = 'DELETE') then
		insert into da_999.domain_hist 
		(id, name, description, state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
		VALUES(old.id, old.name, old.description, old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		 old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
		return old;
	elseif (TG_OP = 'INSERT') then
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		return new;
	end if;
end
$function$
;

CREATE OR REPLACE FUNCTION da_999.dq_rule_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin
   if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.dq_rule be where id = new.id;
		select max(history_id) into max_history_id from da_999.dq_rule where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.dq_rule_hist
        (id, name, rule_ref,  description,
        state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.rule_ref, old.description, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return new;
   elseif (TG_OP = 'DELETE') then
        insert into da_999.dq_rule_hist
        (id, name, rule_ref,  description,
        state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.rule_ref, old.description, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.entity_attribute_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.entity_attribute where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.entity_attribute_hist
        (id, name, description, entity_id, attribute_type, history_start, history_end, version_id,
        created, creator, modified, modifier, enumeration_id, attribute_id)
        values(old.id, old.name, old.description, old.entity_id, old.attribute_type, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.enumeration_id, old.attribute_id);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.entity_attribute_hist
        (id, name, description, entity_id, attribute_type, history_start, history_end, version_id,
        created, creator, modified, modifier, enumeration_id, attribute_id)
        VALUES(old.id, old.name, old.description, old.entity_id, old.attribute_type, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.enumeration_id, old.attribute_id);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.entity_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
        max_history_id int8;
		max_version_id int8;
begin
   if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.entity where id = new.id;
		select max(history_id) into max_history_id from da_999.entity where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.entity_hist
        (id, name, description, entity_folder_id, state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.description, old.entity_folder_id, old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		 old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return new;
   elseif (TG_OP = 'DELETE') then
        insert into da_999.entity_hist
        (id, name, description, entity_folder_id, state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        VALUES(old.id, old.name, old.description, old.entity_folder_id, old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		 old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.entity_query_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin
    if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.entity_query where id = new.id;
		select max(history_id) into max_history_id from da_999.entity_query where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.entity_query_hist
        (id, name, description, query_text, entity_id, system_id, 
        state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.description, old.query_text, old.entity_id, old.system_id, 
		old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return new;
   elseif (TG_OP = 'DELETE') then
        insert into da_999.entity_query_hist
        (id, name, description, query_text, entity_id, system_id, 
         state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        VALUES(old.id, old.name, old.description, old.query_text, old.entity_id, old.system_id, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.entity_sample_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.entity_sample where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.entity_sample_hist
        (id, name, description, entity_id, system_id, entity_query_id, entity_query_version_id, sample_type, last_updated, history_start, history_end, version_id,
        created, creator, modified, modifier, is_main)
        values(old.id, old.name, old.description, old.entity_id, old.system_id, old.entity_query_id, old.entity_query_version_id, old.sample_type, old.last_updated, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.is_main);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.entity_sample_hist
        (id, name, description, entity_id, system_id, entity_query_id, entity_query_version_id, sample_type, last_updated, history_start, history_end, version_id,
        created, creator, modified, modifier, is_main)
        VALUES(old.id, old.name, old.description, old.entity_id, old.system_id, old.entity_query_id, old.entity_query_version_id, old.sample_type, old.last_updated, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier, old.is_main);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.entity_sample_property_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.entity_sample_property where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.entity_sample_property_hist
        (id, name, description, path_type, "path", entity_sample_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.name, old.description, old.path_type, old."path", old.entity_sample_id, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.entity_sample_property_hist
        (id, name, description, path_type, "path", entity_sample_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.name, old.description, old.path_type, old."path", old.entity_sample_id, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.indicator_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$

declare 
	max_history_id int8;
	max_version_id int8;
begin
   if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.indicator where id = new.id;
		select max(history_id) into max_history_id from da_999.indicator where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

   		insert into da_999.indicator_hist
        (id, name, description, calc_code, dq_checks, 
        state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id, formula, domain_id, indicator_type_id)
        values(old.id, old.name, old.description, old.calc_code, old.dq_checks, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id, old.formula, old.domain_id, old.indicator_type_id);
        return new;
   elseif (TG_OP = 'DELETE') then
        insert into da_999.indicator_hist
        (id, name, description, calc_code, dq_checks, 
        state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		created, creator, modified, modifier, ancestor_draft_id, published_version_id, formula, domain_id, indicator_type_id)
        VALUES(old.id, old.name, old.description, old.calc_code, old.dq_checks, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id, old.formula, old.domain_id, old.indicator_type_id);
        return old;
   elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        
$function$
;

CREATE OR REPLACE FUNCTION da_999.product_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin 
	if (TG_OP = 'UPDATE') then
		select max(version_id) into max_version_id from da_999.product where id = new.id;
		select max(history_id) into max_history_id from da_999.product where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		new.modified = CURRENT_TIMESTAMP;
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;
	
		insert into da_999.product_hist 
		(id, "name", description, history_start, history_end, version_id, created, creator, modified, modifier, state, workflow_task_id, published_id, history_id, ancestor_draft_id, published_version_id, domain_id, problem, consumer, value, finance_source)
		values(old.id, old.name, old.description, old.history_start, old.history_end, old.version_id, old.created, old.creator, old.modified, old.modifier, old.state, old.workflow_task_id, old.published_id, old.history_id, old.ancestor_draft_id, old.published_version_id, old.domain_id,
              old.problem, old.consumer, old.value, old.finance_source);
		return new;
	elseif (TG_OP = 'DELETE') then
		insert into da_999.product_hist 
		(id, "name", description, history_start, history_end, version_id, created, creator, modified, modifier, state, workflow_task_id, published_id, history_id, ancestor_draft_id, published_version_id, domain_id, problem, consumer, value, finance_source)
		VALUES(old.id, old.name, old.description, old.history_start, CURRENT_TIMESTAMP, old.version_id, old.created, old.creator, CURRENT_TIMESTAMP, old.modifier, old.state, old.workflow_task_id, old.published_id, old.history_id, old.ancestor_draft_id, old.published_version_id, old.domain_id, 
               old.problem, old.consumer, old.value, old.finance_source);
		return old;
	elseif (TG_OP = 'INSERT') then
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		return new;
	end if;
end
$function$
;

CREATE OR REPLACE FUNCTION da_999.provide_end_time(id_offer uuid)
 RETURNS timestamp without time zone
 LANGUAGE plpgsql
AS $function$
declare
   rec record;                                
begin
   select run_id , nominal_end_time, parent_run_id 
   into rec                                   
   from da_999.openlinage_log
   where run_id  = id_offer and event_type = 'COMPLETE';
   
   if rec.parent_run_id is null then                       
    return rec.nominal_end_time;                          
   else
    return dq.provide_end_time(rec.parent_run_id); 
   end if;
end;
$function$
;

CREATE OR REPLACE FUNCTION da_999.provide_full_name(id_offer uuid, full_name text)
 RETURNS text
 LANGUAGE plpgsql
AS $function$
declare
   rec record;                                
begin
   select run_id , system_producer, parent_run_id 
   into rec                                   
   from da_999.openlinage_log_monitor_draft
   where run_id  = id_offer and event_type = 'START';
   
   if rec.parent_run_id is null then                       
    return rec.system_producer::text ;                          
   else
    return da_999.provide_full_name(rec.parent_run_id,rec.system_producer::text + '/'+ full_name ); 
   end if;
end;
$function$
;


CREATE OR REPLACE FUNCTION da_999.reference_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare 
	max_history_id int8;
	max_version_id int8;
begin 
	if (TG_OP = 'UPDATE') then
		select max(version_id) into max_version_id from da_999.reference where id = new.id;
		select max(history_id) into max_history_id from da_999.reference where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;
	
		insert into da_999.reference_hist 
		(id, source_id, source_artifact_type, target_id, target_artifact_type, reference_type, history_start, history_end, version_id, created, creator, modified, modifier)
		values(old.id, old.source_id, old.source_artifact_type, old.target_id, old.target_artifact_type, old.reference_type, old.history_start, old.history_end, old.version_id, old.created, old.creator, old.modified, old.modifier);
		return new;
	elseif (TG_OP = 'DELETE') then
		insert into da_999.reference_hist 
		(id, source_id, source_artifact_type, target_id, target_artifact_type, reference_type, history_start, history_end, version_id, created, creator, modified, modifier)
		VALUES(old.id, old.source_id, old.source_artifact_type, old.target_id, old.target_artifact_type, old.reference_type, old.history_start, CURRENT_TIMESTAMP, old.version_id, old.created, old.creator, old.modified, old.modifier);
		return old;
	elseif (TG_OP = 'INSERT') then
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		return new;
	end if;
end
$function$
;

CREATE OR REPLACE FUNCTION da_999.steward_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.steward where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.steward_hist
        (id, name, description, user_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.name, old.description, old.user_id, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.steward_hist
        (id, name, description, user_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.name, old.description, old.user_id, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.system_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
declare
	max_history_id int8;
	max_version_id int8;
begin
    if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.system where id = new.id;
		select max(history_id) into max_history_id from da_999.system where id = new.id;
		new.history_id = max_history_id + 1;
		new.history_start = CURRENT_TIMESTAMP;
		new.history_end = '9999-12-30';
		new.modified = CURRENT_TIMESTAMP;
		if (old.state = 'PUBLISHED') then
			new.version_id = max_version_id + 1;
		end if;

        insert into da_999.system_hist
        (id, name, description, system_type, connector_id, system_folder_id, state, workflow_task_id,  published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        values(old.id, old.name, old.description, old.system_type, old.connector_id, old.system_folder_id, 
        old.state, old.workflow_task_id, old.published_id, old.version_id,  
        old.history_start, new.history_start, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return new;
    elseif (TG_OP = 'DELETE') then
        insert into da_999.system_hist
        (id, name, description, system_type, connector_id, system_folder_id, state, workflow_task_id, published_id, version_id, history_start, history_end, history_id, 
		 created, creator, modified, modifier, ancestor_draft_id, published_version_id)
        VALUES(old.id, old.name, old.description, old.system_type, old.connector_id, old.system_folder_id, 
        old.state, old.workflow_task_id, old.published_id, old.version_id, old.history_start, CURRENT_TIMESTAMP, old.history_id, 
		old.created, old.creator, old.modified, old.modifier, old.ancestor_draft_id, old.published_version_id);
        return old;
    elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.tag_category_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.tag_category where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.tag_category_hist
        (id, name, description, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.name, old.description, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.tag_category_hist
        (id, name, description, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.name, old.description, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.tag_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.tag where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.tag_hist
        (id, name, description, tag_category_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.name, old.description, old.tag_category_id, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.tag_hist
        (id, name, description, tag_category_id, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.name, old.description, old.tag_category_id, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

CREATE OR REPLACE FUNCTION da_999.tag_to_artifact_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
        declare
        max_version_id int8;
        begin
        if (TG_OP = 'UPDATE') then
        select max(version_id) into max_version_id from da_999.tag_to_artifact where id = new.id;
        new.version_id = max_version_id + 1;
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';

        insert into da_999.tag_to_artifact_hist
        (id, artifact_id, artifact_type, tag_id, description, history_start, history_end, version_id,
        created, creator, modified, modifier)
        values(old.id, old.artifact_id, old.artifact_type, old.tag_id, old.description, old.history_start, new.history_start, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return new;
        elseif (TG_OP = 'DELETE') then
        insert into da_999.tag_to_artifact_hist
        (id, artifact_id, artifact_type, tag_id, description, history_start, history_end, version_id,
        created, creator, modified, modifier)
        VALUES(old.id, old.artifact_id, old.artifact_type, old.tag_id, old.description, old.history_start, CURRENT_TIMESTAMP, old.version_id,
        old.created, old.creator, old.modified, old.modifier);
        return old;
        elseif (TG_OP = 'INSERT') then
        new.history_start = CURRENT_TIMESTAMP;
        new.history_end = '9999-12-30';
        return new;
        end if;
        end
        $function$
;

create trigger business_entity_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.business_entity for each row execute function da_999.business_entity_history();


create trigger comment_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.comment for each row execute function da_999.comment_history();


create trigger custom_attribute_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.custom_attribute for each row execute function da_999.custom_attribute_history();


create trigger custom_attribute_defelement_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.custom_attribute_defelement for each row execute function da_999.custom_attribute_defelement_history();


create trigger custom_attribute_definition_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.custom_attribute_definition for each row execute function da_999.custom_attribute_definition_history();


create trigger data_asset_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.data_asset for each row execute function da_999.data_asset_history();


create trigger domain_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.domain for each row execute function da_999.domain_history();


create trigger dq_rule_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.dq_rule for each row execute function da_999.dq_rule_history();


create trigger entity_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.entity for each row execute function da_999.entity_history();


create trigger entity_attribute_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.entity_attribute for each row execute function da_999.entity_attribute_history();


create trigger entity_query_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.entity_query for each row execute function da_999.entity_query_history();


create trigger entity_sample_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.entity_sample for each row execute function da_999.entity_sample_history();


create trigger entity_sample_property_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.entity_sample_property for each row execute function da_999.entity_sample_property_history();


create trigger indicator_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.indicator for each row execute function da_999.indicator_history();


create trigger product_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.product for each row execute function da_999.product_history();


create trigger steward_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.steward for each row execute function da_999.steward_history();


create trigger system_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.system for each row execute function da_999.system_history();


create trigger tag_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.tag for each row execute function da_999.tag_history();


create trigger tag_category_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.tag_category for each row execute function da_999.tag_category_history();


create trigger tag_to_artifact_history_trigger before
insert
    or
delete
    or
update
    on
    da_999.tag_to_artifact for each row execute function da_999.tag_to_artifact_history();


