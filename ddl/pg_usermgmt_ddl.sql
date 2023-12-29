CREATE SCHEMA usermgmt;

CREATE SEQUENCE usermgmt.platform_users_uid_seq
	INCREMENT BY 1
	MINVALUE 1000000
	MAXVALUE 2147483647
	START 1000000
	CACHE 1
	NO CYCLE;

CREATE TABLE usermgmt.api_tokens (
	id serial4 NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	valid_till timestamp NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	modified timestamp NOT NULL,
	modifier text NOT NULL,
	permissions _text NULL,
	user_roles _text NULL,
	tenant varchar(64) NOT NULL,
	CONSTRAINT api_tokens_pkey PRIMARY KEY (id)
);

CREATE TABLE usermgmt.api_tokens_blacklist (
	id serial4 NOT NULL,
	api_token_id int4 NOT NULL,
	cause text NOT NULL,
	created timestamp NOT NULL,
	creator text NOT NULL,
	CONSTRAINT api_tokens_blacklist_pkey PRIMARY KEY (id)
);

CREATE TABLE usermgmt.external_groups (
	id serial4 NOT NULL,
	"name" text NULL,
	description text NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	permissions _text NULL,
	user_roles _text NULL,
	"attributes" text NULL,
	tenant varchar(64) NULL,
	CONSTRAINT user_groups_pk PRIMARY KEY (id)
);

CREATE TABLE usermgmt.permissions (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	"level" text NULL,
	CONSTRAINT permissions_pk PRIMARY KEY (id)
);

INSERT INTO usermgmt.permissions (id,"name",description,"level") VALUES
	 ('3d5d9433-33d1-4d0b-ae06-f60fc9e3befe','actives_read','View all artifacts in Lottabyte Data Actives','tenant'),
	 ('27403fa9-236f-4b74-a58b-7c22d3f3adc1','actives_write','Modify all artifacts in Lottabyte Data Actives','tenant'),
	 ('b457b11a-0e2d-4c60-9f1c-84701d847dbd','global_admin','Глобальный Администратор. Создание и изменение параметров тенантов','global'),
	 ('b8f62273-dc1c-432a-ae6e-4f110d24a6ab','domain_r','Домены. Чтение','tenant'),
	 ('108f0c3b-8d91-4649-958d-dc609d204dca','domain_u','Домены. Обновление','tenant'),
	 ('391274a2-1602-470f-8791-306f9a3fb1e1','system_r','Системы. Чтение','tenant'),
	 ('8c626aa7-892b-4e12-bfe3-7ba0b0af302d','system_u','Системы. Обновление','tenant'),
	 ('6e552eba-c39b-4fd7-9cc0-cad405c3b4ae','lo_r','Логические объекты. Чтение','tenant'),
	 ('0a436fe6-d19d-4b2b-80e1-80c03858bbe0','lo_u','Логические объекты. Обновление','tenant'),
	 ('541675cf-a050-4a67-a3cc-781e7ad1a692','req_r','Запросы. Чтение','tenant');
INSERT INTO usermgmt.permissions (id,"name",description,"level") VALUES
	 ('86fdd7ae-7490-48d8-a93a-2df38b0557e0','req_u','Запросы. Обновление','tenant'),
	 ('7b1d84b4-5f37-4a8a-b513-60acc5f2e7c6','sample_r','Сэмплы. Чтение','tenant'),
	 ('ac2ad62e-95e2-4c26-a4a3-b1d9b345f5b6','sample_u','Сэмплы. Обновление','tenant'),
	 ('9260e441-9839-490b-8b9e-86ab7f6545ab','active_r','Активы. Чтение','tenant'),
	 ('9877775b-5829-45ce-bef5-b0192aa62669','active_u','Активы. Обновление','tenant'),
	 ('771cd5c7-9a1c-4e44-aafa-56e85df72fee','task_r','Задачи. Чтение','tenant'),
	 ('22739451-4684-4ede-b081-acdfd4c4ec22','task_u','Задачи. Обновление','tenant'),
	 ('bfb95c12-9cee-48a4-9d18-07402a8d9255','connection_r','Подключения к БД. Чтение','tenant'),
	 ('278b51d2-d44c-4ada-97d4-0e5989f546a4','connection_u','Подключения к БД. Обновление','tenant'),
	 ('f10d46fa-b916-4863-8f19-07acfdf74fca','st_r','Стюарды. Чтение','tenant');
INSERT INTO usermgmt.permissions (id,"name",description,"level") VALUES
	 ('863f4411-522d-4fc1-88be-18eb79e518e1','st_u','Стюарды. Обновление','tenant'),
	 ('a67395c3-6aa3-4100-8753-21c9d66f6f61','admin','Администратор. Создание и изменение Пользователей, Групп, Ролей, Стюардов','tenant'),
	 ('03a466fc-3677-48b1-a5fe-7f4afd004447','artifacts_r','Артифакты. Чтение','tenant'),
	 ('0122a515-830a-43cb-acdd-d79b904f5e50','artifacts_u','Артифакты. Обновление','tenant'),
	 ('50aa5558-1486-477f-a31a-7bade53cba02','business_entity_r','Бизнес сущность. Чтение','tenant'),
	 ('e1fdaa10-32b7-4a7a-8761-4434f0c44001','business_entity_u','Бизнес сущность. Обновление','tenant'),
	 ('f56af2a3-57ab-4801-a31b-0c68ac37318d','comments_r','Комментарии. Чтение','tenant'),
	 ('adb16ab0-3ca2-482f-8767-2abf8d62238b','comments_u','Комментарии. Обновление','tenant'),
	 ('e0c6904a-4bc4-4ad1-9b00-b40651cd5aa2','connector_r','Коннектор. Чтение','tenant'),
	 ('0d477669-e334-4495-8bc9-43df38e86400','custom_attribute_r','Кастомные атрибуты. Чтение','tenant');
INSERT INTO usermgmt.permissions (id,"name",description,"level") VALUES
	 ('63d20f01-6690-41d0-a5c1-ee2a6552068e','custom_attribute_u','Кастомные атрибуты. Обновление','tenant'),
	 ('ab283958-d0af-45bf-a594-89531a14276d','elastic_search_r','ElasticSearch. Чтение','tenant'),
	 ('4e22c233-86b8-4e2a-8f84-5b25af7d47e6','elastic_search_u','ElasticSearch. Обновление','tenant'),
	 ('ae11c214-0a57-49a4-a5bf-d2759ce22c1a','enumeration_r','Перечисление. Чтение','tenant'),
	 ('f3c503ab-ac82-4d2a-8978-c40adcd9a929','enumeration_u','Перечисление. Обновление','tenant'),
	 ('6ce658ad-4166-465b-844d-57e16bdd569a','indicator_r','Индикатор. Чтение','tenant'),
	 ('1c7b0296-c7e1-42b1-b0c7-3919009652a0','indicator_u','Индикатор. Обновление','tenant'),
	 ('d177ca4c-3de6-46b9-8766-77b92e57b989','product_r','Продукты. Чтение','tenant'),
	 ('7f9843ac-56f7-4c6f-8250-064ab63b5db5','product_u','Продукты. Обновление','tenant'),
	 ('7c988ff7-8a8a-40c4-99f4-61102973fc70','rating_r','Рейтинг. Чтение','tenant');
INSERT INTO usermgmt.permissions (id,"name",description,"level") VALUES
	 ('b9e39bed-8dd5-492d-a41b-af710fdf7a68','rating_u','Рейтинг. Обновление','tenant'),
	 ('e066d208-7450-437d-863a-70693fefef22','recent_views_r','Recent views. Чтение','tenant'),
	 ('9fd1c1db-eadd-470a-b4cb-eb10cca45492','sample_body_download','Скачивание файлов из S3','tenant'),
	 ('794ec682-43cd-4aa6-bc65-f9b002b507d1','tag_r','Тег. Чтение','tenant'),
	 ('b837ce59-12d7-4322-85ac-cb1ed820460f','tag_u','Тег. Обновление','tenant'),
	 ('8fb16f28-6eeb-499e-bd57-da304ad45768','workflow_r','Workflow. Чтение','tenant'),
	 ('640d005a-7bf1-4b0f-8420-2010ef3d04b1','lo_mdl_r','Модель логических объектов. Чтение','tenant'),
	 ('78710e5a-9fe0-4410-beab-eed5968cf5fa','lo_mdl_u','Модель логических объектов. Обновление','tenant');

CREATE TABLE usermgmt.platform_users (
	uid serial4 NOT NULL,
	username text NULL,
	display_name text NULL,
	description text NULL,
	email text NULL,
	salt text NULL,
	password_hash text NULL,
	apikey_hash text NULL,
	apikey_salt text NULL,
	approval_status text NULL,
	permissions _text NULL,
	user_roles _text NULL,
	current_account_status text NULL,
	internal_user bool NULL,
	deletable bool NULL,
	authenticator text NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	tenant varchar(64) NOT NULL,
	CONSTRAINT "primary" PRIMARY KEY (uid)
);
CREATE INDEX platform_users_username_idx ON usermgmt.platform_users USING btree (username);

CREATE TABLE usermgmt.platform_users_hist (
	uid serial4 NOT NULL,
	username text NULL,
	display_name text NULL,
	description text NULL,
	email text NULL,
	salt text NULL,
	password_hash text NULL,
	apikey_hash text NULL,
	apikey_salt text NULL,
	approval_status text NULL,
	permissions _text NULL,
	user_roles _text NULL,
	current_account_status text NULL,
	internal_user bool NULL,
	deletable bool NULL,
	authenticator text NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	tenant varchar(64) NOT NULL
);

CREATE OR REPLACE FUNCTION usermgmt.platform_users_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin 
	if (TG_OP = 'DELETE') then
		insert into usermgmt.platform_users_hist 
		(uid, username, display_name, description, email, salt, password_hash, apikey_hash, apikey_salt, 
		approval_status, permissions, user_roles, current_account_status, internal_user, deletable, 
		authenticator, created, modified, tenant)
		values (old.uid, old.username, old.display_name, old.description, old.email, old.salt, old.password_hash, old.apikey_hash, old.apikey_salt, 
		old.approval_status, old.permissions, old.user_roles, old.current_account_status, old.internal_user, old.deletable, 
		old.authenticator, old.created, old.modified, old.tenant);
		return old;
	end if;
end
$function$
;

create trigger platform_users_history_trigger before
delete
    on
    usermgmt.platform_users for each row execute function usermgmt.platform_users_history();

CREATE TABLE usermgmt.user_roles (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	permissions _text NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	tenant varchar(64) NOT NULL,
	CONSTRAINT user_roles_pk PRIMARY KEY (id)
);

CREATE TABLE usermgmt.user_roles_hist (
	id uuid NOT NULL,
	"name" text NOT NULL,
	description text NULL,
	permissions _text NULL,
	created timestamp NOT NULL,
	modified timestamp NOT NULL,
	tenant varchar(64) NOT NULL
);

CREATE OR REPLACE FUNCTION usermgmt.user_roles_history()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
begin 
	if (TG_OP = 'DELETE') then
		insert into usermgmt.user_roles_hist 
		(id, "name", description, permissions, created, modified, tenant)
		values (old.id, old."name", old.description, old.permissions, old.created, old.modified, old.tenant);
		return old;
	end if;
end
$function$
;

create trigger user_roles_history_trigger before
delete
    on
    usermgmt.user_roles for each row execute function usermgmt.user_roles_history();

INSERT INTO usermgmt.user_roles (id,"name",description,permissions,created,modified,tenant) VALUES
	 ('14ebd8ec-0422-41ca-9e28-096add613a40','Все пермишены',NULL,'{quality_task_r,workflow_r,domain_r,domain_u,system_r,system_u,lo_u,lo_r,req_r,req_u,sample_r,sample_u,active_r,active_u,task_r,task_u,connection_r,st_r,st_u,product_r,product_u,enumeration_u,enumeration_r,business_entity_r,business_entity_u,artifacts_r,artifacts_u,recent_views_r,connector_r,custom_attribute_r,custom_attribute_u,elastic_search_r,elastic_search_u,indicator_r,indicator_u,rating_r,rating_u,tag_r,tag_u,admin,comments_u,comments_r,sample_body_download,connection_u,actives_write,actives_read,lo_mdl_r,lo_mdl_u,dq_rule_r,dq_rule_u,quality_task_u}','2023-09-05 00:00:00','2023-11-16 11:21:30.996','999');
	
INSERT INTO usermgmt.platform_users (username,display_name,description,email,salt,password_hash,
	apikey_hash,apikey_salt,approval_status,permissions,user_roles,current_account_status,
	internal_user,deletable,authenticator,created,modified,tenant) VALUES
	('admin','admin',NULL,NULL,NULL,'$2y$10$3BrT26IYmlkd.AkgFo2Jn.WzwqQEgprak03eFbcn4ZzudUyApNUv2',
	NULL,NULL,'approved','{}','{14ebd8ec-0422-41ca-9e28-096add613a40}','enabled',
	true,true,'default','2023-02-13 10:26:34.777','2023-02-13 10:26:34.777','999');
	
	
	