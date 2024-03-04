package ru.bssg.lottabyte.core.i18n;

import ru.bssg.lottabyte.core.model.HttpStatus;

import java.text.MessageFormat;
import java.util.List;

public enum Message {
    LBE00001("Domain {0} not found", "Домен {0} не найден", HttpStatus.NOT_FOUND),
    LBE00002("Field {0} not present in searchable fields list",
            "Поле {0} отсутствует в списке полей с возможностью поиска", HttpStatus.BAD_REQUEST),
    LBE00005("Query not set for entity query {0}", "Запрос не задан для запроса сущности {0}", HttpStatus.BAD_REQUEST),
    LBE00006("Entity is null for system connection param {0}",
            "Объект равен нулю для параметра системного подключения {0}", HttpStatus.BAD_REQUEST),
    LBE00007("No connector parameter for parameter id {0}", "Нет параметра соединителя для параметра id {0}", HttpStatus.BAD_REQUEST),
    LBE00008("No parameter value for {0}", "Нет значения параметра для {0}", HttpStatus.BAD_REQUEST),
    LBE00009("GenericJDBCConnectorServiceImpl.querySystem() error",
            "Ошибка GenericJDBCConnectorServiceImpl.querySystem()", HttpStatus.BAD_REQUEST),
    LBE00010("ResultSet close error", "Ошибка закрытия результирующего набора", HttpStatus.BAD_REQUEST),
    LBE00011("Statement close error", "Ошибка закрытия инструкции", HttpStatus.BAD_REQUEST),
    LBE00012("Connection close error", "Ошибка закрытия соединения", HttpStatus.BAD_REQUEST),
    LBE00013("Field {0} not present in fields for join list", "Поле {0} отсутствует в полях для списка соединений", HttpStatus.BAD_REQUEST),
    LBE00014("Table {0} not present in tables for join list",
            "Таблица {0} отсутствует в таблицах для списка соединений", HttpStatus.BAD_REQUEST),
    LBE00015("On column {0} not present in on column for join list",
            "В столбце {0} отсутствует в столбце on для списка соединений", HttpStatus.BAD_REQUEST),
    LBE00016("Equal column {0} not present in equal column for join list",
            "Равный столбец {0} отсутствует в равном столбце для списка соединений", HttpStatus.BAD_REQUEST),
    LBE00017("Wrong connector type: {0}", "Неправильный тип разъема: {0}", HttpStatus.BAD_REQUEST),
    LBE00031("Authorization token not provided", "Токен авторизации не предоставлен", HttpStatus.NOT_AUTHORIZED),
    LBE00033("Invalid path format: {0}", "Недопустимый формат пути: {0}", HttpStatus.BAD_REQUEST),
    LBE00034("Not all attributes exist for an entity with id {0}. List: {1}",
            "Не все атрибуты существуют для объекта с идентификатором {0}. Список: {1}", HttpStatus.BAD_REQUEST),
    LBE00035("Failed to download the file: {0}", "Не удалось загрузить файл: {0}", HttpStatus.BAD_REQUEST),
    LBE00036("Failed to upload the file: {0}", "Не удалось загрузить файл: {0}", HttpStatus.BAD_REQUEST),
    LBE00037("Artifact_id: {0} not found for artifact_type: {1}", "Artifact_id: {0} не найдено для artifact_type: {1}", HttpStatus.BAD_REQUEST),
    LBE00041("Cannot upload empty file", "Не удается загрузить пустой файл", HttpStatus.NOT_FOUND),
    LBE00044("Artifact type {0} is invalid", "Тип артефакта {0} недопустим", HttpStatus.BAD_REQUEST),
    LBE00045("Query param must be 'POST' or 'GET'. {0}", "Параметр запроса должен быть 'POST' или 'GET'. {0}", HttpStatus.BAD_REQUEST),
    LBE00046("User with id {0} not found for this tenant",
            "Пользователь с идентификатором {0} не найден для этого арендатора", HttpStatus.NOT_FOUND),
    LBE00048("User name can not be empty", "Имя пользователя не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00049("Display name can not be empty", "Отображаемое имя не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00050("Password can not be empty", "Пароль не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00051("Role name can not be empty", "Имя роли не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00053("Role with id {0} not found", "Роль с идентификатором {0} не найдена", HttpStatus.NOT_FOUND),
    LBE00054("Permission {0} not found", "Разрешение {0} не найдено", HttpStatus.BAD_REQUEST),
    LBE00055("Role with name {0} already exists", "Роль с именем {0} уже существует", HttpStatus.BAD_REQUEST),
    LBE00056("User with username {0} already exists", "Пользователь с именем пользователя {0} уже существует", HttpStatus.BAD_REQUEST),
    LBE00058("External user can not be removed or modified", "Внешний пользователь не может быть удален или изменен", HttpStatus.BAD_REQUEST),
    LBE00061("Role with id {0} not found", "Роль с идентификатором {0} не найдена", HttpStatus.BAD_REQUEST),
    LBE00062("schedule_params: {0} does not fit the schedule_type: {1}",
            "schedule_params: {0} не соответствует schedule_type: {1}", HttpStatus.NOT_FOUND),
    LBE00063("Invalid schedule_type: {0}", "Недопустимый тип расписания: {0}", HttpStatus.NOT_FOUND),
    LBE00064("Role is assigned to at least one user, remove role from users first",
            "Роль назначена по крайней мере одному пользователю, сначала удалите роль у пользователей", HttpStatus.NOT_FOUND),
    LBE00065("Role is assigned to at least one external group, remove role from external groups first",
            "Роль назначена по крайней мере одной внешней группе, сначала удалите роль из внешних групп", HttpStatus.NOT_FOUND),
    LBE00066("The task time must be later than the current time. schedule_params: {0}, schedule_type: {1}",
            "Время выполнения задачи должно быть позже текущего времени. параметры расписания: {0}, тип расписания: {1}", HttpStatus.BAD_REQUEST),
    LBE00067("Unknown artifact state - {0}", "Неизвестное состояние артефакта - {0}", HttpStatus.BAD_REQUEST),
    LBE00068("Failed to delete the file: {0}", "Не удалось удалить файл: {0}", HttpStatus.BAD_REQUEST),
    LBE00069("State for Artifact_id: {0} and artifact_type: {1} must be PUBLISHED",
            "Должно быть опубликовано состояние для Artifact_id: {0} и artifact_type: {1}", HttpStatus.BAD_REQUEST),
    LBE00070("New password is required", "Введите новый пароль", HttpStatus.BAD_REQUEST),
    LBE00071("Passwords do not match", "Пароли не совпадают", HttpStatus.BAD_REQUEST),
    LBE00072("Wrong password", "Неправильный пароль", HttpStatus.BAD_REQUEST),

    LBE00101("Domain with id {0} not found", "Домен с идентификатором {0} не найден", HttpStatus.BAD_REQUEST),
    LBE00103("Domain with id {0} and version_id {1} not found", "Домен с id {0} и version_id {1} не найден", HttpStatus.NOT_FOUND),
    LBE00104("New Domain name can not be empty", "Новое доменное имя не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00105("Domains with ids: {0} not found", "Домены с идентификаторами: {0} не найдены", HttpStatus.BAD_REQUEST),
    LBE00106("Number of domains can't be over 100", "Количество доменов не может превышать 100", HttpStatus.BAD_REQUEST),
    LBE00107("Stewards exist in domain with id: {0}", "Стюарды существуют в домене с идентификатором: {0}", HttpStatus.BAD_REQUEST),
    LBE00108("Domain name must be unique: {0}", "Доменное имя должно быть уникальным: {0}", HttpStatus.BAD_REQUEST),
    LBE00109("Not all domains exist ({0})", "Не все домены существуют ({0})", HttpStatus.BAD_REQUEST),
    LBE00110("Cannot remove Systems(s) {0} from Domain. Data Assets exist with current System and Domain: {1}",
            "Не удается удалить системы {0} из домена. Ресурсы данных существуют в текущей системе и домене: {1}", HttpStatus.BAD_REQUEST),
    LBE00111("Cannot remove Domain. Remove Data Assets from Domain first",
            "Не удается удалить домен. Сначала удалите ресурсы данных из домена", HttpStatus.BAD_REQUEST),
    LBE00112("Domain has Systems, remove Systems from Domain first",
            "В домене есть системы, сначала удалите системы из домена", HttpStatus.BAD_REQUEST),
    LBE00113("You have no access to modify domain {0}", "У вас нет доступа к изменению домена {0}", HttpStatus.BAD_REQUEST),
    LBE00114("Systems {0} can not be added to domain. You have no access to systems.",
            "Системы {0} не могут быть добавлены в домен. У вас нет доступа к системам.", HttpStatus.BAD_REQUEST),
    LBE00116("You have no access to domain {0}", "У вас нет доступа к домену {0}", HttpStatus.BAD_REQUEST),
    LBE00117("Can't change Domain, you have no access to current Domain {0}",
            "Не удается сменить домен, у вас нет доступа к текущему домену {0}", HttpStatus.BAD_REQUEST),
    LBE00119("Domain already has Draft version with id {0}, please modify Draft version",
            "У домена уже есть черновик версии с идентификатором {0}, пожалуйста, измените черновик версии", HttpStatus.BAD_REQUEST),
    LBE00120("Cannot remove Domain. Remove Products from Domain first",
            "Не удается удалить домен. Сначала удалите продукты из домена", HttpStatus.BAD_REQUEST),
    LBE00121("Cannot remove Domain. Remove Business Entities from Domain first",
            "Не удается удалить домен. Сначала удалите бизнес-сущности из домена", HttpStatus.BAD_REQUEST),
    LBE00122("Cannot remove Domain. Remove Indicators from Domain first",
            "Не удается удалить домен. Сначала удалите показатели из домена", HttpStatus.BAD_REQUEST),
    LBE00123("Cannot remove Domain. Remove Users from Domain first",
            "Не удается удалить домен. Сначала удалите пользователей из домена", HttpStatus.BAD_REQUEST),

    LBE00201("Steward with id {0} not found", "Стюард с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00202("User id is not provided for Steward", "Идентификатор пользователя не предоставлен для Steward", HttpStatus.BAD_REQUEST),
    LBE00203("Steward with user id {0} already exists", "Стюард с идентификатором пользователя {0} уже существует", HttpStatus.BAD_REQUEST),
    LBE00204("Stewards not found with id/ids: {0}", "Не найдены стюарды с идентификатором/ids: {0}", HttpStatus.BAD_REQUEST),

    LBE00301("Entity with id {0} not found", "Entity с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00303("Entity name can not be empty", "Имя Entity не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00304("entity_folder with id {0} not found", "entity_folder с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00305("Entity with name '{0}' already exists in entity_folder with id {1}",
            "Entity с именем '{0}' уже существует в entity_folder с идентификатором {1}", HttpStatus.BAD_REQUEST),
    LBE00306("parent_entity_folder with id {0} not found", "parent_entity_folder с идентификатором {0} не найдена", HttpStatus.BAD_REQUEST),
    LBE00307("entity_folder with name '{0}' already exists in entity_folder with id {1}",
            "entity_folder с именем '{0}' уже существует в entity_folder с идентификатором {1}", HttpStatus.BAD_REQUEST),
    LBE00308("entity_folder id can not be empty", "Идентификатор entity_folder не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00309("parent_folder id {0} already a child of folder {1}",
            "Идентификатор родительской папки {0} уже является дочерним элементом папки {1}", HttpStatus.BAD_REQUEST),
    LBE00310("entity_folder with id {0} contains child Entities, remove them before deletion",
            "entity_folder с идентификатором {0} содержит дочерние объекты, удалите их перед удалением", HttpStatus.BAD_REQUEST),
    LBE00311("entity_id can not be empty", "entity_id не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00312("entity_attribute with id {0} not found", "entity_attribute с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00313("entity_attribute id can not be empty", "идентификатор entity_attribute не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00314("Cannot remove Systems(s) {0} from Entity. Data Assets exist with current System and Entity: {1}",
            "Не удается удалить системы {0} из объекта. Активы данных существуют в текущей системе и объекте: {1}", HttpStatus.BAD_REQUEST),
    LBE00315("Cannot remove Entity. Remove Data Assets linked with this Entity first",
            "Не удается удалить сущность. Сначала удалите активы данных, связанные с этим объектом", HttpStatus.BAD_REQUEST),
    LBE00317("Cannot remove Entity. First remove entity_query for entity with entity_id: {0}",
            "Не удается удалить сущность. Сначала удалите entity_query для объекта с entity_id: {0}", HttpStatus.BAD_REQUEST),
    LBE00318("Cannot remove Entity. Remove Entity_sample linked with this Entity first",
            "Не удается удалить сущность. Сначала удалите Entity_sample, связанный с этим объектом", HttpStatus.BAD_REQUEST),
    LBE00319(
            "Cannot remove Entity. Remove entity_attribute or entity_sample_property linked with this entity_attribute first: {0}",
            "Не удается удалить сущность. Сначала удалите entity_attribute или entity_sample_property, связанные с этим entity_attribute: {0}", HttpStatus.BAD_REQUEST),
    LBE00320(
            "Cannot remove Entity_query. Remove Entity_sample linked with this Entity_query first. entity_query_id: {0}",
            "Не удается удалить Entity_query. Сначала удалите Entity_sample, связанный с этим Entity_query. entity_query_id: {0}", HttpStatus.BAD_REQUEST),
    LBE00321("Cannot remove Entity_query. Remove Task linked with this Entity_query first",
            "Не удается удалить Entity_query. Сначала удалите задачу, связанную с этим Entity_query", HttpStatus.BAD_REQUEST),
    LBE00322("Entity can not be added to systems {0}. You have no access to systems.",
            "Entity не может быть добавлен в системы {0}. У вас нет доступа к системам.", HttpStatus.BAD_REQUEST),
    LBE00323("Entity can not be removed from systems {0}. You have no access to systems.",
            "Entity не может быть удален из систем {0}. У вас нет доступа к системам.", HttpStatus.BAD_REQUEST),
    LBE00324("You have no access to Entity {0}.", "У вас нет доступа к сущности {0}", HttpStatus.BAD_REQUEST),
    LBE00325("Can't change Entity, you have no access to current Entity {0}.",
            "Не удается изменить сущность, у вас нет доступа к текущей сущности {0}", HttpStatus.BAD_REQUEST),
    LBE00326("Entity already has Draft version with id {0}, please modify Draft version",
            "У Entity уже есть черновик версии с идентификатором {0}, пожалуйста, измените черновик версии", HttpStatus.BAD_REQUEST),
    LBE00327("Domain name must be unique: {0}", "Доменное имя должно быть уникальным: {0}", HttpStatus.BAD_REQUEST),
    LBE00328("Entity with id {0} and version_id {1} not found",
            "Entity с идентификатором {0} и version_id {1} не найден", HttpStatus.NOT_FOUND),
    LBE00329("The attribute of a data entity {0} cannot be deleted while it is in the indicator formula",
            "Атрибут объекта данных {0} не может быть удален, пока он находится в формуле индикатора", HttpStatus.BAD_REQUEST),
    LBE00330(
            "Cannot remove Entity Attribute. Remove entity_sample_property linked with this entity_attribute first: {0}",
            "Не удается удалить атрибут. Сначала удалите entity_sample_property, связанные с этим entity_attribute: {0}", HttpStatus.BAD_REQUEST),
    LBE00331("Cannot remove Entity Attribute. It is referenced in these objects: ",
            "Не удается удалить атрибут. Он связан с объектами: ", HttpStatus.BAD_REQUEST),

    LBE00401("entity_query with id {0} not found", "entity_query с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00402("entity_query id must not be null", "идентификатор entity_query не должен быть нулевым", HttpStatus.BAD_REQUEST),
    LBE00403("You have no access to Entity Query {0}", "У вас нет доступа к Entity Query {0}", HttpStatus.BAD_REQUEST),
    LBE00404("Entity Query name can not be empty", "Имя Entity Query не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00405("Entity Query query text can not be empty", "Entity Query текст запроса не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00406("Entity Query already has Draft version with id {0}, please modify Draft version",
            "Entity Query уже имеет черновую версию с идентификатором {0}, пожалуйста, измените черновую версию", HttpStatus.BAD_REQUEST),
    LBE00407("Entity Query with id {0} and version_id {1} not found",
            "Entity Query с id {0} и version_id {1} не найден", HttpStatus.NOT_FOUND),

    LBE00501("Data Asset name can not be empty", "Имя Data Asset не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00503("Data Asset with id {0} not found", "Data Asset с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE00504("You have no access to Data Asset {0}", "У вас нет доступа к Data Asset {0}", HttpStatus.BAD_REQUEST),
    LBE00505("Data Asset already has Draft version with id {0}, please modify Draft version",
            "Data Asset уже имеет черновую версию с идентификатором {0}, пожалуйста, измените черновую версию", HttpStatus.BAD_REQUEST),
    LBE00506("Data Asset with id {0} and version_id {1} not found",
            "Data Asset с идентификатором {0} и version_id {1} не найден", HttpStatus.NOT_FOUND),

    LBE00901("System with name {0} is not unique in folder", "Система с именем {0} не уникальна в папке", HttpStatus.BAD_REQUEST),
    LBE00902("system_folder with id {0} not found", "системная папка с идентификатором {0} не найдена", HttpStatus.NOT_FOUND),
    LBE00903("Parent system_folder with id {0} not found",
            "Родительская системная папка с идентификатором {0} не найдена", HttpStatus.NOT_FOUND),
    LBE00904("System with id {0} not found", "Система с идентификатором {0} не найдена", HttpStatus.NOT_FOUND),
    LBE00905("System type {0} not found", "Тип системы {0} не найден", HttpStatus.BAD_REQUEST),
    LBE00906("System name can not be empty", "Имя системы не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00907("System type can not be empty", "Системный тип не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00909("Some Entity Samples exist in System, please remove Entity Samples before removing System",
            "В системе существуют некоторые образцы Entity Samples, пожалуйста, удалите образцы сущностей перед удалением системы", HttpStatus.BAD_REQUEST),
    LBE00910("Some Entity Queries exist in System, please remove Entity Queries before removing System",
            "В системе существуют некоторые запросы сущностей, пожалуйста, удалите запросы сущностей перед удалением системы", HttpStatus.BAD_REQUEST),
    LBE00911("Some System Connections exist in System, please remove System Connections before removing System",
            "В системе существуют некоторые системные подключения, пожалуйста, удалите системные подключения перед удалением системы", HttpStatus.BAD_REQUEST),
    LBE00913("System Folder name can not be empty", "Имя System Folder не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE00914("System Folder with name {0} already exists in parent folder/root",
            "System Folder с именем {0} уже существует в родительской папке/root", HttpStatus.BAD_REQUEST),
    LBE00915("Not all systems exist: {0}", "Не все системы существуют: {0}", HttpStatus.BAD_REQUEST),
    LBE00916("Folder with id: {0} has System with id: {1}",
            "Папка с идентификатором: {0} содержит систему с идентификатором: {1}", HttpStatus.BAD_REQUEST),
    LBE00917("Cannot remove Domain(s) {0} from System. Data Assets exist with current System and Domain: {1}",
            "Не удается удалить домен(ы) {0} из системы. Ресурсы данных существуют в текущей системе и домене: {1}", HttpStatus.BAD_REQUEST),
    LBE00918("Cannot remove System. Remove Data Assets from System first",
            "Не удается удалить систему. Сначала удалите ресурсы данных из системы", HttpStatus.BAD_REQUEST),
    LBE00919("You have no access to modify system {0}", "У вас нет доступа к изменению системы {0}", HttpStatus.BAD_REQUEST),
    LBE00920("System can not be added to domains {0}. You have no access to domains.",
            "Система не может быть добавлена в домены {0}. У вас нет доступа к доменам.", HttpStatus.BAD_REQUEST),
    LBE00921("You have no access to system {0}.", "У вас нет доступа к системе {0}", HttpStatus.BAD_REQUEST),
    LBE00922("System can not be empty", "Система не может быть пустой", HttpStatus.BAD_REQUEST),
    LBE00923("Can't change system, you have no access to current system {0}.",
            "Не удается изменить систему, у вас нет доступа к текущей системе {0}", HttpStatus.BAD_REQUEST),
    LBE00924("System already has Draft version with id {0}, please modify Draft version",
            "В системе уже есть черновик версии с идентификатором {0}, пожалуйста, измените черновик версии", HttpStatus.BAD_REQUEST),
    LBE00925("System with id {0} and version_id {1} not found", "Система с id {0} и version_id {1} не найдена", HttpStatus.NOT_FOUND),

    LBE01001("Tag with id {0} not found", "Тег с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE01002("tag_category id is not provided", "идентификатор tag_category не указан", HttpStatus.BAD_REQUEST),
    LBE01003("Tag name can not be empty", "Имя тега не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01004("tag_category with id {0} not found", "tag_category с идентификатором {0} не найден", HttpStatus.BAD_REQUEST),
    LBE01005("Tag with name '{0}' already exists", "Тег с именем '{0}' уже существует", HttpStatus.BAD_REQUEST),
    LBE01006("tag_category {0} has child tags", "tag_category {0} имеет дочерние теги", HttpStatus.BAD_REQUEST),
    LBE01007("tag_category name is not provided", "имя тега_категории не указано", HttpStatus.BAD_REQUEST),
    LBE01008("tag_category with name '{0}' already exists", "tag_category с именем '{0}' уже существует", HttpStatus.BAD_REQUEST),
    LBE01009("Tag can not be empty", "Тег не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01010("Tag with name {0} not found", "Тег с именем {0} не найден", HttpStatus.NOT_FOUND),

    LBE01101("Connector with id {0} not found", "Соединение с идентификатором {0} не найден", HttpStatus.NOT_FOUND),

    LBE01201("system_connection with id {0} not found", "system_connection с идентификатором {0} не найдено", HttpStatus.NOT_FOUND),
    LBE01202("system_connection_param with id {0} not found",
            "system_connection_param с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE01203("system_connection_id must not be null", "system_connection_id не должен быть нулевым", HttpStatus.BAD_REQUEST),
    LBE01204("System connection param with Connector param id {0} is Required but is empty",
            "Параметр системного подключения с идентификатором параметра разъема {0} требуется, но пуст", HttpStatus.BAD_REQUEST),
    LBE01206("System Connection is used in tasks, remove tasks first",
            "В задачах используется системное подключение, сначала удалите задачи", HttpStatus.BAD_REQUEST),
    LBE01207(
            "To remove system_connection_params with id: {0}, the required field must be false for connection_parameters",
            "Чтобы удалить system_connection_params с идентификатором: {0}, обязательное поле должно быть false для connection_parameters", HttpStatus.BAD_REQUEST),
    LBE01208("You have no access to System Connection {0}", "У вас нет доступа к системному подключению {0}", HttpStatus.BAD_REQUEST),
    LBE01209("Connector param id should be set for all connector params",
            "Идентификатор параметра разъема должен быть установлен для всех параметров разъема", HttpStatus.BAD_REQUEST),

    LBE01302("Tag {0} is not linked to artifact {1}", "Тег {0} не связан с артефактом {1}", HttpStatus.BAD_REQUEST),

    LBE01401("Task with id {0} not found", "Задача с идентификатором {0} не найдена", HttpStatus.NOT_FOUND),
    LBE01402("Both schedule_params and schedule_types must be entered in the task",
            "В задаче должны быть введены как schedule_params, так и schedule_types", HttpStatus.BAD_REQUEST),
    LBE01403("schedule_params {0} with schedule_types {1} failed validation",
            "schedule_params {0} с schedule_types {1} не удалось выполнить проверку", HttpStatus.BAD_REQUEST),
    LBE01404("entity_query {0} and system_connection {1} belong to different systems",
            "entity_query {0} и system_connection {1} принадлежат разным системам", HttpStatus.BAD_REQUEST),
    LBE01406("Task name should not be null", "Имя задачи не должно быть нулевым", HttpStatus.BAD_REQUEST),
    LBE01407("The task list is empty", "Список задач пуст", HttpStatus.NOT_FOUND),
    LBE01408("You have no access to Task {0}", "У вас нет доступа к задаче {0}", HttpStatus.BAD_REQUEST),
    LBE01409("Task with queryId {0} not found", "Задача с идентификатором запроса {0} не найдена", HttpStatus.NOT_FOUND),

    LBE01502("task_run with task_id {0} not found", "task_run с task_id {0} не найден", HttpStatus.NOT_FOUND),

    LBE01602("TextSampleBody is null", "Текстовое тело образца равно null", HttpStatus.BAD_REQUEST),

    LBE01701("Missing comments for artifactId: {0}", "Отсутствуют комментарии для artifactId: {0}", HttpStatus.BAD_REQUEST),
    LBE01702("The comment text is missing", "Текст комментария отсутствует", HttpStatus.BAD_REQUEST),
    LBE01703("Invalid artifact_type: {0}", "Недопустимый artifact_type: {0}", HttpStatus.BAD_REQUEST),
    LBE01704("artifact_id: {0} not found for artifact_type: {1}", "artifact_id: {0} не найдено для artifact_type: {1}", HttpStatus.BAD_REQUEST),
    LBE01705("Missing comments with id: {0}", "Отсутствуют комментарии с идентификатором: {0}", HttpStatus.BAD_REQUEST),

    LBE01801("Custom attribute definition id can not be empty",
            "Идентификатор определения пользовательского атрибута не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01802("Custom attribute definition with id {0} not found",
            "Определение пользовательского атрибута с идентификатором {0} не найдено", HttpStatus.NOT_FOUND),
    LBE01804(
            "Custom attribute with definition id {0} is of Enumerated type and artifact_id of Enum value is empty of absent",
            "Пользовательский атрибут с идентификатором определения {0} имеет перечислимый тип, а artifact_id значения Enum пуст или отсутствует", HttpStatus.BAD_REQUEST),
    LBE01805("Custom attribute definition element with id {0} not found for Custom attribute definition {1}",
            "Элемент определения пользовательского атрибута с идентификатором {0} не найден для определения пользовательского атрибута {1}", HttpStatus.NOT_FOUND),
    LBE01806("Custom attribute definition with id {0} found more than once",
            "Определение пользовательского атрибута с идентификатором {0} найдено более одного раза", HttpStatus.BAD_REQUEST),
    LBE01807("Custom attribute def element with id {0} not found",
            "Элемент пользовательского атрибута def с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE01808("Custom attribute type can not be changed", "Тип пользовательского атрибута изменить невозможно", HttpStatus.BAD_REQUEST),
    LBE01809("Custom attribute def element name can not be empty",
            "Имя элемента пользовательского атрибута def не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01810("Custom attribute definition name can not be empty",
            "Имя определения пользовательского атрибута не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01811("Custom attribute definition type can not be empty",
            "Тип определения пользовательского атрибута не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01812("Custom attribute definition multiple values parameter can not be empty",
            "Параметр множественных значений пользовательского определения атрибута не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01813("Minimum and maximum values can be only specified for Numeric Custom attribute definition type",
            "Минимальные и максимальные значения могут быть указаны только для числового типа определения пользовательского атрибута", HttpStatus.BAD_REQUEST),
    LBE01814("Min length and max length values can be only specified for String Custom attribute definition type",
            "Значения минимальной длины и максимальной длины могут быть указаны только для типа определения пользовательского атрибута String", HttpStatus.BAD_REQUEST),
    LBE01815("Custom attribute definition 'required' parameter can not be empty",
            "Параметр пользовательского определения атрибута 'required' не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01816("Custom attribute of enumerated type should contain at least one def element",
            "Пользовательский атрибут перечисляемого типа должен содержать по крайней мере один элемент def", HttpStatus.BAD_REQUEST),
    LBE01817("Custom attribute def element name can not be empty",
            "Имя Custom attribute def element не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE01818("Custom attribute def element {0} specified more than once",
            "Custom attribute def element {0} указан более одного раза", HttpStatus.BAD_REQUEST),
    LBE01819("Custom attribute def elements can only be set for Enumerated attribute type",
            "Пользовательские элементы определения атрибутов могут быть установлены только для перечисляемого типа атрибута", HttpStatus.BAD_REQUEST),
    LBE01820("Custom attribute def element's definition id can not be changed",
            "Идентификатор Custom attribute def element не может быть изменен", HttpStatus.BAD_REQUEST),

    /*
     * LBE01801("Missing CustomAttributeDefinition with id: {0}"),
     * LBE01802("Missing CustomAttributeDefElement with id: {0}"),
     * LBE01803("When updating CustomAttributeDefElement, you cannot change the type: {0}, {1}"
     * ),
     * LBE01804("Missing CustomAttributeEnumValue with id: {0}"),
     */

    LBE01904("DataAsset with that name already exists: {0}", "DataAsset с таким именем уже существует: {0}", HttpStatus.BAD_REQUEST),

    LBE02001("Sample with id {0} not found", "Sample с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE02002("Sample_property with id {0} not found", "Sample_property с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE02003("Sample_id is not set for the sample_property with id: {0}",
            "Sample_id не задан для sample_property с идентификатором: {0}", HttpStatus.BAD_REQUEST),
    LBE02004("Sample with name {0} already exist", "Sample с именем {0} уже существует", HttpStatus.BAD_REQUEST),
    LBE02005("Sample with type: {0} must have type one of the following: json, xml, csv, table, text",
            "Sample с типом: {0} должен иметь один из следующих типов: json, xml, csv, таблица, текст", HttpStatus.BAD_REQUEST),
    LBE02006("You have no access to Entity Sample {0}", "У вас нет доступа к Sample Sample {0}", HttpStatus.BAD_REQUEST),
    LBE02007("Entity Sample Property {0} can not be linked to another Entity Sample",
            "Entity Sample Property {0} не может быть связано с другим образцом объекта", HttpStatus.BAD_REQUEST),

    LBE02101("EntitySampleType with type: {0} failed validation: {1}",
            "EntitySampleType с типом: {0} ошибка проверки: {1}", HttpStatus.BAD_REQUEST),
    LBE02102("EntitySampleType with type: {0} must have a SampleBody",
            "EntitySampleType с типом: {0} должен иметь SampleBody", HttpStatus.BAD_REQUEST),
    LBE02103("File size exceeds {0}Mb", "Размер файоа превышает {0}Мб", HttpStatus.BAD_REQUEST),

    LBE02201("Attribute with name '{0}' already exists", "Атрибут с именем '{0}' уже существует", HttpStatus.BAD_REQUEST),
    LBE02202("Attribute type does not exist: {0}", "Тип атрибута не существует: {0}", HttpStatus.BAD_REQUEST),
    LBE02203("Attribute with id {0} not found", "Атрибут с идентификатором {0} не найден", HttpStatus.BAD_REQUEST),
    LBE02204("There is an Attribute with this name: {0} in entity_id: {1}",
            "В entity_id: {1} есть атрибут с таким именем: {0}", HttpStatus.BAD_REQUEST),
    LBE02205("Attribute can not be added to Entity {0}. You have no access to Entity.",
            "Атрибут не может быть добавлен к сущности {0}. У вас нет доступа к Сущности.", HttpStatus.BAD_REQUEST),
    LBE02206("Attribute can not be changed, it belongs to Entity {0}. You have no access to Entity",
            "Атрибут не может быть изменен, он принадлежит сущности {0}. У вас нет доступа к сущности", HttpStatus.BAD_REQUEST),
    LBE02207("Attribute can not be removed from Entity {0}. You have no access to Entity",
            "Атрибут не может быть удален из объекта {0}. У вас нет доступа к сущности", HttpStatus.BAD_REQUEST),
    LBE02208("Entity Attribute can not be moved to other Entity",
            "Атрибут сущности не может быть перемещен в другую сущность", HttpStatus.BAD_REQUEST),
    LBE02209("Attributes list can only be changed for non PUBLISHED Entities",
            "Список атрибутов может быть изменен только для неопубликованных объектов", HttpStatus.BAD_REQUEST),

    LBE02301("The rating should be from 1 to 5. Current value: {0}",
            "Рейтинг должен быть от 1 до 5. Текущее значение: {0}", HttpStatus.BAD_REQUEST),

    LBE02401("Indicator with id {0} not found", "Индикатор с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE02402("Indicator name can not be empty", "Название индикатора не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE02403("Indicator already has Draft version with id {0}, please modify Draft version",
            "Индикатор уже имеет черновую версию с идентификатором {0}, пожалуйста, измените черновую версию", HttpStatus.BAD_REQUEST),
    LBE02404("Indicator type can not be empty", "Тип индикатора не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE02405("Rule type can not be empty", "Тип проверки не может быть пустым", HttpStatus.BAD_REQUEST),

    LBE02501("Business Entity with id {0} not found", "Business Entity с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE02502("Business Entity name can not be empty", "Название Business Entity не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE02503("Business Entity already has Draft version with id {0}, please modify Draft version",
            "У Business Entity уже есть черновик версии с идентификатором {0}, пожалуйста, измените черновик версии", HttpStatus.BAD_REQUEST),
    LBE02504("The essence itself cannot be synonymous with itself. id {0}",
            "Сущность сама по себе не может быть синонимом самой себя. идентификатор {0}", HttpStatus.BAD_REQUEST),
    LBE02505("Cannot delete business entity with id {0} which has children",
            "Нельзя удалить бизнес-сущность {0}, так как у нее есть дочерние элементы", HttpStatus.BAD_REQUEST),

    LBE02601("Enumeration with id {0} not found", "Перечисление с идентификатором {0} не найдено", HttpStatus.BAD_REQUEST),
    LBE02602("Enumeration name can not be empty", "Имя перечисления не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE02603("There are duplicates in the variants array: {0}", "В массиве variants есть дубликаты: {0}", HttpStatus.BAD_REQUEST),
    LBE02604("The variants array must not be empty", "Массив вариантов не должен быть пустым", HttpStatus.BAD_REQUEST),

    LBE02701("Enumeration with id {0} not found", "Перечисление с идентификатором {0} не найдено", HttpStatus.BAD_REQUEST),
    LBE02702("If attribute_type = ENUMERATION, then enumeration_id cannot be empty",
            "Если attribute_type = ENUMERATION, то enumeration_id не может быть пустым", HttpStatus.BAD_REQUEST),
    LBE02703("Enumeration id cannot be empty", "Идентификатор перечисления не может быть пустым", HttpStatus.BAD_REQUEST),

    LBE02801("Reference Source Artifact Id should be set",
            "Должен быть установлен идентификатор артефакта ссылочного источника", HttpStatus.BAD_REQUEST),
    LBE02802("Reference Source Artifact Type should be set",
            "Должен быть установлен тип артефакта ссылочного источника", HttpStatus.BAD_REQUEST),
    LBE02803("Reference Target Artifact Id should be set",
            "Должен быть установлен идентификатор эталонного целевого артефакта", HttpStatus.BAD_REQUEST),
    LBE02804("Reference Target Artifact Type should be set",
            "Должен быть установлен тип эталонного целевого артефакта", HttpStatus.BAD_REQUEST),
    LBE02805("Reference Type should be set", "Должен быть установлен ссылочный тип", HttpStatus.BAD_REQUEST),
    LBE02806("Reference Source Artifact Id and Target Artifact Id can not be the same",
            "Идентификатор эталонного исходного артефакта и идентификатор целевого артефакта не могут совпадать", HttpStatus.BAD_REQUEST),
    LBE02807("Reference between Source Artifact and Target artifact already exists with the same Reference Type",
            "Ссылка между исходным артефактом и целевым артефактом уже существует с одним и тем же типом ссылки", HttpStatus.BAD_REQUEST),
    LBE02808("Reference with id {0} not found", "Ссылка с идентификатором {0} не найдена", HttpStatus.BAD_REQUEST),
    LBE02809("Source artifact with id {0} and type {1} not found",
            "Исходный артефакт с идентификатором {0} и типом {1} не найден", HttpStatus.BAD_REQUEST),
    LBE02810("Target artifact with id {0} and type {1} not found",
            "Целевой артефакт с идентификатором {0} и типом {1} не найден", HttpStatus.BAD_REQUEST),
    LBE02812("Reference with source id {0} and target id {1} not found",
            "Ссылка с идентификатором источника {0} и идентификатором цели {1} не найдена", HttpStatus.BAD_REQUEST),

    LBE02900("Tenant name must be unique: {0}", "Имя Tenant должно быть уникальным: {0}", HttpStatus.BAD_REQUEST),
    LBE02901("Tenant must have a unique domain: {0}", "У Tenant должен быть уникальный домен: {0}", HttpStatus.BAD_REQUEST),
    LBE02902("When deleting a tenant, it should not be default: {0}",
            "При удалении Tenant не должно быть значения по умолчанию: {0}", HttpStatus.BAD_REQUEST),
    LBE02903("Tenant with id {0} not found", "Tenant с идентификатором {0} не найден", HttpStatus.BAD_REQUEST),

    LBE03001("Invalid state {0} in search request, allowed DRAFT or PUBLISHED",
            "Недопустимое состояние {0} в поисковом запросе, разрешенный DRAFT или PUBLISHED", HttpStatus.BAD_REQUEST),
    LBE03002("Workflow task with {0} not found", "Workflow task с {0} не найдена", HttpStatus.NOT_FOUND),
    LBE03003("Only DRAFT artifact can be canceled", "Только DRAFT артефакта может быть отменен", HttpStatus.BAD_REQUEST),
    LBE03004("Invalid workflow task, draft artifact of type {0} and id {1} not found",
            "Недопустимая задача рабочего процесса, черновик артефакта типа {0} и идентификатора {1} не найден", HttpStatus.NOT_FOUND),
    LBE03005("Trying to Approve removal of artifact on non READY_FOR_REMOVAL workflow state artifact",
            "Пытаюсь одобрить удаление артефакта для артефакта состояния READY_FOR_REMOVAL, не готового к использованию", HttpStatus.BAD_REQUEST),
    LBE03006("No published id is found for draft {0} in READY_FOR_REMOVAL state",
            "Не найден опубликованный идентификатор для черновика {0} в состоянии READY_FOR_REMOVAL", HttpStatus.NOT_FOUND),
    LBE03007("Action {0} is not supported by current artifact's workflow",
            "Действие {0} не поддерживается рабочим процессом текущего артефакта", HttpStatus.BAD_REQUEST),
    LBE03008("Artifact type {0} is not managed by workflow", "Тип артефакта {0} не управляется рабочим процессом", HttpStatus.BAD_REQUEST),
    LBE03009("No artifact with id {0} and PUBLISHED state is found",
            "Артефакт с идентификатором {0} и PUBLISHED состоянием не найден", HttpStatus.BAD_REQUEST),
    LBE03010("Workflow task action with {0} not found", "Действие задачи рабочего процесса с {0} не найдено", HttpStatus.NOT_FOUND),
    LBE03012("No process definition key found for Artifact type {0} and Artifact action {1}",
            "Не найден ключ процесса для типа артефакта {0} и действия с артефактом {1}", HttpStatus.BAD_REQUEST),
    LBE03013("Unknown process key {0} in workflow settings for artifact type {1} and action {1}",
            "Указан несуществующий ключ процесса {0} для типа артефакта {1} и действия {2}", HttpStatus.BAD_REQUEST),

    LBE03101("Product with id {0} not found", "Продукт с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE03102("Product with id {0} and version_id {1} not found", "Продукт с id {0} и version_id {1} не найден", HttpStatus.NOT_FOUND),
    LBE03103("Not all DataEntityAttribute exist: {0}", "Не все DataEntityAttribute существуют: {0}", HttpStatus.BAD_REQUEST),
    LBE03104("Not all indicator exist: {0}", "Не все индикаторы существуют: {0}", HttpStatus.BAD_REQUEST),
    LBE03105("Not all data assets exist: {0}", "Не все активы существуют: {0}", HttpStatus.BAD_REQUEST),

    LBE03201("BlackList token with id {0} not found", "BlackList token с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE03202("Token with id {0} not found", "Токен с идентификатором {0} не найден", HttpStatus.NOT_FOUND),
    LBE03203("The elements: {0} is missing in the directory: {1}", "Элементы: {0} отсутствуют в каталоге: {1}", HttpStatus.NOT_FOUND),
    LBE03204("You cannot delete an indicator if its id is specified in the formula: {0}",
            "Вы не можете удалить индикатор, если его идентификатор указан в формуле: {0}", HttpStatus.BAD_REQUEST),
    LBE03205("DQ rule is used", "DQ правило используется", HttpStatus.CONFLICT),
    LBE03206("DQ rule is used. You can only disable it.", "Невозможно удалить привязанное правило проверки качества данных, т.к. по нему уже запущены действующие задачи DQ. Вы можете выключить задачу DQ.", HttpStatus.BAD_REQUEST),


    LBE03301("Record {0} not found", "Запись {0} не найдена", HttpStatus.NOT_FOUND),

    LBE03419("DQ rule already has Draft version with id {0}, please modify Draft version",
            "У правила уже есть черновик версии с идентификатором {0}, пожалуйста, измените черновик версии", HttpStatus.BAD_REQUEST);

    private String ru, en;
    private HttpStatus httpStatus;

    Message(String en, String ru, HttpStatus httpStatus) {
        this.ru = ru;
        this.en = en;
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    public String getText(String lang) {
        switch (lang) {
            case "ru":
                return this.ru;
            case "en":
                return this.en;
            default:
                return null;
        }
    }

    public static Message fromString(String text, String lang) {
        for (Message b : Message.values()) {
            switch (lang) {
                case "ru":
                    if (b.ru.equalsIgnoreCase(text)) {
                        return b;
                    }
                case "en":
                    if (b.en.equalsIgnoreCase(text)) {
                        return b;
                    }
            }
        }
        return null;
    }

    public static String getString(Message msg, String lang) {
        return msg == null ? null : msg.getText(lang);
    }

    public static String format(Message msg, String lang, Object... msgArgs) {
        if (msg == null) {
            return null;
        } else {
            String logMsg;
            if (msgArgs != null && msgArgs.length > 0) {
                String logTemplate = getString(msg, lang);
                logMsg = MessageFormat.format(logTemplate, msgArgs);
            } else {
                logMsg = getString(msg, lang);
            }
            return logMsg;
        }
    }

    public static String format(String stringMsg, String lang, Object... msgArgs) {
        Message msg = Message.fromString(stringMsg, lang);
        if (msg == null) {
            return null;
        } else {
            String logMsg;
            if (msgArgs != null && msgArgs.length > 0) {
                String logTemplate = getString(msg, lang);
                logMsg = MessageFormat.format(logTemplate, msgArgs);
            } else {
                logMsg = getString(msg, lang);
            }
            return logMsg;
        }
    }
    //
    // private String text;
    // Message(String text) {
    // this.text = text;
    // }
    //
    // public String getText() {
    // return this.text;
    // }
    //
    // public static Message fromString(String text) {
    // for (Message b : Message.values()) {
    // if (b.text.equalsIgnoreCase(text)) {
    // return b;
    // }
    // }
    // return null;
    // }
    // public static String getString(Message msg) {
    // return msg == null ? null : msg.getText();
    // }
    // public static String format(Message msg, Object... msgArgs) {
    // if (msg == null) {
    // return null;
    // } else {
    // String logMsg;
    // if (msgArgs != null && msgArgs.length > 0) {
    // String logTemplate = getString(msg);
    // logMsg = MessageFormat.format(logTemplate, msgArgs);
    // } else {
    // logMsg = getString(msg);
    // }
    // return logMsg;
    // }
    // }
}
