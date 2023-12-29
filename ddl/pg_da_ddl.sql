CREATE SCHEMA da;

CREATE SCHEMA wf;

CREATE TABLE da.backup_run (
	id uuid NOT NULL,
	"path" text NULL,
	result_msg text NULL,
	tenant_id int8 NOT NULL,
	backup_start timestamp NOT NULL,
	backup_end timestamp NULL,
	backup_state text NULL,
	last_updated timestamp NOT NULL,
	CONSTRAINT backup_run_pkey PRIMARY KEY (id)
);

CREATE TABLE da.connector (
	id uuid NOT NULL,
	"name" text NOT NULL,
	system_type text NOT NULL,
	description text NULL,
	enabled bool NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT connector_pk PRIMARY KEY (id)
);

INSERT INTO da.connector (id,"name",system_type,description,enabled,created,creator,modified,modifier) VALUES
	 ('e8eed68a-3d49-4c6a-b69e-f38f241150c8','Excel File in S3 Storage Connector','excel_file','Connector to get sample data from Excel file in S3 storage',true,'2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000'),
	 ('1247e6eb-eef4-4aa3-b520-cd06a2609d4e','Generic REST API Connector','rest_api','Generic REST API connector',true,'2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000'),
	 ('f49534e2-7f95-11ed-a1eb-0242ac120002','Excel File in S3 Storage Test','excel_file','Test s3',true,'2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000'),
	 ('353362f1-c5a2-4f1f-8713-a0b46e2c6b73','JDBC Table Connector','relational_database','Generic JDBC relations database connector',true,'2022-09-01 00:00:00','1000000','2022-12-19 00:00:00','1000000');

CREATE TABLE da.connector_param (
	id uuid NOT NULL,
	connector_id uuid NOT NULL,
	show_order int4 NOT NULL,
	"name" text NOT NULL,
	display_name text NOT NULL,
	description text NULL,
	example text NULL,
	param_type text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	enum_values _text NULL,
	required bool NULL DEFAULT false,
	CONSTRAINT connector_param_pk PRIMARY KEY (id)
);

INSERT INTO da.connector_param (id,connector_id,show_order,"name",display_name,description,example,param_type,created,creator,modified,modifier,enum_values,required) VALUES
	 ('abe8563e-9571-456e-a654-9e63635420d2','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',4,'sql_limit_position','LIMIT query position (PREFIX, SUFFIX)','Specify PREFIX if limit clause is located after SELECT clause, specifa SUFFIX if limit clase is located at the end of the query','SUFFIX','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('775e1435-d303-4eb7-81a0-492aa80c1138','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',5,'sql_limit_clause','Limit clause','Specify limit clause of the database dialect','LIMIT','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('d12563d7-1204-49bf-ad6f-03b616d80dc3','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',6,'sql_record_count','Record count','Specify record count for limit query','50','INTEGER','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('f3f1c653-e9ba-4175-9a3b-b03d4408c6ef','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',7,'jdbc_driver_class_name','JDBC driver class name','Specify JDBC driver class name','org.postgresql.Driver','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('3aa1b0a5-d00d-4e4b-a9a3-7ba6d1298f27','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',8,'jdbc_command_timeout','JDBC command timeout','Specify command timeout in seconds','60','INTEGER','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('732788e4-5b63-4287-a09c-802be30232af','e8eed68a-3d49-4c6a-b69e-f38f241150c8',1,'s3_endpoint_url','S3 endpoint URL','Specify S3 endpoint URL','https://storage.yandexcloud.net','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('db8326ab-945c-4cba-9a25-798ac3a15774','e8eed68a-3d49-4c6a-b69e-f38f241150c8',2,'s3_service_name','S3 service name','Specify S3 service name','s3','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('e423bcfa-542b-428b-b295-c7d4ad702124','e8eed68a-3d49-4c6a-b69e-f38f241150c8',4,'s3_file_path','S3 file path','Specify file path inside S3 bucket','/folder1/dd0e83ab-36a5-4f0c-9fb3-13b272e15aec','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('e8eb7289-321b-4dab-ad36-254593cfa874','e8eed68a-3d49-4c6a-b69e-f38f241150c8',7,'excel_first_row_headers','Excel file contains row names in 1-st row','Specify if excel file has row name in 1-st row or range','TRUE','BOOLEAN','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('1855fb76-c95e-42d7-ba55-84a5dfdb494c','e8eed68a-3d49-4c6a-b69e-f38f241150c8',5,'s3_access_key','S3 access key','Specify S3 access key, if required','S3 access key','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false);
INSERT INTO da.connector_param (id,connector_id,show_order,"name",display_name,description,example,param_type,created,creator,modified,modifier,enum_values,required) VALUES
	 ('5d0e1333-d086-4d54-b220-bce6cd1741ce','e8eed68a-3d49-4c6a-b69e-f38f241150c8',6,'s3_secret_access_key','S3 secret access key','Specify S3 secret access key, if required','S3 secret access key','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('e8eed68a-3d49-4c6a-b69e-f38f24115012','e8eed68a-3d49-4c6a-b69e-f38f241150c8',8,'s3_signing_region','S3 signing region','Specify  S3 signing region, if required','us-east-1','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('d6e357a1-1ea7-4582-9538-80debf5e8e65','e8eed68a-3d49-4c6a-b69e-f38f241150c8',3,'s3_bucket_name','S3 bucket name','Specify S3 bucket name','bucket1','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('3cecd634-7461-487a-96dc-7aa9aeb47f9e','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',1,'restapi_base_url','REST API base url for service','Specify REST API base URL','https://service1.local/api/','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('d0fb438e-9a77-485a-87b2-86c93dc2b7ec','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',3,'restapi_basicauth_username','REST API basic auth username','Specify Username for basic auth','admin','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('efa2513b-1f7a-43e6-9d77-83f90d832548','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',5,'restapi_bearer_token','REST API bearer token','Specify Bearer token for REST API','590a2872-d50e-47e4-afc6-ed008e26aaba','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('571fe7a6-7f96-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',2,'s3_service_name','S3 service name','Specify S3 service name','s3','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('0e6e671c-1a2d-4abd-8487-423a9954e0cb','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',6,'restapi_result_type','REST API result format (XML, JSON)','Specify format of result (XML, JSON)','JSON','ENUM','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000','{XML,JSON}',false),
	 ('74d26dfa-7f96-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',3,'s3_bucket_name','S3 bucket name','Specify S3 bucket name','bucket1','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('5bde39b9-9405-4562-8518-dfb736dd6f58','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',2,'restapi_auth_method','REST API auth type','Specify Auth type for rest API (basic, bearer token)','basic','ENUM','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000','{none,basic,"bearer token"}',false);
INSERT INTO da.connector_param (id,connector_id,show_order,"name",display_name,description,example,param_type,created,creator,modified,modifier,enum_values,required) VALUES
	 ('44310656-7f97-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',5,'s3_access_key','S3 access key','Specify S3 access key, if required','S3 access key','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('5ddf69b2-7f97-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',6,'s3_secret_access_key','S3 secret access key','Specify S3 secret access key, if required','s3_secret_access_key','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('79c98f04-7f97-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',7,'excel_first_row_headers','Excel file contains row names in 1-st row','Specify if excel file has row name in 1-st row or range','TRUE','BOOLEAN','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('978d35e0-7f97-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',8,'s3_signing_region','S3 signing region','Specify  S3 signing region, if required','us-east-1','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('9033837c-7f96-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',4,'s3_file_path','S3 file path','Specify file path inside S3 bucket','b57a75f0-7f96-11ed-a1eb-0242ac120002','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('34ff4a3b-0e54-4871-ba28-7af593393608','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',9,'jdbc_driver_jar_name','JDBC driver jar name','JDBC driver jar name','postgresql-42.5.1.jar','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('34ff4a3b-0e54-4871-ba28-7af593393607','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',2,'jdbc_username','JDBC username','Specify username','admin','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('60c38d5e-46cb-4c5a-a6b5-210525bec43b','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',1,'jdbc_url','JDBC url','Specify JDBC URL to database','jdbc:oracle:thin:@//myoracle.db.server:1521/my_servicename','TEXT','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false),
	 ('23c6d4dc-7f96-11ed-a1eb-0242ac120002','f49534e2-7f95-11ed-a1eb-0242ac120002',1,'s3_endpoint_url','S3 endpoint URL','Specify S3 endpoint URL','http://10.0.0.1:30324','TEXT','2022-12-19 00:00:00','1000000','2022-12-19 00:00:00','1000000',NULL,false),
	 ('09b43d76-a2eb-48d1-9343-6d0ef5624f43','1247e6eb-eef4-4aa3-b520-cd06a2609d4e',4,'restapi_basicauth_password','REST API basic auth password','Specify Password for basic auth','password','PASSWORD','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,false);
INSERT INTO da.connector_param (id,connector_id,show_order,"name",display_name,description,example,param_type,created,creator,modified,modifier,enum_values,required) VALUES
	 ('79367788-bcf8-42e1-af79-95e86ec0ada9','353362f1-c5a2-4f1f-8713-a0b46e2c6b73',3,'jdbc_password','JDBC password','Specify password','password','PASSWORD','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000',NULL,true);

CREATE TABLE da.elastic_versions (
	id serial4 NOT NULL,
	"schema" text NOT NULL,
	"version" int4 NOT NULL,
	CONSTRAINT elastic_versions_pkey PRIMARY KEY (id)
);

CREATE TABLE da.ldap_properties (
	id uuid NOT NULL,
	tenant_id int4 NOT NULL,
	provider_url text NOT NULL,
	principal text NULL,
	credentials text NULL,
	base_dn text NULL,
	user_query text NOT NULL,
	CONSTRAINT permissions_pk PRIMARY KEY (id)
);

INSERT INTO da.ldap_properties (id,tenant_id,provider_url,principal,credentials,base_dn,user_query) VALUES
	 ('974867d6-9bc9-48ce-a9e7-e5150ce6f515',999,'ldap://127.0.0.1:389','admin','password','dc=COMPANY,dc=COM','(sAMAccountName={0})');

CREATE TABLE da.pg_versions (
	id serial4 NOT NULL,
	ddl text NOT NULL,
	"version" int4 NOT NULL,
	CONSTRAINT pg_versions_pkey PRIMARY KEY (id)
);



CREATE TABLE da.system_type (
	id text NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	CONSTRAINT system_type_pk PRIMARY KEY (id)
);

INSERT INTO da.system_type (id,"name",description,created,creator,modified,modifier) VALUES
	 ('relational_database','relational_database','Реляционная база данных','2022-10-01 00:00:00','1000000','2022-10-01 00:00:00','1000000'),
	 ('rest_api','rest_api','REST API','2022-09-01 00:00:00','1000000','2022-09-01 00:00:00','1000000');

CREATE TABLE da.tenant (
	id int4 NOT NULL,
	"name" text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	"domain" text NULL,
	default_tenant bool NOT NULL DEFAULT false,
	CONSTRAINT tenant_pk PRIMARY KEY (id)
);

INSERT INTO da.tenant (id,"name",created,creator,modified,modifier,"domain",default_tenant) VALUES
	 (999,'Default tenant','2022-12-01 00:00:00','1000000','2022-12-01 00:00:00','1000000','test.com',true);

CREATE TABLE da.versions (
	id serial4 NOT NULL,
	"type" text NOT NULL,
	"version" int4 NOT NULL,
	CONSTRAINT versions_pkey PRIMARY KEY (id)
);

INSERT INTO da.versions ("type", "version") VALUES
	 ('elastic', 1),
	 ('pg', 1);

CREATE OR REPLACE VIEW da."Коннекторы и параметры"
AS SELECT c.name AS "Коннектор",
    cp.show_order AS "#",
    cp.display_name AS "Параметр",
    cp.example AS "Пример"
FROM da.connector_param cp
LEFT JOIN da.connector c ON c.id = cp.connector_id;

